addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.7.4")

addSbtPlugin("com.iheart" % "sbt-play-swagger" % "0.9.1-PLAY2.7")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.6.1")

addSbtPlugin("io.github.davidmweber" % "flyway-sbt" % "6.2.0")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.3.0")

addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.2.7")

resolvers += "Flyway" at "https://flywaydb.org/repo"
