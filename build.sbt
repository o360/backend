name := "open360"

version := "1.0"

scalaVersion := "2.12.3"

lazy val root =
  (project in file(".")).enablePlugins(PlayScala, SwaggerPlugin, DockerPlugin)

scalacOptions ++= Seq(
  "-Ywarn-inaccessible",
  "-Ywarn-unused",
  "-Ywarn-unused-import"
)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "3.0.0",
  "org.postgresql" % "postgresql" % "42.0.0",
  "org.davidbild" %% "tristate-play" % "0.3.0",
  "com.mohiva" %% "play-silhouette" % "5.0.7",
  "com.mohiva" %% "play-silhouette-persistence" % "5.0.7",
  "com.typesafe.play" %% "play-mailer" % "6.0.1",
  "com.typesafe.play" %% "play-mailer-guice" % "6.0.1",
  "org.scalaz" %% "scalaz-core" % "7.2.10",
  "com.google.apis" % "google-api-services-sheets" % "v4-rev473-1.22.0",
  "com.google.apis" % "google-api-services-drive" % "v3-rev74-1.22.0",
  "org.flywaydb" % "flyway-core" % "4.1.2",
  "io.scalaland" %% "chimney" % "0.1.6",
  "commons-io" % "commons-io" % "2.6",
  filters,
  guice
)

libraryDependencies ++= Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.1",
  "org.scalacheck" %% "scalacheck" % "1.13.4",
  "com.h2database" % "h2" % "1.4.194",
  "org.mockito" % "mockito-core" % "2.7.19",
  "com.ninja-squad" % "DbSetup" % "2.1.0",
  "com.mohiva" %% "play-silhouette-testkit" % "5.0.7"
).map(_ % Test)

routesImport ++= Seq("controllers.api.TristateQueryBinder._")

flywayLocations := Seq("migrations/common", "migrations/postgres")
flywayUrl := sys.env.getOrElse("DATABASE_URL", "")
flywayUser := sys.env.getOrElse("DATABASE_USER", "")
flywayPassword := sys.env.getOrElse("DATABASE_PASSWORD", "")
flywayOutOfOrder := true

fork in Test := true
javaOptions in Test += "-Dconfig.file=conf/test.conf"
parallelExecution in Test := false
coverageExcludedPackages := "<empty>;router\\..*;"

dockerExposedPorts := Seq(9000)
packageName in Docker := "open360/api"
dockerUpdateLatest := true
(stage in Docker) := (stage in Docker).dependsOn(swagger).value

swaggerDomainNameSpaces := Seq("controllers.api")

scalafmtOnCompile := true
