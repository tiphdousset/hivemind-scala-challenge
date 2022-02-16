package com.nealmilstein.scalachallenge

import cats.effect.{IO, Resource, Blocker}
import org.http4s._
import org.http4s.implicits._
import munit.CatsEffectSuite

import doobie._
import doobie.hikari._

import org.http4s.circe._

class BestRatedSpec extends CatsEffectSuite {
  // TODO: fix another day

  // val transactor: Resource[IO, HikariTransactor[IO]] =
  //   for {
  //     ce <- ExecutionContexts.fixedThreadPool[IO](32)
  //     xa <- HikariTransactor.newHikariTransactor[IO](
  //       "org.postgresql.Driver",
  //       "jdbc:postgresql://db:5432/",
  //       "scalachallenge",
  //       "scalachallenge",
  //       ce,
  //       Blocker.liftExecutionContext(ce)
  //     )
  //   } yield xa

  // test("amazon/best-rated returns status code 200") {
  //   assertIO(
  //     retBestRated(BestRatedRequest("01.01.2010", "31.12.2020", 2, 2))
  //       .map(_.status),
  //     Status.Ok
  //   )
  // }

  // test("amazon/best-rated returns status code 400") {
  //   assertIO(
  //     retBestRated(BestRatedRequest("foo", "31.12.2020", 2, 2))
  //       .map(_.status),
  //     Status.BadRequest
  //   )
  // }

  // private[this] def retBestRated(body: BestRatedRequest): IO[Response[IO]] = {
  //   // implicit val encoder: Encoder[BestRatedRequest] = deriveEncoder
  //   implicit def entityEncoder[F[_]]: EntityEncoder[F, BestRatedRequest] =
  //     jsonEncoderOf[F, BestRatedRequest]

  //   transactor.use { xa =>
  //     val postBestRated =
  //       Request[IO](Method.POST, uri"/amazon/best-rated").withEntity(body)
  //     Routes.reviewRoutes(xa).orNotFound(postBestRated)
  //   }
  // }
}
