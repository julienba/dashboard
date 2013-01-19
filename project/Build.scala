import sbt._
import Keys._
import PlayProject._
import org.jba.sbt.plugin.MustachePlugin._

object ApplicationBuild extends Build {

    val appName         = "dashboard"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      // Add your project dependencies here,
      "org.jsoup" % "jsoup" % "1.6.1",
      "postgresql" % "postgresql" % "9.1-901.jdbc4",
        
      // Date in java, always a pleasure...  
      "net.java.dev.rome" % "rome" % "1.0.0", // Currently just for DateParser
      "org.ocpsoft.prettytime" % "prettytime" % "1.0.8.Final",
      
      // Mustache
      "org.jba" %% "play2-mustache" % "1.0.2",
      "com.twitter" %% "util-core" % "4.0.1"       
      
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(

      resolvers += Resolver.url("julienba.github.com", url("http://julienba.github.com/repo/"))(Resolver.ivyStylePatterns),
      
      // Mustache settings
      mustacheEntryPoints <<= (sourceDirectory in Compile)(base => base / "assets" / "mustache" ** "*.html"),

      mustacheOptions := Seq.empty[String],
      resourceGenerators in Compile <+= MustacheFileCompiler,        
      templatesImport += "org.jba.Mustache"
    )

}
