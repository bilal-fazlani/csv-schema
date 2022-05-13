ThisBuild / scalaVersion := "3.1.2"
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
ThisBuild / homepage := Some(url("https://github.com/bilal-fazlani/csv-schema"))

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

lazy val example = project
  .in(file("./example"))
  .settings(
    name := "example",
    publish / skip := true,
    excludeDependencies += Libs.stdLib
  )
  .dependsOn(csvSchema)
