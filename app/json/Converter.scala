package json

import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.json.Generic._

import models._
import util.Time
import rss.RSSURL

/**
 * Convert models to json
 */
object Converter {

  implicit object TabFormat extends Writes[Tab] {
    def writes(o: Tab) = JsObject(List(
	    "id" -> JsNumber(o.id.get),
	    "title" -> JsString(o.title),
	    "position" -> JsNumber(o.position)
	  ))
  }
	
  implicit object ModuleFormat extends Writes[Module] {
	def writes(o: Module) = JsObject(List(
	    "id" -> JsNumber(o.id.get),
	    "title" -> JsString(o.title),
	    "websiteUrl" -> JsString(o.websiteUrl),
	    "url" -> JsString(o.url),
	    "status" -> JsString(o.status),
        "type" -> JsString(o.typeStr),
	    "feeds" -> JsArray(o.feeds.map(FeedFormat.writes(_)))
	  ))
  }
	
  implicit object FeedFormat extends Writes[Feed] {
    def writes(o: Feed) = JsObject(List(
	    "id" -> JsNumber(o.id.get),
	    "title" -> JsString(o.title),
	    "url" -> JsString(o.url),
	    "date" -> JsString(Time.prettify(o.pubDate)),
	    "read" -> JsBoolean(o.read)
	  ))
  }
  
  implicit object RSSURLFormat extends Writes[RSSURL] {
    def writes(o: RSSURL) = JsObject(List(
        "title" -> JsString(o.title),
        "websiteUrl" -> JsString(o.websiteUrl),
        "url" -> JsString(o.url),
        "type" -> JsString(o.typeStr)
      ))
  }	
}
