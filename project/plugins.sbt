addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.13")

addSbtPlugin("com.iheart" % "sbt-play-swagger" % "0.5.2")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.5")

addSbtPlugin("org.flywaydb" % "flyway-sbt" % "4.1.2")

resolvers += "Flyway" at "https://flywaydb.org/repo"
