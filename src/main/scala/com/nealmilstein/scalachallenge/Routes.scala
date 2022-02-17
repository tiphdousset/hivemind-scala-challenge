package com.nealmilstein.scalachallenge

import cats.effect.Sync
import cats.implicits._
import cats.data.EitherT

import org.http4s.{HttpRoutes, MessageFailure}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec._

import doobie.implicits._
import doobie.util.transactor

import io.circe._
import io.circe.generic.JsonCodec

import java.lang.Throwable
import java.sql.SQLException
import java.util.Date
import java.text.{SimpleDateFormat, ParseException}

final case class BestRatedRequest(
    start: Date,
    end: Date,
    limit: Limit,
    min_number_reviews: MinNumberReviews
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

  private val limitDecoder: Decoder[Limit] = new Decoder[Limit] {
    def apply(c: HCursor): Decoder.Result[Limit] =
      for {
        limitInt <- c.value.as[Int]
        limit <-
          if (limitInt > 0)
            Right(limitInt)
          else
            (Left(DecodingFailure("limit must be > 0", c.history)))
      } yield limit
  }

  private val minNumberReviewsDecoder: Decoder[MinNumberReviews] =
    new Decoder[MinNumberReviews] {
      def apply(c: HCursor): Decoder.Result[MinNumberReviews] =
        for {
          minNumberReviewsInt <- c.value.as[Int]
          minNumberReviews <-
            if (minNumberReviewsInt > 0)
              Right(minNumberReviewsInt)
            else
              (Left(
                DecodingFailure("min_number_reviews must be > 0", c.history)
              ))
        } yield minNumberReviews
    }

  implicit val bestRatedRequestDecoder: Decoder[BestRatedRequest] =
    new Decoder[BestRatedRequest] {
      def apply(c: HCursor): Decoder.Result[BestRatedRequest] = for {
        start <- c.downField("start").as[Date]
        end <- c.downField("end").as[Date]
        limit <- c.downField("limit").as[Limit](limitDecoder)
        min_number_reviews <- c
          .downField("min_number_reviews")
          .as[MinNumberReviews](minNumberReviewsDecoder)
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
      val bestRatings = for {
        decoded <- req.attemptAs[BestRatedRequest]
        dbResult <- EitherT(sql"""
            SELECT asin, AVG(overall)
            FROM reviews
            WHERE created_at > CAST(${decoded.start} AS TIMESTAMP)
              AND created_at < CAST(${decoded.end} AS TIMESTAMP)
            GROUP BY asin
            HAVING COUNT(id) >= ${decoded.min_number_reviews}
            ORDER BY AVG DESC
            LIMIT ${decoded.limit}
          """.query[(String, Float)].to[List].transact(xa).attempt)
        bestRatings <- EitherT.rightT[F, Throwable](
          dbResult.map(row => ProductRatingAverage(row._1, row._2))
        )
      } yield bestRatings

      for {
        either <- bestRatings.value
        resp <- either match {
          case Left(err) =>
            err match {
              case _: MessageFailure => BadRequest("Invalid JSON request body")
              case _: SQLException   => InternalServerError("Database error")
              case _ => InternalServerError("Internal server error")
            }
          case Right(ratings) => Ok(ratings)
        }
      } yield resp
    }
  }
}
