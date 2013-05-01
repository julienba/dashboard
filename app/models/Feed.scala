package models

import anorm._
import anorm.SqlParser._

import play.api.db._
import play.api.Play.current
import play.api.Logger

import java.util.Date


case class Feed(
    id:Pk[Long] = NotAssigned, 
    identifier: String,
    moduleId: Long,
    title: String,
    url: String,
    description: String,
    pubDate: Date,
    read: Boolean
  )
  
object Feed {
  
  /**
   * PARSER
   */
  val simple = {
    get[Pk[Long]]("feed.id") ~
    get[String]("feed.identifier") ~
    get[Long]("feed.moduleId") ~
    get[String]("feed.title") ~
    get[String]("feed.url") ~
    get[String]("feed.description") ~
    get[Date]("feed.pubDate") ~
    get[Boolean]("feed.read") map {
      case id~identifier~moduleId~title~url~description~pubDate~read => Feed(id, identifier, moduleId, title, url, description, pubDate, read)
    }
  }

  /**
   * CHAIN
   */      
  private[models] def select(module: Module, link: rss.Link)(implicit connection: java.sql.Connection): Option[Feed] = {
    SQL("SELECT * FROM feed WHERE moduleId = {module} AND identifier = {identifier}")
      .on('module -> module.id.get, 'identifier -> link.identifier)
      .as(Feed.simple.singleOpt)
  }
  
  /**
   * UTIL
   */
  private def cleanDescription(desc: String) = {
    if(desc.isEmpty()) ""
    else 
      if(desc.size > 1024) 
        desc.substring(0, 1023)
      else desc    
  }  
  /**
   * QUERY
   */    
  def create(module: Module, links: List[rss.Link]): List[Feed] = {
    DB.withConnection { implicit connection =>
      try {
        for(link <- links) yield {
        
          val result = 
            select(module, link).map { feed =>
              feed
            } getOrElse {
              insert(Feed(
                  NotAssigned, 
                  link.identifier, 
                  module.id.get, 
                  link.title, 
                  link.url, 
                  cleanDescription(link.description), 
                  link.date, 
                  false
                ))            
            }
          result  
        }
      }catch {
        case e: Exception => {
          Logger.error("an exception occurred", e)
          Nil
        }
      }
    }
  }

  private[models] def insert(feed: Feed): Feed = {
    Logger.debug("insert feed: " + feed.url)    
	DB.withConnection { implicit connection =>
	  // select nextval(‘project_seq’)
	  //val id = SQL("select next value for feed_id_seq").as(scalar[Long].single)
	  val id = SQL("select nextval('feed_id_seq')").as(scalar[Long].single)
      
	  SQL("""
	      INSERT INTO feed (id, identifier, moduleId, title, url, description, pubDate, read)
	      VALUES ({id}, {identifier}, {moduleId}, {title}, {url}, {description}, {pubDate}, {read})
	      """)
	      .on('id -> id, 'identifier -> feed.identifier, 'moduleId -> feed.moduleId, 'title -> feed.title, 'url -> feed.url,
	    		  'description -> feed.description, 'pubDate -> feed.pubDate, 'read -> feed.read)
	      .executeUpdate()
	      
	  feed.copy(id = Id(id))   
    }
  }
  
  def findByModule(moduleId: Long) = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM feed WHERE moduleId = {moduleId} ORDER BY pubDate DESC limit 10")
      	.on('moduleId -> moduleId)
      	.as(Feed.simple *)
    }
  }
  
  def markAsRead(id: Long){                                                                                                                                                                                         
    DB.withConnection { implicit connection =>
      SQL ("UPDATE feed SET read=true where id = {id}")
        .on('id -> id)         
        .executeUpdate()       
    }
  }  
}