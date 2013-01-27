package form

import play.api.data._
import play.api.data.Forms._

object CommonForm {
  val reorderForm = Form(
      single("ids" -> list(number))
    )
}