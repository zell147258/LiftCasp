package code.snippet

/**
 * Created by ivan on 24/12/13.
 */

import code.lib._
import net.liftweb.util.Helpers
import Helpers._
import code.model.{Job, Proxy}
import net.liftweb.http.SHtml._
import net.liftweb.http.{ReRender, S}

import net.liftweb.http.js.JsCmds.SetHtml
import net.liftweb.http.js.JsCmds.SetHtml
import bootstrap.liftweb.ActorManager


class ShowProxy {
  var proxy: String = ""
  var descriptionproxy: String = ""
  var proxyList: List[String] = List.empty[String]

  def addProxy() {
    proxyList.foreach(println(_))
    proxyList.map(Proxy.create.proxy(_).save())
    reRender
  }

  def deleteProxy(p: Proxy) {
    p.delete_!
    reRender
  }

  def saveProxy(p: Proxy, newProxy: String, descr: String) {
    p.proxy(newProxy).description(descr).save()
    reRender
  }

  def editProxy(p: Proxy) = {
    descriptionproxy = p.description
    proxy = p.proxy

    SetHtml("proxy_id_" + p.id, <td>
      {ajaxText(p.proxy.get, proxytext => proxy = proxytext)}
    </td>
      <td>
        <span>
          {ajaxText(p.description.get, descriptiontext => descriptionproxy = descriptiontext)}
        </span>
      </td>
      <td>
        {a(() => saveProxy(p, proxy, descriptionproxy), <button class="btn btn-default">Save</button>)}
      </td>
      <td>
        {a(() => deleteProxy(p), <button class="btn btn-default">Delete</button>)}
      </td>
    )
  }

  def getTable = {
    Proxy.findAll().map {
      p => <tr id={"proxy_id_" + p.id}>
        <td>
          {p.proxy}
        </td>
        <td>
          {p.description}
        </td>
        <td>
          {a(() => editProxy(p), <button class="btn btn-default">Edit</button>)}
        </td>
        <td>
          {a(() => deleteProxy(p), <button class="btn btn-default">Delete</button>)}
        </td>
      </tr>
    }
  }

  def reRender = {
    S.redirectTo("/proxy")
  }

  def render = {
    "#proxy-list-table" #> getTable &
      "#ProxyManager *" #> {
        <span>
          {ajaxTextarea("", proxylist => {
          proxyList = proxylist.trim.split("\n").toList
        })}
        </span>
          <span>
            {a(() => addProxy(), <button class="btn btn-default">Add</button>)}
          </span>
      }
  }
}
