val scala3Version = "3.1.2"

lazy val csvSchema = project
  .in(file("./csv-schema"))
  .settings(
    organization := "com.bilal-fazlani",
    name := "csv-validation",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
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
    organization := "com.bilal-fazlani",
    name := "example",
    publish / skip := true,
    scalaVersion := scala3Version,
    excludeDependencies += Libs.stdLib
  )
  .dependsOn(csvSchema)