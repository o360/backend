name := "open360"

version := "1.0"

scalaVersion := "2.13.1"

lazy val root =
  (project in file(".")).enablePlugins(PlayScala, SwaggerPlugin, DockerPlugin, FlywayPlugin)

scalacOptions ++= Seq(
  "-Xlint:inaccessible",
  "-Xlint:unused",
  s"-P:silencer:sourceRoots=${baseDirectory.value.getCanonicalPath}",
  s"-P:silencer:pathFilters=routes/.*"
)

libraryDependencies ++= Seq(
  compilerPlugin("com.github.ghik" %% "silencer-plugin" % "1.4.4" % Provided cross CrossVersion.full),
  "com.github.ghik" %% "silencer-lib" % "1.4.4" % Provided cross CrossVersion.full
)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "4.0.2",
  "org.postgresql" % "postgresql" % "42.0.0",
  "com.mohiva" %% "play-silhouette" % "6.1.0",
  "com.mohiva" %% "play-silhouette-persistence" % "6.1.0",
  "com.typesafe.play" %% "play-mailer" % "8.0.0",
  "com.typesafe.play" %% "play-mailer-guice" % "8.0.0",
  "org.scalaz" %% "scalaz-core" % "7.2.30",
  "com.google.apis" % "google-api-services-sheets" % "v4-rev473-1.22.0",
  "com.google.apis" % "google-api-services-drive" % "v3-rev74-1.22.0",
  "org.flywaydb" % "flyway-core" % "4.1.2",
  "io.scalaland" %% "chimney" % "0.4.1",
  "commons-io" % "commons-io" % "2.6",
  filters,
  guice
)

libraryDependencies ++= Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3",
  "org.scalacheck" %% "scalacheck" % "1.14.1",
  "com.h2database" % "h2" % "1.4.194",
  "org.mockito" % "mockito-core" % "2.7.19",
  "com.ninja-squad" % "DbSetup" % "2.1.0",
  "com.mohiva" %% "play-silhouette-testkit" % "6.1.0"
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

dockerBaseImage := "openjdk:8"
dockerExposedPorts := Seq(9000)
packageName in Docker := "open360/api"
dockerUpdateLatest := true
(stage in Docker) := (stage in Docker).dependsOn(swagger).value

swaggerDomainNameSpaces := Seq("controllers.api")

scalafmtOnCompile := true
