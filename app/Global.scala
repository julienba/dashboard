import akka.actor._
import akka.util.Timeout

import scala.concurrent.duration._

import play.api._
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._

import models.Module
import crawler.Crawler

object Global extends GlobalSettings {
  
  val DEFAULT_NB_FETCH_FEED 	= 10
  val DEFAULT_CRAWLER_FREQUENCY = 2
  
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

