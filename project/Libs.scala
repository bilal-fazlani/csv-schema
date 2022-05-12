import sbt._

object Libs {
  val zioVersion = "2.0.0-RC6"
  
  val zioOrg = "dev.zio"

  lazy val zio = zioOrg %% "zio" % zioVersion
  lazy val zioNio = zioOrg %% "zio-nio" % "2.0.0-RC7"
  lazy val zioPrelude = zioOrg %% "zio-prelude" % "1.0.0-RC14"

  val zioConfigVersion = "3.0.0-RC9"
  lazy val zioConfigMagnolia =
    zioOrg %% "zio-config-magnolia" % zioConfigVersion
  lazy val zioConfigYaml = zioOrg %% "zio-config-yaml" % zioConfigVersion
//------------------------------------------------------------------------------

  lazy val stdLib = "org.scala-lang.modules" % "scala-collection-compat_2.13"
  lazy val zioTest = zioOrg %% "zio-test" % zioVersion % Test
  lazy val zioTestSbt = zioOrg %% "zio-test-sbt" % zioVersion % Test
}
