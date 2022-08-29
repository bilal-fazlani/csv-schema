ThisBuild / scalaVersion := "3.2.0-RC4"
ThisBuild / organization := "com.bilal-fazlani"
ThisBuild / organizationName := "Bilal Fazlani"
ThisBuild / testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/bilal-fazlani/csv-schema"),
    "https://github.com/bilal-fazlani/csv-schema.git"
  )
)
ThisBuild / developers := List(
  Developer(
    "bilal-fazlani",
    "Bilal Fazlani",
    "bilal.m.fazlani@gmail.com",
    url("https://bilal-fazlani.com")
  )
)
ThisBuild / licenses := List(
  "MIT License" -> url(
    "https://github.com/bilal-fazlani/csv-schema/blob/main/LICENSE"
  )
)
ThisBuild / homepage := Some(url("https://github.com/bilal-fazlani/csv-schema"))

lazy val root = project
  .in(file("."))
  .aggregate(csvSchema, csvSchemaExperimental, example)
  .settings(
    name := "csv-schema-root",
    publish / skip := true
  )

lazy val csvSchema = project
  .in(file("./csv-schema"))
  .settings(
    name := "csv-schema",
    libraryDependencies ++= Seq(
      Libs.zio,
      Libs.zioNio,
      Libs.zioPrelude,
      Libs.zioConfigYaml,
      Libs.zioConfigMagnolia,
      Libs.zioTest,
      Libs.zioTestSbt
    ),
    excludeDependencies += Libs.stdLib
  )

lazy val csvSchemaExperimental = project
  .in(file("./csv-schema-experimental"))
  .settings(
    name := "csv-schema-experimental",
    publish / skip := true,
    libraryDependencies ++= Seq(
      Libs.zio,
      Libs.hasher,
      Libs.zioNio,
      Libs.zioPrelude,
      Libs.zioConfigYaml,
      Libs.zioConfigMagnolia,
      Libs.zioTest,
      Libs.zioTestSbt
    ),
    excludeDependencies += Libs.stdLib
  )

lazy val example = project
  .in(file("./example"))
  .settings(
    name := "example",
    publish / skip := true,
    excludeDependencies += Libs.stdLib
  )
  .dependsOn(csvSchema, csvSchemaExperimental)
