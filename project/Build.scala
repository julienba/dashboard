import sbt._
import Keys._
import play.Project._
import org.jba.sbt.plugin.MustachePlugin._

object ApplicationBuild extends Build {

    val appName         = "dashboard"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      // play
      jdbc,
      anorm,
      javaJpa,
      javaJdbc,
      // Add your project dependencies here,
      "org.jsoup" % "jsoup" % "1.6.1",
      "postgresql" % "postgresql" % "9.1-901.jdbc4",
        
      // Date in java, always a pleasure...  
      "net.java.dev.rome" % "rome" % "1.0.0", // Currently just for DateParser
      "org.ocpsoft.prettytime" % "prettytime" % "1.0.8.Final",
      
      // Mustache
      "org.jba" %% "play2-mustache" % "1.1.2"
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(

      resolvers += Resolver.url("julienba.github.com", url("http://julienba.github.com/repo/"))(Resolver.ivyStylePatterns),
    
      // cloudfoundry
      unmanagedBase <<= baseDirectory { base => base / "lib" },
  
      // Mustache settings
      mustacheEntryPoints <<= (sourceDirectory in Compile)(base => base / "assets" / "mustache" ** "*.html"),

      mustacheOptions := Seq.empty[String],
      resourceGenerators in Compile <+= MustacheFileCompiler,        
      templatesImport += "org.jba.Mustache"
    )

}
