package com.nealmilstein.scalachallenge

import cats.effect.{ConcurrentEffect, Timer}
import doobie.util.transactor
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import scala.concurrent.ExecutionContext.global

object Server {

  def stream[F[_]: ConcurrentEffect](
      xa: transactor.Transactor[F]
  )(implicit T: Timer[F]): Stream[F, Nothing] = {

    for {
      _ <- BlazeClientBuilder[F](global).stream
      httpApp = Routes.reviewRoutes[F](xa).orNotFound
      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      exitCode <- BlazeServerBuilder[F](global)
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode
  }.drain
}
