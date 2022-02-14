val Http4sVersion = "0.21.31"
val CirceVersion = "0.13.0"
val MunitVersion = "0.7.27"
val LogbackVersion = "1.2.5"
val MunitCatsEffectVersion = "1.0.5"
val DoobieVersion = "0.13.4"
val fs2Version = "2.5.10"

lazy val root = (project in file("."))
  .settings(
    organization := "com.nealmilstein",
    name := "scalachallenge",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.6",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "org.scalameta" %% "munit" % MunitVersion % Test,
      "org.typelevel" %% "munit-cats-effect-2" % MunitCatsEffectVersion % Test,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "org.scalameta" %% "svm-subs" % "20.2.0",
      "org.tpolecat" %% "doobie-core" % DoobieVersion,
      "org.tpolecat" %% "doobie-postgres" % DoobieVersion,
      "org.tpolecat" %% "doobie-hikari" % DoobieVersion,
      "co.fs2" %% "fs2-core" % fs2Version,
      "co.fs2" %% "fs2-io" % fs2Version,
      "io.circe" %% "circe-parser" % CirceVersion
    ),
    addCompilerPlugin(
      "org.typelevel" %% "kind-projector" % "0.13.0" cross CrossVersion.full
    ),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    testFrameworks += new TestFramework("munit.Framework")
  )

scalacOptions ~= {
  _.filterNot(Set("-Xfatal-warnings"))
}

scalacOptions ++= Seq(
  "-Ymacro-annotations"
)

fork in run := true

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
