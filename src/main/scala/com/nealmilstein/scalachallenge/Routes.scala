package com.nealmilstein.scalachallenge

import cats.effect.Sync
import cats.implicits._

import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec._

import doobie.implicits._
import doobie.util.transactor

import io.circe._
import io.circe.generic.JsonCodec

import java.util.Date
import java.text.{SimpleDateFormat, ParseException}

final case class BestRatedRequest(
    start: Date,
    end: Date,
    limit: Int,
    min_number_reviews: Int
)

object BestRatedRequest {
  private implicit val dateDecoder: Decoder[Date] = new Decoder[Date] {
    val dateFormatter = new SimpleDateFormat("dd.MM.yyyy")
    dateFormatter.setLenient(false)

    def apply(c: HCursor): Decoder.Result[Date] =
      for {
        dateStr <- c.value.as[String]
        date <-
          try {
            Right(dateFormatter.parse(dateStr))
          } catch {
            case _: ParseException =>
              Left(
                DecodingFailure(
                  "Attempt to decode invalid date object",
                  c.history
                )
              )
          }
      } yield date
  }

  implicit val bestRatedRequestDecoder: Decoder[BestRatedRequest] =
    new Decoder[BestRatedRequest] {
      def apply(c: HCursor): Decoder.Result[BestRatedRequest] = for {
        start <- c.downField("start").as[Date]
        end <- c.downField("end").as[Date]
        limit <- c.downField("limit").as[Int]
        min_number_reviews <- c.downField("min_number_reviews").as[Int]
      } yield BestRatedRequest(start, end, limit, min_number_reviews)
    }
}

@JsonCodec
final case class ProductRatingAverage(
    asin: String,
    average_rating: Float
)

object Routes {
  def reviewRoutes[F[_]: Sync](xa: transactor.Transactor[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] { case req @ POST -> Root / "amazon" / "best-rated" =>
      for {
        decoded <- req.attemptAs[BestRatedRequest].value
        resp <- decoded match {
          case Left(err) => BadRequest(err.message)
          case Right(bestRatedRequest) =>
            for {
              bestRatings <- sql"""
              SELECT asin, AVG(overall)
              FROM reviews
              WHERE created_at > CAST(${bestRatedRequest.start} AS TIMESTAMP)
                AND created_at < CAST(${bestRatedRequest.end} AS TIMESTAMP)
              GROUP BY asin
              HAVING COUNT(id) >= ${bestRatedRequest.min_number_reviews}
              ORDER BY AVG DESC
              LIMIT ${bestRatedRequest.limit}
            """.query[(String, Float)].to[List].transact(xa)
              resp <- Ok(
                bestRatings.map(rating =>
                  ProductRatingAverage(rating._1, rating._2)
                )
              )
            } yield resp
        }
      } yield resp
    }
  }
}
