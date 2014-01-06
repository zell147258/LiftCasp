package bootstrap.liftweb

import net.liftweb.sitemap.{SiteMap, Menu}
import net.liftweb.http.S
import net.liftweb.sitemap.Loc.Hidden

/**
 * Created by ivan on 24/12/13.
 */

case class MenuLoc(menu: Menu) {
  lazy val url: String = S.contextPath + menu.loc.calcDefaultHref
  lazy val fullUrl: String = S.hostAndPath + menu.loc.calcDefaultHref
}

object Site {
  val home = MenuLoc(Menu.i("Home") / "index")
  val proxy = MenuLoc(Menu.i("Proxy") / "proxy")
  val userAgent = MenuLoc(Menu.i("UserAgent") / "useragent")
  val jobs = MenuLoc(Menu.i("Jobs") / "jobs")

  def webMenus(directoryName: String): List[Menu] = {
    //    todo add >> RequireLoggedIn
    import java.io.File

    (new File(directoryName)).listFiles.filter(_.isFile).filter(_.getName endsWith "html").map(f =>
      MenuLoc(Menu.i("Web | " + f.getName.split('.')(0)) / "web" / "test" / f.getName.split('.')(0) >> Hidden).menu
    ).toList
  }

  val webTestDir = System.getProperty("user.dir") + "/src/main/webapp/web/test"

  private def menus = List(
    home.menu,
    proxy.menu,
    userAgent.menu,
    jobs.menu) ::: webMenus(webTestDir)

  def siteMap: SiteMap = SiteMap(menus: _*)
}
