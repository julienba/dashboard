package models                                                                                                                                                                                                                               

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class User(email: String, name: String, password: String)

object User {

  val simple = {
    get[String]("users.mail") ~
    get[String]("users.name") ~
    get[String]("users.password") map {
      case mail~name~password => User(mail, name, password)
    }
  }

  def findByEmail(mail: String): Option[User] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM users WHERE mail = {mail}")
      	.on('mail -> mail)
      	.as(User.simple.singleOpt)
    }
  }  
  
  def authenticate(mail: String, password: String): Option[User] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM users WHERE mail = {mail} AND password = {password}")
      	.on('mail -> mail, 'password -> password)
      	.as(User.simple.singleOpt)
    }
  }
  
}