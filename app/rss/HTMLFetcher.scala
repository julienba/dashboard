package rss

import play.api.libs.ws.WS
import play.api.libs.concurrent._
import play.api.Logger

import org.jsoup.Jsoup
import org.jsoup.select.Elements

import scala.collection.JavaConverters._

import java.net.URI

case class RSSURL(title: String, websiteUrl: String, url: String, typeStr: String)

object HTMLFetcher {
  
  /**
   * <link rel="alternate" type="application/atom+xml" title="Run Tings Proper - Atom" href="http://runtingsproper.blogspot.com/feeds/posts/default" />
   * <link rel="alternate" type="application/rss+xml" title="Run Tings Proper - RSS" href="http://runtingsproper.blogspot.com/feeds/posts/default?alt=rss" />
   */
  def findRSS(url: String): Promise[List[RSSURL]] = {
    Logger.debug("Parse website: " + url)
    WS.url(url).get.map{ html =>
      
      val titles = Jsoup.parse(html.body).select("title").asScala
      val title: String = if(!titles.isEmpty) titles(0).html() else url
      val links = Jsoup.parse(html.body).select("link[rel=alternate]")
      
      val result = 
        for(link <- links.asScala) yield {
          val typeStr = link.attr("type")
          val href = link.attr("href") 
          // val title = link.attr("title") // Title is wrong in most of my favorite website :/         
          RSSURL(title, url, cleanURL(url, href), typeStr)
        }
      result.toList
    }
  }

  // TODO: improve
  private def cleanURL(webUrl: String, href: String): String = {
    var res = ""
    if(!href.contains("http")) {
      val uri = new URI(webUrl)
      res = uri.getScheme + "://" + uri.getHost
      if(!res.endsWith("/"))
        res += "/"

      if(res.endsWith("/") && href.startsWith("/"))
        res = res.substring(0, res.length - 1)
    }
      
    res + href
  }
  
  def extractWebsiteURL(rssURL: String) = {
    val uri = new URI(rssURL)
    uri.getScheme + "://" + uri.getHost
  }
  
}
