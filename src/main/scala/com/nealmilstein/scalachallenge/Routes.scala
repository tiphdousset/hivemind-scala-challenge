package com.nealmilstein.scalachallenge

import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec._
import doobie.util.transactor
import io.circe.generic.JsonCodec

@JsonCodec
final case class BestRatedRequest(
    start: String,
    end: String,
    limit: Int,
    min_number_reviews: Int
)

object Routes {
  def reviewRoutes[F[_]: Sync](xa: transactor.Transactor[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] { case req @ POST -> Root / "amazon" / "best-rated" =>
      for {
        bestRatedRequest <- req.as[BestRatedRequest]
        resp <- Ok(bestRatedRequest)
      } yield resp
    }
  }
}
