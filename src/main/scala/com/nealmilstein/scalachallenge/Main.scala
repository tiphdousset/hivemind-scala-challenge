package com.nealmilstein.scalachallenge

import cats.effect._
import cats.effect.{Blocker, ExitCode, IO, IOApp}

import doobie._
import doobie.implicits._
import doobie.hikari._
import doobie.util

import io.circe._, io.circe.generic.semiauto._, io.circe.parser._

import fs2.{Stream, text}

import java.nio.file.Paths

final case class Review(
    asin: String,
    reviewerID: String,
    reviewerName: String,
    helpful: List[Int],
    reviewText: String,
    overall: Float,
    summary: String,
    unixReviewTime: Long
)

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

  def initTable(xa: util.transactor.Transactor[IO]): IO[Unit] = {
    for {
      _ <- sql"""
          CREATE TABLE reviews (
            id integer generated always as identity NOT NULL,
            asin text NOT NULL,
            reviewerID text NOT NULL,
            reviewerName text NOT NULL,
            helpfulNumerator integer NOT NULL,
            helpfulDenominator integer NOT NULL,
            reviewText text NOT NULL,
            overall integer NOT NULL,
            summary text NOT NULL,
            created_at timestamp with time zone NOT NULL,
            PRIMARY KEY(id)
          );
        """.update.run.transact(xa)
      _ <- sql"CREATE INDEX reviews_asin ON reviews (asin);".update.run
        .transact(xa)
      _ <-
        sql"CREATE INDEX reviews_created_at ON reviews (created_at);".update.run
          .transact(xa)
    } yield ()
  }

  def initSchema(xa: util.transactor.Transactor[IO]): IO[Unit] = {
    for {
      doesTableExist <- sql"""
        SELECT EXISTS (
          SELECT FROM information_schema.tables
          WHERE table_name = 'reviews'
        );
      """.query[Boolean].unique.transact(xa)
      _ <- if (!doesTableExist) initTable(xa) else IO.unit
    } yield ()
  }

  def importReviews(
      fileName: String,
      xa: util.transactor.Transactor[IO]
  ) = {
    val infoMessage = Stream.eval(IO(println("Importing reviews...")))
    val truncateTable =
      Stream.eval(sql"TRUNCATE TABLE reviews".update.run.transact(xa))

    val reviewDecoder: Decoder[Review] = deriveDecoder
    val insertReviews = Stream.resource(Blocker[IO]).flatMap { blocker =>
      fs2.io.file
        .readAll[IO](Paths.get(fileName), blocker, 4096)
        .through(text.utf8Decode)
        .through(text.lines)
        .map(line => reviewDecoder.decodeJson(parse(line).getOrElse(Json.Null)))
        .collect { case Right(review) => review }
        .evalMap(review => sql"""
          INSERT INTO reviews (
            asin,
            reviewerID,
            reviewerName,
            helpfulNumerator,
            helpfulDenominator,
            reviewText,
            overall,
            summary,
            created_at
          ) VALUES (
            ${review.asin},
            ${review.reviewerID},
            ${review.reviewerName},
            ${review.helpful(0)},
            ${review.helpful(1)},
            ${review.reviewText},
            ${review.overall},
            ${review.summary},
            to_timestamp(${review.unixReviewTime})
          )
        """.update.run.transact(xa))
    }

    infoMessage ++ truncateTable ++ insertReviews
  }

  def run(args: List[String]): IO[ExitCode] =
    transactor.use { xa =>
      for {
        _ <- initSchema(xa)
        _ <-
          if (args.length > 0 && args(0).trim().length() > 0)
            importReviews(args(0), xa).compile.drain.as(IO.unit)
          else IO.unit
        _ <- Server.stream[IO](xa).compile.drain.as(ExitCode.Success)
      } yield (ExitCode.Success)
    }
}
