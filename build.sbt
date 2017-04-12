name := "bw-assessment"

version := "1.0"

scalaVersion := "2.11.8"

lazy val root = (project in file(".")).enablePlugins(PlayScala, SwaggerPlugin, DockerPlugin)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "2.0.2",
  "org.postgresql" % "postgresql" % "42.0.0",
  "com.mohiva" %% "play-silhouette" % "4.0.0",
  "com.mohiva" %% "play-silhouette-persistence" % "4.0.0",
  "org.scala-lang.modules" %% "scala-async" % "0.9.6",
  "org.davidbild" % "tristate-play_2.11" % "0.2.0",
  filters
)

libraryDependencies ++= Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0",
  "org.scalacheck" %% "scalacheck" % "1.13.4",
  "com.h2database" % "h2" % "1.4.194",
  "org.flywaydb" % "flyway-core" % "4.1.2",
  "org.mockito" % "mockito-core" % "2.7.19",
  "com.ninja-squad" % "DbSetup" % "2.1.0",
  "com.mohiva" % "play-silhouette-testkit_2.11" % "4.0.0"
).map(_ % Test)

routesImport ++= Seq("controllers.api.TristateQueryBinder._")

flywayLocations := Seq("migrations")
flywayUrl := sys.env.getOrElse("DATABASE_URL", "")
flywayUser := sys.env.getOrElse("DATABASE_USER", "")
flywayPassword := sys.env.getOrElse("DATABASE_PASSWORD", "")
flywayOutOfOrder := true

fork in Test := true
javaOptions in Test += "-Dconfig.file=conf/test.conf"
parallelExecution in Test := false
coverageExcludedPackages := "<empty>;router\\..*;"

dockerExposedPorts := Seq(9000)
packageName in Docker := "bw-assessment/api"
dockerUpdateLatest := true
(stage in Docker) := (stage in Docker).dependsOn(swagger).value

swaggerDomainNameSpaces := Seq("controllers.api")
