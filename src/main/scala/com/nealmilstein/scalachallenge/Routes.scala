package com.nealmilstein.scalachallenge

import cats.effect.Sync
import cats.implicits._

import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec._

import doobie.implicits._
import doobie.util.transactor

import io.circe.generic.JsonCodec

@JsonCodec
final case class BestRatedRequest(
    start: String,
    end: String,
    limit: Int,
    min_number_reviews: Int
)

@JsonCodec
final case class ProductRatingAverage(
    asin: String,
    average_rating: Float
)

object Routes {
  def reviewRoutes[F[_]: Sync](xa: transactor.Transactor[F]): HttpRoutes[F] = {
    def reverseDate(date: String) = date.split('.').reverse.mkString(".")
    val dateRegex = raw"\d{2}\.\d{2}\.\d{4}"

    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] { case req @ POST -> Root / "amazon" / "best-rated" =>
      for {
        decoded <- req.attemptAs[BestRatedRequest].value
        resp <- {
          decoded match {
            case Left(_) => BadRequest()
            case Right(json)
                if !json.start.matches(dateRegex)
                  || !json.end.matches(dateRegex) =>
              BadRequest()
            case Right(requestJson) =>
              for {
                bestRatings <- sql"""
                  SELECT asin, AVG(overall)
                  FROM reviews
                  WHERE created_at > CAST(${reverseDate(requestJson.start)} AS TIMESTAMP)
                    AND created_at < CAST(${reverseDate(requestJson.end)} AS TIMESTAMP)
                  GROUP BY asin
                  HAVING COUNT(id) >= ${requestJson.min_number_reviews}
                  ORDER BY AVG DESC
                  LIMIT ${requestJson.limit}
                """.query[(String, Float)].to[List].transact(xa)
                resp <- Ok(
                  bestRatings.map(rating =>
                    ProductRatingAverage(rating._1, rating._2)
                  )
                )
              } yield resp
          }
        }
      } yield resp
    }
  }
}
