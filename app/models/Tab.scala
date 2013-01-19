package models


import anorm._
import anorm.SqlParser._

import play.api.db._
import play.api.Play.current
import play.api.Logger

case class Tab(
    id: Pk[Long] = NotAssigned,
    title: String, 
    position: Int,
    user: String
  )
  
object Tab {
  
  val simple = {
    get[Pk[Long]]("tab.id") ~
    get[String]("tab.title") ~   
    get[Int]("tab.position") ~
    get[String]("tab.users") map {   
      case id~title~position~user => Tab(id, title, position, user)
    } 
  }  
  
  def findAll(user: String) = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM tab WHERE users = {user}")
      	.on('user -> user)
      	.as(Tab.simple *)
    }    
  }
  
  def find(id: Long): (Option[Tab], List[Module]) = {
    DB.withConnection { implicit connection =>
      val tab = SQL("SELECT * FROM tab WHERE id = {id}")
        			.on('id -> id)
        			.as(Tab.simple.singleOpt)
        	
      val modules = tab.map( t => Module.selectAll(id)).getOrElse(Nil)
      (tab, modules)
    }
  }
  
  def save(tab: Tab): Tab = {
    DB.withConnection { implicit connection =>
      
      val id = tab.id.getOrElse {
        SQL("SELECT nextval('tab_id_seq')").as(scalar[Long].single)
      }
      
      SQL("INSERT INTO tab VALUES({id}, {title}, {position}, {user})")
      	.on('id -> id, 'title -> tab.title, 'position -> tab.position, 'user -> tab.user)
      	.executeUpdate() 

      tab.copy(id = Id(id))	
    }
  }

  def isOwner(tabId: Long, username: String): Boolean = {
    DB.withConnection { implicit connection =>
      SQL("select count(id) = 1 FROM tab where id = {id} AND users = {user}")
        .on('id -> tabId, 'user -> username)
        .as(scalar[Boolean].single)
    }
  }
  
}
