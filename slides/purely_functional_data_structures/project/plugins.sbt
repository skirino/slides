// Comment to get more information during initialization
logLevel := Level.Warn

// resolvers
resolvers += Resolver.sonatypeRepo("releases")

// sbt plugins
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.7.0")

addSbtPlugin("com.sksamuel.scapegoat" %% "sbt-scapegoat" % "0.94.6")

addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "1.1.0")
