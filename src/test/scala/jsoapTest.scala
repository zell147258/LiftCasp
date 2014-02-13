import code.snippet.HelloWorld
import code.utils.ProcessUtils
import java.io._
import javax.script.ScriptEngineManager
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mozilla.javascript.Context
import org.specs2.mutable.Specification
import scala.collection.JavaConversions._
import sun.org.mozilla.javascript.internal.RhinoException

/**
 * Created by ivan on 03/02/14.
 */
class jsoapTest extends Specification {


  "test" should {
    "jsoap" in {
      val casperScript = java.io.File.createTempFile("casperjs", ".js")
      val bw = new BufferedWriter(new FileWriter(casperScript))
      bw.write( """casper.start('http://spys.ru/free-proxy-list/IT/', function() {
                  |    this.echo(this.getHTML());
                  |});
                  |
                  |casper.run();""".stripMargin)
      bw.close()

      val command = "casperjs test " + casperScript.getPath
      var result = ""
      var i = 0
      try {
        val process = ProcessUtils(command, 40).run()
        val subProcessInputReader = new BufferedReader(new InputStreamReader(process.getInputStream()))
        var line = subProcessInputReader.readLine()
        while (line != null) {
          if (i == 0)
            i += 1
          else
            result += line + "\n"
          line = subProcessInputReader.readLine()
        }
        process.destroy()

      }
      catch {
        case e: InterruptedException => println("timeout ")
      }
      try {
        casperScript.delete()
      }
      val newPage = java.io.File.createTempFile("casperjs_result", ".html")
      val bw2 = new BufferedWriter(new FileWriter(newPage))
      bw2.write(result)
      bw2.close
      val doc = Jsoup.parse(newPage, "UTF-8", "http://spys.ru/free-proxy-list/IT/")

      //      val doc = Jsoup.connect("http://spys.ru/free-proxy-list/IT/").get()
      var proxyList = List.empty[String]

      val tr = doc.select(".spy14")
      tr.foreach {
        el =>
          if (el.childNodes.size() == 4) {
            val proxy = el.childNodes.get(0) + ":" + el.childNodes.get(3)
            if (proxy.toString.matches("\\d+\\.\\d+\\.\\d+\\.\\d+:\\d+")) {
              if (el.parent().parent().childNodes().get(2).childNodes().get(0).childNodes().get(0).toString.toLowerCase == "noa")
                proxyList ::= proxy
            }
          }
      }
      newPage.delete()

      proxyList
      ok

    }


  }
}
