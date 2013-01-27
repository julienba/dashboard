package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json._

import anorm.NotAssigned

import models.{Tab, Module, User}
import json.Converter._
import form.CommonForm.reorderForm

object Tabs extends Controller with Secured {
      
  val tabForm = Form(
      single("title" -> nonEmptyText)
    )
  
  def save(username: String) = IsAuthenticated { user => implicit request =>
  	tabForm.bindFromRequest.fold(
  	  errors => BadRequest ,
  	  {
  	  	case(title) =>	
  	  	  val tab = Tab.save(Tab(NotAssigned, title, -1, username))
  	  	  Ok(toJson(tab))
  	  }
  	)
  }
  
  def json(username: String) = IsAuthenticated { user => _ =>
    val tabs = Tab.findAll(username)
    Ok(toJson(tabs))
  }
  
  def savePosition(username: String) = IsAuthenticated { user => implicit request =>
    reorderForm.bindFromRequest.fold(
      errors => BadRequest,
      {
        case(ids) => {
          Tab.savePosition(username, ids)
          Ok
        }
  	  }
    )
  }
}
