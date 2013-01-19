package controllers

import play.api._
import play.api.mvc._

import models.Feed

object Feeds extends Controller with Secured {
  
  def read(id: Long) = IsAuthenticated { username => implicit request =>
    Feed.markAsRead(id)
    Ok
  }
}