package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json._

import rss.{RSSURL, HTMLFetcher, RSSFetcher}
import crawler.Crawler
import models.{Module, Feed}
import json.Converter._
import form.CommonForm.reorderForm

case class Website(url: Option[String], rss: Option[String])

object Modules extends Controller with Secured {
  
  val websiteForm = Form(
    mapping(
      "url" -> optional(text),
      "rss" ->optional(text)
    )(Website.apply)(Website.unapply)
  )
  
  val rssForm = Form(
      mapping(
        "title" -> text,
        "websiteUrl" -> text,
        "url" -> text,
        "type" -> text
      )(RSSURL.apply)(RSSURL.unapply)
    )

  def findRSS(tabId: Long) = IsAuthenticated { username => implicit request =>
    websiteForm.bindFromRequest.fold(
      errors => BadRequest,
      website => {
        if(website.url.isDefined) {
          val promise = HTMLFetcher.findRSS(website.url.get)
          Async { promise.map(links => Ok(toJson(links))) }
        } else if (website.rss.isDefined) {
          
          val websiteURL = HTMLFetcher.extractWebsiteURL(website.rss.get)
          Async {
            RSSFetcher.findType(website.rss.get).map { optURL =>
              optURL match {
                case Some(typeStr) =>
                  Ok(toJson(RSSURL(websiteURL, websiteURL, website.rss.get, typeStr)))
                case None => NotFound  
              }  
            }
          }
        } else NotFound
      }
    )
  }

  def create(tabId: Long) = IsOwnerOf(tabId) { user => implicit request =>
    rssForm.bindFromRequest.fold(
      errors => BadRequest,
      rss => {
        val module = Module.save(tabId, rss)
        Ok
      }
    )
  }

  def json(tabId: Long) = IsAuthenticated { username => implicit request =>    
    val modules = Module.findAll(tabId).map( module => module.copy(feeds = Feed.findByModule(module.id.get)) )
    
    Ok(toJson(modules))
  }  

  def fetch(id: Long) = Action {
    Module.find(id).map { module =>
      val promiseLinks = Crawler(module)
      Async {
        promiseLinks.map(links => Ok(toJson(links.reverse.take(10))))
      }
    }.getOrElse(NotFound)
  }
  
  val editForm = Form(tuple(
      "id" -> number,
      "title" -> text,
      "websiteUrl" -> text,
      "url" -> text,
      "type" -> text
    ))
  
  def saveJS = IsAuthenticated { username => implicit request =>
    editForm.bindFromRequest.fold(
      erros => BadRequest,
      {
        case(id, title, websiteUrl, url, typeStr) => {
          Module.edit(id, title, websiteUrl, url, typeStr)
          Ok
        }
      }
    )
  }
  
  def delete(tabId: Long, id: Long) = IsOwnerOf(tabId) { user => implicit request =>
    Module.delete(id)
    Ok
  }
  
  def savePosition(tabId: Long) = IsOwnerOf(tabId) { user => implicit request =>
    reorderForm.bindFromRequest.fold(
      errors => BadRequest,
      {
        case(ids) => {
          
          Module.savePosition(ids)
          Ok
        }
  	  }
    )
  }
  
}
