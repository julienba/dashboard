import akka.actor._
import akka.util.duration._
import akka.util.Timeout

import play.api._
import play.api.libs.concurrent._
import play.api.Play.current

import models.Module
import crawler.Crawler

object Global extends GlobalSettings {
  
  val DEFAULT_NB_FETCH_FEED 	= 10
  val DEFAULT_CRAWLER_FREQUENCY = 2 //minutes
  
  override def onStart(app: Application) {

    if(Play.configuration.getBoolean("actor.active").getOrElse(false)){
      Logger.info("Actor is starting")
      val rssActor = Akka.system.actorOf(Props[DummyRSSFetcher])
      Akka.system.scheduler.schedule(
        10 seconds,
        DEFAULT_CRAWLER_FREQUENCY minutes,
        rssActor,
        "tick"
      )
    }
  }
}

class DummyRSSFetcher extends Actor {
  def receive = {
    case x => {
      // Basic fetcher
      Logger.info("Fetch all RSS")
      //Module.findAll.map(Crawler(_))
      Module.findOlder(Global.DEFAULT_NB_FETCH_FEED).map(Crawler(_))
    }
  }
}

