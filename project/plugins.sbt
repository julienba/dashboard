// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("play" % "sbt-plugin" % "2.0.4")

resolvers += Resolver.url("julienba.github.com", url("http://julienba.github.com/repo/"))(Resolver.ivyStylePatterns)

addSbtPlugin("org.jba" % "play2-plugins-mustache" % "1.0.1")