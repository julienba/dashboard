package controllers
 
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import play.api.libs.json.Json._
import play.api.libs.json._

import models.{User, Tab}

object Application extends Controller with Secured {

  val loginForm = Form(
    tuple(
      "mail" -> text,
      "password" -> text
    ) verifying ("Invalid mail or password", result => result match {
      case (mail, password) => User.authenticate(mail, password).isDefined
    })
  ) 
  
  val Home = Redirect(routes.Application.index)      
  
  def login = Action { implicit request =>
    Ok(views.html.login(loginForm))  
  }  
  
  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(   
      formWithErrors => BadRequest(views.html.login(formWithErrors)),
      user => Redirect(routes.Application.index).withSession("mail" -> user._1)
    )
  }
  
  def logout = Action {
    Redirect(routes.Application.login).withNewSession.flashing(
      "success" -> "You've been logged out"
    )
  }
  
  def index = IsAuthenticated { username => implicit request =>
    Ok(views.html.index())
  }
  
  def javascriptRoutes = Action { implicit request =>
    import routes.javascript._
    Ok(
      Routes.javascriptRouter("jsRoutes")(
        Tabs.json,
        Tabs.save,
        Modules.fetch,
        Modules.saveJS,
        Modules.json,
        Modules.findRSS,
        Modules.create,
        Modules.delete,
        Feeds.read
      )
    ).as("text/javascript")
  }
}

/**
 * Provide security features
 */
trait Secured {
  
  /**
   * Retrieve the connected user email.
   */
  private def username(request: RequestHeader) = request.session.get("mail")

  /**
   * Redirect to login if the user in not authorized.
   */
  private def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Application.login)
  
  // --
  
  /** 
   * Action for authenticated users.
   */
  def IsAuthenticated(f: => String => Request[AnyContent] => Result) = 
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }

  def IsAuthenticated[A](bp: BodyParser[A])(f: => String => Request[A] => Result) = 
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(bp)(request => f(user)(request))
    }

  def IsOwnerOf(tab: Long)(f: => String => Request[AnyContent] => Result) = IsAuthenticated { user => request =>
    if(Tab.isOwner(tab, user))
      f(user)(request)
    else
      Results.Forbidden
  }
}
