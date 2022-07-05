import sbt._

object Libs {
  val zioVersion = "2.0.0"
  
  val zioOrg = "dev.zio"

  lazy val zio = zioOrg %% "zio" % zioVersion
  lazy val zioNio = zioOrg %% "zio-nio" % "2.0.0"
  lazy val zioPrelude = zioOrg %% "zio-prelude" % "1.0.0-RC15"

  val zioConfigVersion = "3.0.1"
  lazy val zioConfigMagnolia =
    zioOrg %% "zio-config-magnolia" % zioConfigVersion
  lazy val zioConfigYaml = zioOrg %% "zio-config-yaml" % zioConfigVersion
  lazy val hasher = ("com.outr" %% "hasher" % "1.2.2").cross(CrossVersion.for3Use2_13)
//------------------------------------------------------------------------------

  lazy val stdLib = "org.scala-lang.modules" % "scala-collection-compat_2.13"
  lazy val zioTest = zioOrg %% "zio-test" % zioVersion % Test
  lazy val zioTestSbt = zioOrg %% "zio-test-sbt" % zioVersion % Test
}
