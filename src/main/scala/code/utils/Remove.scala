package code.utils

import net.liftweb.http.js.JsCmd

/**
 * Created by ivan on 27/12/13.
 */
object Remove {
  def apply(uid: String): JsCmd = new Remove(uid)
}

class Remove(uid: String) extends JsCmd {
  def toJsCmd = {
    "try{jQuery(\"" + ("#" + uid) + "\").remove();}catch (e) {}"
  }
}