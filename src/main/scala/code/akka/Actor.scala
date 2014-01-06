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


/**
 * Created by ivan on 26/12/13.
 */

case class CustomException(message: String, spiderConfigName: String) extends Exception

case class CasperParam(userAgent: UserAgent, urls: List[Url], proxy: Option[code.model.Proxy])

case class CasperReturn(succes: Int, viewedPages: Int, procesTime: Period)

case class StartCasper()

case class StopJobById(job: Job)

case class RunJob()

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

  var actorNumber = 20
  var total: Int = 0
  var currentTotalPages: Int = 0
  val runners = context.actorOf(Props[RunCasper].withRouter(RoundRobinRouter(nrOfInstances = actorNumber)).withDispatcher("my-dispatcher"), name = "runners")
  var prev: Date = null
  var next: Date = null
  var hitsTarget = 0
  var totalTime: Double = -1
  var avgTime: Double = -1
  var proxy: List[code.model.Proxy] = getProxy
  var userAgent: List[code.model.UserAgent] = getUserAgent
  var urls = job.urls.toList
  var children: List[ActorRef] = List.empty[ActorRef]
  var activeActor = 0

  val df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

  var stop = false

  ActorManager.system.scheduler.scheduleOnce(0 seconds, self, RunJob())

  def newLog(message: String) = {
    JobLog.create.job(job).message(message).timeStamp(new Date).save()
  }

  //  ActorManager.system.scheduler.schedule(1 seconds, 5 seconds, runners, cp)

  def schedulerManager {
    val now = new Date();
    var changed = false
    var newHit = 0
    var after = false
    var scheduler = job.scheduler.toList
    if (scheduler.count(s => s.time.get.after(new Date)) > 0)
      scheduler = scheduler.filter(s => s.time.get.after(new Date))
    else
      scheduler = List(scheduler.last)
    scheduler.foreach {
      s =>
        if (!after) {
          val time = new Date(s.time.get.getTime)
          if (time.before(now)) {
            if (prev == null || (!prev.equals(time) && prev.before(time))) {
              prev = time
              newHit = s.hit.get
              changed = true
            }
          }
          else {
            after = true
            next = time

          }
          if (changed) {
            hitsTarget = newHit
            total = 0
            currentTotalPages = 0
            if (next == null)
              newLog("Current time: " + df.format(prev) + ". New target: " + newHit)
            else
              newLog("Current time: " + df.format(prev) + ", next time: " + df.format(prev) + ". New target: " + newHit)
          }
        }
    }


  }

  def getProxy: List[code.model.Proxy] = code.model.Proxy.findAll().toList

  def getUserAgent: List[code.model.UserAgent] = code.model.UserAgent.findAll().toList


  def receive: Actor.Receive = {

    case r: RunJob =>
      if (!stop) {
        schedulerManager

        if (total < hitsTarget) {
          var delay: Double = 0
          if (next != null && total > 0) {
            val toGo = new Period(DateTime.now(), new DateTime(next)).toStandardSeconds.getSeconds
            val unit = toGo / (hitsTarget - (total / actorNumber)).toDouble
            val del = unit - avgTime
            delay = if (del > 0) del else 0
          }
          Range(0, actorNumber - activeActor).par.map {
            i =>
              val userA = userAgent(Random.nextInt(userAgent.length))
              val proxyR = proxy(Random.nextInt(proxy.length))
              var urlsR: List[Url] = List.empty[Url]
              for (i <- 0 to Random.nextInt(urls.size))
                urlsR = urls(Random.nextInt(urls.size)) :: urlsR
              val cp = CasperParam(userA, urlsR, Some(proxyR))
              activeActor += 1
              ActorManager.system.scheduler.scheduleOnce(delay seconds, runners, cp)
          }
          ActorManager.system.scheduler.scheduleOnce(avgTime + 1 seconds, self, RunJob())

        }
        else {
          val sleep = new Period(DateTime.now(), new DateTime(next)).toStandardSeconds.getSeconds
          newLog("Target complete. Wait " + sleep + " seconds")
          ActorManager.system.scheduler.scheduleOnce(sleep seconds, self, RunJob())
        }

      }
      else
        println("Stopped")

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
      stop = true


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
      val temp = java.io.File.createTempFile("casperjs", "script")
      val bw = new BufferedWriter(new FileWriter(temp));
      bw.write(SpiderUtil.generateString(cp));
      bw.close();
      if (cp.proxy.isDefined)
        command = "casperjs --proxy=" + cp.proxy.get.proxy.get + " " + temp.getPath
      else
        command = "casperjs " + temp.getPath
      try {
        val process = ProcessUtils(command, 60).run()

        val subProcessInputReader = new BufferedReader(new InputStreamReader(process.getInputStream()))
        var line = subProcessInputReader.readLine()
        while (line != null) {
          result += line + "\n"
          line = subProcessInputReader.readLine()
        }
        process.destroy()


      }
      catch {
        case e: InterruptedException =>
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


