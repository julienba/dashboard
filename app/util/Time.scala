package jba.util

import java.util.Date

import org.ocpsoft.pretty.time.PrettyTime

object Time {
  private val pretty = new PrettyTime()
  
  def prettify(date: Date): String = pretty.format(date)
}