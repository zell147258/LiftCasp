package code.snippet

import net.liftweb.http.{LiftRules, S}
import net.liftweb.sitemap.MenuItem
import net.liftweb.util.Helpers.strToCssBindPromoter
import scala.xml.NodeSeq

/*
* Use for render sidebar menu
* */
class MainMenu {

  val menuEntries =
    (for {sm <- LiftRules.siteMap;
          req <- S.request
    } yield sm.buildMenu(req.location).lines) openOr Nil


  def createMenu(item: MenuItem) : NodeSeq= {
    var styles = item.cssClass openOr ""
    var activeItem: Boolean = false
    var arrowIcon = "arrow"

//    if (item.equals(menuEntries.head)) styles += " start"
//
    if (item.current) styles += " active"

    if (item.kids.filter(k => k.current).length > 0 || item.kids.filter( k => k.kids.filter(x => x.current).length>0).length > 0 ) {
      activeItem = true
      styles += " active"
      arrowIcon += " open"
    }

    item.kids match {
      case Nil =>
        <li class={styles}>
          <a href={item.uri}>
            <span class="title">
              {item.text}
            </span>{if (item.current) <span class="selected"></span>}
          </a>
        </li>

      case kids =>
        <li class={styles}>
          <a href="javascript:;">
            <span class="title">
              {item.text}
            </span>{if (activeItem) <span class="selected"></span>}<span class={arrowIcon}></span>
          </a>
          <ul class="sub-menu">
            {for (kid <- kids) yield {
            var stylesKid = item.cssClass openOr ""
            if (kid.current) stylesKid += "active"
            createMenu(kid)
          }}
          </ul>
        </li>
    }
  }


  def builder = "*" #> menuEntries.map(item => createMenu(item))

}
