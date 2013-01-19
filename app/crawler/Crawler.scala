package crawler

import play.api.Play.current
import play.api.Logger
import play.api.libs.concurrent._

import models.{Feed, Module, ModuleStatus}
import rss._

object Crawler {
  
  def apply(module: Module): Promise[List[Feed]] = {
    fetch(module).map(Feed.create(module, _))
  }
  
  private def fetch(module: Module): Promise[List[Link]] = {
    
    Module.updateLastupdate(module.id.get)
    
    try {
      module.typeStr match {
        case "application/rss+xml" => 
          RSSFetcher.fetch(RSSExtractor(module.id.get, module.url))
          
        case "application/atom+xml" =>
          RSSFetcher.fetch(AtomExtractor(module.id.get, module.url))          
          
        case _ => {
          Logger.error("Unknow module type: " + module.typeStr)
          Module.updateStatus(module.id.get, ModuleStatus.ERROR)
          PurePromise(Nil)
        }
      }    
    } catch {
      case e: Exception => {
        Logger.error("Unexpected exception during crawling", e)
        Module.updateStatus(module.id.get, ModuleStatus.ERROR)
        PurePromise(Nil)
      }
    }
  }

}
