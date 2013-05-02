package rss

import scala.concurrent.Future

import java.util.Date

import com.sun.syndication.io.impl.DateParser

import play.api.libs._
import play.api.libs.concurrent._
import play.api.libs.ws._
import play.api.Logger
import play.api.cache.Cache
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._

import models.{Module, ModuleStatus}

import scala.xml.{Node, NodeSeq}

case class Link(identifier: String, title: String, description: String, url: String, date: Date)

/**
 * Define one extractor by rss type
 */
trait LinksExtractor {
  val url: String
  def getLinks(response: Response): List[Link]
  val cacheExpiration: Int = 30
  
  def getIdentifier(title: String, description: String): String = 
    Codecs.sha1(title + description)
    
  def getDate(date: String): Date = {
    val result = DateParser.parseDate(date)
    if(result == null) new Date else result
  }  
  
  def markAsEmpty(moduleId: Long, url: String, rawFeeds: List[Node]) {
    if(rawFeeds.isEmpty) {
      Logger.warn("0 feeds for url: " + url)
      Module.updateStatus(moduleId, ModuleStatus.EMPTY)
    }    
  }
}

case class RSSExtractor(moduleId: Long, url: String) extends LinksExtractor {
  def getLinks(response: Response): List[Link] = {
	val rawFeeds = (response.xml \\ "rss" \ "channel" \ "item").toList
	markAsEmpty(moduleId, url, rawFeeds)
	
	rawFeeds.map { item =>
	  val title = (item \ "title") text
	  val description = (item \ "content") text
	  val feedUrl = (item \ "link" ) text
	  val dateAsString = (item \ "pubDate") text
	  
	  Link(getIdentifier(title, description), title, description, feedUrl, getDate(dateAsString))
	  
	} reverse
  }
}

case class AtomExtractor(moduleId: Long, url: String) extends LinksExtractor {
  
  private def attributeValue(attrs: (String, String)*)(n: Node) = {
    val t = n attribute "rel" exists (_ == "alternate")
    attrs forall { 
      case (key, value) => n attribute key exists (_.text == value)
    }  
  }
  
  def getLinks(response: Response): List[Link] = {
    
    val rawFeeds = (response.xml \\ "feed" \ "entry" ).toList
    markAsEmpty(moduleId, url, rawFeeds)
    
    rawFeeds.map { item =>  
      val title = (item \ "title") text
      val description = (item \ "content") text
      
      val links = (item \ "link" ).filter(attributeValue("rel" -> "alternate","type"  -> "text/html"))
      val feedUrl = links.headOption.map( l => l \"@href" text).getOrElse("TODO")

      val dateAsString = (item \ "updated") text
      
      Link(getIdentifier(title, description), title, description, feedUrl, getDate(dateAsString))
      
    } reverse
  }
}

/**
 * Generic fetcher
 */
object RSSFetcher {
  
  /**
   * Try to find a type for random rssURL
   */
  def findType(rssURL: String): Future[Option[String]] = {
    for {
      rssLinks <- RSSFetcher.fetch(RSSExtractor(-1, rssURL))
      atomLinks <- RSSFetcher.fetch(AtomExtractor(-1, rssURL))
    } yield {
      
      if(!rssLinks.isEmpty) Some("application/rss+xml")
      else if (!atomLinks.isEmpty) Some("application/atom+xml")
      else None  
    }
  }
  
  def fetch(implicit r: LinksExtractor): Future[List[Link]] =
    cacheValue.map(Promise.pure(_)).getOrElse(retrieve)
  
  def retrieve(implicit r: LinksExtractor): Future[List[Link]] = {
    
    WS.url(r.url).get().map { response =>
      try {
        cacheValue(r.getLinks(response))
      } catch {
        case e: Exception => {
          Logger.error("error during parsing of: " + r.url, e)
          Nil
        }
      }
    }
  }  
  
  /**
   * get a cache value
   */
  def cacheValue(implicit r: LinksExtractor): Option[List[Link]] = Cache.getAs[List[Link]](r.url)
  
  /**
   * set a cache value
   */
  def cacheValue(links: List[Link])(implicit r: LinksExtractor): List[Link] = {
    
    Cache.set(r.url, links, r.cacheExpiration)
    links
  }  
}
