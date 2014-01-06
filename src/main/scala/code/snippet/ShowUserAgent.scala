package code.snippet

/**
 * Created by ivan on 24/12/13.
 */

import net.liftweb.util.Helpers
import Helpers._
import code.model.UserAgent
import net.liftweb.http.SHtml._
import net.liftweb.http.S

import net.liftweb.http.js.JsCmds.SetHtml
import bootstrap.liftweb.ActorManager


class ShowUserAgent {
  var useragent: String = ""
  var useragentList: List[String] = List.empty[String]

  def addUserAgent() {
    useragentList.map(UserAgent.create.userAgent(_).save())
    reRender
  }

  def deleteUserAgent(u: UserAgent) {
    u.delete_!
    reRender
  }

  def saveUserAgent(u: UserAgent, newUserAgent: String) {
    u.userAgent(newUserAgent).save()
    reRender
  }

  def editUserAgent(u: UserAgent) = {
    SetHtml("useragent_id_" + u.id, <td>
      <span>
        {ajaxText(u.userAgent.get, useragenttext => useragent = useragenttext)}
      </span>
    </td>
      <td>
        {a(() => saveUserAgent(u, useragent), <button class="btn btn-default">Save</button>)}
      </td>
      <td>
        {a(() => deleteUserAgent(u), <button class="btn btn-default">Delete</button>)}
      </td>
    )
  }

  def getTable = {
    UserAgent.findAll().map {
      u => <tr id={"useragent_id_" + u.id}>
        <td>
          <span>
            {u.userAgent}
          </span>
        </td>
        <td>
          {a(() => editUserAgent(u), <button class="btn btn-default">Edit</button>)}
        </td>
        <td>
          {a(() => deleteUserAgent(u), <button class="btn btn-default">Delete</button>)}
        </td>
      </tr>
    }
  }

  def reRender = {
    S.redirectTo("/useragent")
  }

  def render = {
    "#useragent-list-table" #> getTable &
      "#UserAgentManager *" #> {
        <span>
          {ajaxTextarea("", proxylist => {
          useragentList = proxylist.trim.split("\n").toList
        })}
        </span>
          <span>
            {a(() => addUserAgent(), <button class="btn btn-default">Add</button>)}
          </span>
      }
  }
}
