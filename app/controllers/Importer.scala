package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Akka
import play.api.Play.current

import scala.xml._

import anorm.NotAssigned

import models.{Tab, Module}
import rss._

object Importer extends Controller with Secured {
    
  /**
   * Format example:
   * 	<body>
        	<outline text="Main" title="Main" icon="http://www.netvibes.com/img/uwa.png">
        		<outline xmlUrl="http://linuxfr.org/news.atom" htmlUrl="http://linuxfr.org/news" text="LinuxFr.org : les dépêches" title="LinuxFr.org : les dépêches" description="" />
        	    <outline provider="rue89" url="" title="Rue89 : A la Une" text="Rue89 : A la Une">
                	<outline xmlUrl="http://www.rue89.com/feed" text="A la une" title="A la une" />
                	<outline xmlUrl="http://www.rue89.com/rue89-politique/feed" text="Rue89 Politique" title="Rue89 Politique" />
 
   */
  def netvibes(username: String) = IsAuthenticated(parse.multipartFormData) { username => request =>
    request.body.file("file").map { file =>
      import java.io.File

      Logger.info("upload netvibes XML")
      Akka.future(parseNetvibesXML(username, file.ref.file))
      Application.Home
      
    } getOrElse  BadRequest
  }
  private def parseNetvibesXML(username: String, file: java.io.File) {
    val loadNode = xml.XML.loadFile(file)
    val outlines = loadNode \ "body" \ "outline"
    
    for(tab <- outlines) {
      val tabTitle = (tab \ "@title").text
      println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ tab: " + tabTitle)
      // Create Tab
      Logger.debug("Create tab %s for user %s".format(tabTitle, username))
      val tabEntity = Tab.save(Tab(NotAssigned, tabTitle, -1, username))
      
      val modules = tab \ "outline"
      
      for(module <- modules) {
        val title = (module \ "@title").text
        val htmlUrl = (module \ "@htmlUrl").text
        
        if(htmlUrl.isEmpty()) {
          val innerModule = module \ "outline"
          for(inner <- innerModule) {
            val xmlUrl = (inner \ "@xmlUrl").text
            //TODO: do nothing for the moment
          }
        } else {
          // create module
          val promise = HTMLFetcher.findRSS(htmlUrl)
          promise.map { links =>
            if(links.isEmpty){
              Logger.info("Canot retrieve rss feed for url: " + htmlUrl)
            } else {
              
              Module.save(tabEntity.id.get, links(0))
              Logger.debug("Create module %s in tab %s for user %s with links %s - %s".format(htmlUrl, tabEntity.title, username, links(0).title, links(0).url))
            }
          }
          //TODO create module
          //println("title: %s \t htmlUrl: %s".format(title, htmlUrl) )  
        }
        
      }
    }
  }
}
