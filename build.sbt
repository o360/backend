name := "bw-assessment"

version := "1.0"

scalaVersion := "2.11.8"

lazy val root = (project in file(".")).enablePlugins(PlayScala, SwaggerPlugin, DockerPlugin)

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.2.0",
  "org.postgresql" % "postgresql" % "42.0.0",
  filters
) ++ Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test,
  "org.scalacheck" %% "scalacheck" % "1.13.4" % Test,
  "com.h2database" % "h2" % "1.4.194" % Test,
  "org.flywaydb" % "flyway-core" % "4.1.2" % Test,
  "org.mockito" % "mockito-core" % "2.7.19" % Test
)

flywayLocations := Seq("migrations")
flywayUrl := sys.env.getOrElse("DATABASE_URL", "")
flywayUser := sys.env.getOrElse("DATABASE_USER", "")
flywayPassword := sys.env.getOrElse("DATABASE_PASSWORD", "")
flywayOutOfOrder := true

fork in Test := true
javaOptions in Test += "-Dconfig.file=conf/test.conf"

dockerExposedPorts := Seq(9000)
packageName in Docker := "bw-assessment/api"
dockerUpdateLatest := true
(stage in Docker) := (stage in Docker).dependsOn(swagger).value

swaggerDomainNameSpaces := Seq("controllers.api")
