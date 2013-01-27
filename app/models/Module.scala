package models

import anorm._
import anorm.SqlParser._

import play.api.db._
import play.api.Play.current
import play.api.Logger

import java.util.Date

object ModuleStatus extends Enumeration {
  type ModuleStatus = Value
  val OK = Value("OK")
  val ERROR = Value("ERROR")
  val EMPTY = Value("EMPTY")
  val UNKNOW = Value("UNKNOW")
}

import ModuleStatus._

case class Module(
    id: Pk[Long] = NotAssigned,
    tabId: Long,
    title: String,
    websiteUrl: String,
    url: String,  
    status: String,
    lastUpdate: Date,
    typeStr: String,
    position: Int,
    feeds: List[Feed] = Nil
  )
  
object Module extends AnormExtension {
    
  val simple = {
    get[Pk[Long]]("module.id") ~
    get[Long]("module.tabId") ~
    get[String]("module.title") ~
    get[String]("module.website_url") ~
    get[String]("module.url") ~
    get[String]("module.status") ~
    get[Date]("module.lastUpdate") ~
    get[String]("module.type") ~
    get[Int]("module.position") map {   
      case id~tabId~title~websiteUrl~url~status~lastUpdate~typeStr~position => Module(id, tabId, title, websiteUrl, url, status, lastUpdate, typeStr, position)
    } 
  }
  
  /**
   * CHAIN QUERY
   */
  def selectAll(tabId: Long)(implicit connection: java.sql.Connection) = {
    SQL("SELECT * FROM module WHERE tabId = {tabId}")
      .on('tabId -> tabId)
      .as(Module.simple *)    
  }
  
  /**
   * QUERY
   */
  def save(tabId: Long, rssURL: rss.RSSURL): Module = {
    DB.withConnection { implicit connection =>
      
      val id = SQL("SELECT nextval('module_id_seq')").as(scalar[Long].single) 
      
      val lastUpdate = new Date
      val position = -1
      
      SQL("INSERT INTO module VALUES({id}, {tabId}, {title}, {websiteUrl}, {url}, {status}, {lastUpdate}, {type}, {position})")
      	.on('id -> id, 'tabId -> tabId, 'title -> rssURL.title, 'websiteUrl -> rssURL.websiteUrl, 'url -> rssURL.url, 'type -> rssURL.typeStr, 'status -> "OK", 'lastUpdate -> lastUpdate, 'position -> position)
      	.executeUpdate()
      	
      Module(Id(id), tabId, rssURL.title, rssURL.websiteUrl, rssURL.url, "OK", lastUpdate, rssURL.typeStr, position)	
    }    
  }
  
  def edit(id: Long, title: String, websiteUrl: String, url: String, typeStr: String) {
    DB.withConnection { implicit connection =>
      SQL("UPDATE module SET title= {title}, website_url = {websiteUrl}, url= {url}, type = {type} WHERE id = {id}")
        .on('id -> id, 'title -> title, 'websiteUrl -> websiteUrl, 'url -> url, 'type -> typeStr)
        .executeUpdate()
    }
  }
  
  def findAll(): List[Module] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM module")
      	.as(Module.simple *)
    }
  }
  
  def findOlder(nb: Int): List[Module] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM module WHERE status = 'OK' ORDER BY lastUpdate ASC LIMIT {limit}")
      	.on('limit -> nb)
      	.as(Module.simple *)
    }    
  }
  
  def findAll(tabId: Long): List[Module] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM module WHERE tabId = {tabId} ORDER BY position")
      	.on('tabId -> tabId)
      	.as(Module.simple *)
    }
  }  

  def find(id: Long): Option[Module] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM module where id = {id}")
      	.on('id -> id)
      	.as(Module.simple.singleOpt)
    }
  }

  def updateStatus(id: Long, status: ModuleStatus) {
    DB.withConnection { implicit connection =>
      SQL("UPDATE module SET status={status} WHERE id = {id}")
        .on('id -> id, 'status -> status.toString)
        .executeUpdate
    }
  }
  
  def updateLastupdate(id: Long) {
    DB.withConnection { implicit connection =>
      SQL("UPDATE module SET lastUpdate={lastUpdate} WHERE id = {id}")
        .on('id -> id, 'lastUpdate -> new Date)
        .executeUpdate
    }    
  }
  
  def delete(id: Long) {
    DB.withConnection { implicit connection =>
      SQL("DELETE from module where id = {id}")
      	.on('id -> id)
      	.executeUpdate
    }
  }
  
  def savePosition(positions: List[Int]) {
    DB.withConnection { implicit connection =>
      for( (id, index) <- positions.zipWithIndex){
        SQL("UPDATE module SET position={position} WHERE id={id}")
          .on('position -> index, 'id -> id)
          .executeUpdate()
      }
    }
  }  
}
