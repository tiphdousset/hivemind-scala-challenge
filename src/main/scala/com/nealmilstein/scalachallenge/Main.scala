package com.nealmilstein.scalachallenge

import cats.effect._
// import cats.implicits._
import cats.effect.{Blocker, ExitCode, IO, IOApp}

import doobie._
// import doobie.implicits._
import doobie.hikari._

object Main extends IOApp {
  val transactor: Resource[IO, HikariTransactor[IO]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32)
      xa <- HikariTransactor.newHikariTransactor[IO](
        "org.postgresql.Driver",
        "jdbc:postgresql://db:5432/",
        "scalachallenge",
        "scalachallenge",
        ce,
        Blocker.liftExecutionContext(ce)
      )
    } yield xa

  def run(args: List[String]): IO[ExitCode] =
    transactor.use { xa =>
      Server.stream[IO](xa).compile.drain.as(ExitCode.Success)
    }
}
