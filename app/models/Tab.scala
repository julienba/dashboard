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
  /**
   * PARSER
   */
  val simple = {
    get[Pk[Long]]("tab.id") ~
    get[String]("tab.title") ~   
    get[Int]("tab.position") ~
    get[String]("tab.users") map {   
      case id~title~position~user => Tab(id, title, position, user)
    } 
  }  

  /**
   * CHAIN QUERY
   */  
  def select(id: Long)(implicit connection: java.sql.Connection): Option[Tab] = {
    SQL("SELECT * FROM tab WHERE id = {id}")
      .on('id -> id)
      .as(Tab.simple.singleOpt)    
  }
  
  /**
   * QUERY
   */  
  def findAll(user: String) = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM tab WHERE users = {user} ORDER BY position")
      	.on('user -> user)
      	.as(Tab.simple *)
    }    
  }
  
  def find(id: Long): (Option[Tab], List[Module]) = {
    DB.withConnection { implicit connection =>
      val tab = select(id)
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

  def savePosition(username: String, positions: List[Int]) {
    DB.withConnection { implicit connection =>
      for( (id, index) <- positions.zipWithIndex){
        SQL("UPDATE tab SET position={position} WHERE id={id} AND users={user}")
          .on('position -> index, 'id -> id, 'user -> username)
          .executeUpdate()
      }
    }
  }
  
  def isOwner(tabId: Long, username: String): Boolean = {
    DB.withConnection { implicit connection =>
      SQL("SELECT COUNT(id) = 1 FROM tab WHERE id = {id} AND users = {user}")
        .on('id -> tabId, 'user -> username)
        .as(scalar[Boolean].single)
    }
  }
  
}
