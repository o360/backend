addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.3")

addSbtPlugin("com.iheart" % "sbt-play-swagger" % "0.6.2-PLAY2.6")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.5")

addSbtPlugin("org.flywaydb" % "flyway-sbt" % "4.1.2")

addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "1.10")

resolvers += "Flyway" at "https://flywaydb.org/repo"
