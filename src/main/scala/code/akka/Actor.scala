package code.akka

import akka.actor._
import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.routing.{Broadcast, RoundRobinRouter}
import code.model.{Url, UserAgent, Job, JobLog}
import scala.concurrent.duration._
import sys.process._
import java.io._
import scala.util.Random
import bootstrap.liftweb.ActorManager
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import java.util.Date
import org.joda.time.{Period, DateTime}
import scala.Some
import akka.actor.OneForOneStrategy
import code.utils.ProcessUtils
import java.text.SimpleDateFormat
import org.jsoup.Jsoup
import scala.collection.JavaConversions._


/**
 * Created by ivan on 26/12/13.
 */

case class CustomException(message: String, spiderConfigName: String) extends Exception

case class CasperParam(userAgent: UserAgent, urls: List[Url], proxy: Option[code.model.Proxy])

case class CasperReturn(succes: Int, viewedPages: Int, procesTime: Period)

case class StartCasper()

case class StopJobById(job: Job)

case class RunJob()

case class RetriveProxyAction()

object SpiderUtil {

  def nameEncoder(url: String): String = java.net.URLEncoder.encode(url, "UTF-8")

  def nameDecoder(filename: String): String = java.net.URLDecoder.decode(filename, "UTF-8")

  def generateString(cp: CasperParam) = {
    var s: String = "var casper = require('casper').create();\n\n"
    s += "casper.userAgent('" + cp.userAgent.userAgent.get.trim + "');\n"
    s += "casper.start('" + cp.urls.head.url.get.trim + "', function() {\n\tthis.echo(\"OK\");\n});;\n\n"
    cp.urls.tail.map {
      u =>
        s += "casper.wait(" + (200 + Random.nextInt(1500)).toString + ", function() {});\n"
        s += "casper.thenOpen('" + u.url.get.trim + "', function() {this.echo(\"OK\");});\n\n"
    }
    s += "casper.run();"
    s
  }
}


class CasperSpider() extends Actor with akka.actor.ActorLogging {


  context.setReceiveTimeout(1 minute)
  var managers: Map[String, ActorRef] = Map.empty[String, ActorRef]
  lazy val proxy = context.actorOf(Props(classOf[RetriveProxy]), name = "proxy")
  proxy.tell(RetriveProxyAction(),self)




  def receive = {
    case job: Job =>
      JobLog.create.job(job).message("Job started").timeStamp(new Date).save()
      log.info(" Start Spidering [" + job.name + "]")
      val name = "spider_" + SpiderUtil.nameEncoder(job.id.toString)
      val newActor: Map[String, ActorRef] = Map(name -> context.actorOf(Props(classOf[JobManager], job), name = name))
      managers = managers ++ newActor
      managers

    case st: StopJobById =>
      JobLog.create.job(st.job).message("Job Stopped").timeStamp(new Date).save()
      managers.get("spider_" + SpiderUtil.nameEncoder(st.job.id.toString)).map(_ ! PoisonPill)

    case r: RetriveProxyAction =>
      println("retrive proxy in casper spider")
      managers.values.map(_ ! RetriveProxyAction)


    case _ =>
  }

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: ActorKilledException ⇒ Restart
      case _: NullPointerException ⇒ Restart
      case _: IllegalArgumentException ⇒ Restart
      case ce: CustomException ⇒
        println(ce.toString)
        Restart
    }
}


class JobManager(job: Job) extends Actor with akka.actor.ActorLogging {
  println(job.name)

  var globalTotal = 0
  var globalTotalPages = 0

  var actorNumber = 25
  var total: Int = 0
  var currentTotalPages: Int = 0
  val runners = context.actorOf(Props[RunCasper].withRouter(RoundRobinRouter(nrOfInstances = actorNumber)).withDispatcher("my-dispatcher"), name = "runners")
  var hitsTarget = 0
  var totalTime: Double = -1
  var avgTime: Double = -1
  var proxy: List[code.model.Proxy] = getProxy
  var userAgent: List[code.model.UserAgent] = getUserAgent
  var urls = job.urls.toList
  var children: List[ActorRef] = List.empty[ActorRef]
  var activeActor = 0
  var state: Option[SchedulerState] = None

  val df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");


  ActorManager.system.scheduler.scheduleOnce(0 seconds, self, RunJob())

  def newLog(message: String) = {
    JobLog.create.job(job).message(message).timeStamp(new Date).save()
  }

  //  ActorManager.system.scheduler.schedule(1 seconds, 5 seconds, runners, cp)


  def getProxy: List[code.model.Proxy] = code.model.Proxy.findAll().toList

  def getUserAgent: List[code.model.UserAgent] = code.model.UserAgent.findAll().toList


  def receive: Actor.Receive = {

    case r: RetriveProxyAction => proxy = getProxy

    case r: RunJob =>
      if (state.isDefined)
        state = Some(state.get.copy(currentHits = total))

      SchedulerUtils.schedulerManager(job, state) match {
        case Some(state) => {
          if (!this.state.isDefined)
            this.state = Some(state)
          if (state.waitTime.isDefined) {
            newLog("Target complete. Wait " + state.waitTime.get + " seconds")
            ActorManager.system.scheduler.scheduleOnce(state.waitTime.get seconds, self, RunJob())
          }
          else {
            var delay: Double = 0
            if (total > 0) {
              var del: Double = 0
              val toGo = new Period(SchedulerUtils.getNow, state.nextDateTime).toStandardSeconds.getSeconds
              if ((state.targetHits - (state.currentHits / actorNumber)) > 0) {
                val unit = toGo / (state.targetHits - (state.currentHits / actorNumber)).toDouble
                del = unit - avgTime
              }

              delay = if (del > 0) del else 5
            }

            Range(0, actorNumber - activeActor).par.map {
              i =>
              //                println("inizializzo i range")
                val userA = userAgent(Random.nextInt(userAgent.length))
                val proxyR = proxy(Random.nextInt(proxy.length))
                var urlsR: List[Url] = List.empty[Url]
                var urlsNum = Random.nextInt(10)
                urlsNum = if (urlsNum > 0) urlsNum else 1
                //                println("urlsnum "+ urlsNum +" delay: "+ delay)
                for (i <- 0 to urlsNum)
                  urlsR = urls(Random.nextInt((urls.size))) :: urlsR
                val cp = CasperParam(userA, urlsR, Some(proxyR))
                activeActor += 1
                ActorManager.system.scheduler.scheduleOnce(delay seconds, runners, cp)
            }
            //            println("schedulo il prossimo ")
            ActorManager.system.scheduler.scheduleOnce(avgTime + 1 seconds, self, RunJob())
          }

        }
        case None => newLog("Job Complete")
      }


    case cr: CasperReturn =>
      total += cr.succes

      globalTotal += cr.succes
      globalTotalPages += cr.viewedPages
      currentTotalPages += cr.viewedPages
      println("Total: " + total)
      println("Pages: " + currentTotalPages)
      totalTime += cr.procesTime.toStandardSeconds.getSeconds
      activeActor -= 1
      job.total(total).activeProcess(activeActor).globalTotal(globalTotal).totalPages(globalTotalPages).currentPages(currentTotalPages) save

    case sc: StopJobById =>


      println("KILL")
      context.self ! Kill


    case _ =>
  }
}


class RunCasper extends Actor with akka.actor.ActorLogging {

  def receive: Actor.Receive = {

    case sc: StopJobById =>
      println("start runcasper kill")
      context.self ! Kill
      println("runcasper kill")
    case cp: CasperParam =>
      val startDate = DateTime.now()
      var command = ""
      var result = ""
      val temp = java.io.File.createTempFile("casperjs", ".js")
      val bw = new BufferedWriter(new FileWriter(temp))
      bw.write(SpiderUtil.generateString(cp))
      bw.close()
      if (cp.proxy.isDefined)
        command = "casperjs --proxy=" + cp.proxy.get.proxy.get + " " + temp.getPath
      else
        command = "casperjs " + temp.getPath
      try {
        val process = ProcessUtils(command, 40).run()

        val subProcessInputReader = new BufferedReader(new InputStreamReader(process.getInputStream()))
        var line = subProcessInputReader.readLine()
        while (line != null) {
          result += line + "\n"
          line = subProcessInputReader.readLine()
        }
        process.destroy()
        //        println("ucciso il processo ")


      }
      catch {
        case e: InterruptedException => println("timeout ")
      }
      try {
        temp.delete()
      }

      val size = result.split("\n").filter(_ == "OK").size
      val endDate = DateTime.now()
      sender ! CasperReturn(if (size > 0) 1 else 0, result.split("\n").filter(_ == "OK").size, new Period(startDate, endDate))


    case _ =>
  }

}


class RetriveProxy extends Actor with akka.actor.ActorLogging {


  def receive: Actor.Receive = {
    case r: RetriveProxyAction => {
      println("retrive proxy")
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

      if (!proxyList.isEmpty) {
        val all = code.model.Proxy.findAll()
        proxyList.foreach(p => code.model.Proxy.create.proxy(p).save())
        all.foreach(_.delete_!)
      }
      println("retrive proxy finish ")
      ActorManager.system.scheduler.scheduleOnce(1 day, self, RetriveProxyAction)
      println("reset retrive proxy")
      context.parent.tell(RetriveProxyAction, self)
      println("send message to parent")

    }
    case _ =>
  }
}


