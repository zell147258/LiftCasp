package code.snippet

import net.liftweb.http.{SessionVar, RequestVar, S}
import net.liftweb.util.{StringHelpers, Helpers}
import Helpers._
import net.liftweb.http.SHtml._
import code.model.{Scheduler, Url, Job}
import net.liftweb.http.js.JsCmds.{Alert, SetHtml}
import net.liftweb.common.Empty
import net.liftweb.http.js.{JsCmd, JsCmds}
import bootstrap.liftweb.ActorManager
import scala.xml.NodeSeq
import net.liftweb.http.js.jquery.JqJsCmds
import net.liftweb.http.js.jquery.JqJE.{JqRemove, JqId}
import code.utils.Remove
import org.joda.time.{Interval, DateTime}
import org.joda.time.format.{DateTimeFormat, ISODateTimeFormat, DateTimeFormatter}
import code.akka.StopJobById


/**
 * Created by ivan on 26/12/13.
 */

object CurrentJob extends SessionVar[Option[(Job)]](Empty)

class JobManager {

  var name: String = ""
  var urlsList: List[String] = List.empty[String]


  def reRender = {
    S.redirectTo("/jobs")
  }

  def deleteJob(j: Job) {
    j.delete_!
    reRender
  }

  def jobDetails(j: Job) = {
    CurrentJob.set(Some(j))
    SetHtml("content-body", S.runTemplate("jobdetail" :: Nil).get)
  }


  def getTable = {
    Job.findAll().map {
      j => <tr id={"job_id_" + j.id}>
        <td>
          <span>
            {j.name}
          </span>
        </td>
        <td>
          {a(() => jobDetails(j), <button class="btn btn-default">Detail</button>)}
        </td>

        <td>
          {a(() => deleteJob(j), <button class="btn btn-default">Delete</button>)}
        </td>
      </tr>
    }
  }

  def addJob() = {
    if ((!name.isEmpty) && (Job.findAll().toList.filter(j => j.name == name).isEmpty)) {
      if (!urlsList.isEmpty) {
        val job = Job.create.name(name).date(now).saveMe()
        urlsList.map(Url.create.pid(job).url(_).save())
        reRender
      }
      else
        SetHtml("urls_error", <span>Urls must be defined</span>)
    }
    else
      SetHtml("name_error", <span>Name must be defined or duplicate</span>)

  }

  def render = {
    "#jobs-list-table" #> getTable &
      "#NewJobForm *" #> {
        <table>
          <tr>
            <td>
              <label>Name</label>
            </td>
            <td>
              {ajaxText("", nameForm => {
              name = nameForm.trim
            })}
            </td>
            <td id="name_error"></td>
          </tr>
          <tr>
            <td>
              <label>Urls</label>
            </td>
            <td>
              {ajaxTextarea("", urlslist => {
              urlsList = urlslist.trim.split("\n").toList
            })}
            </td>
            <td id="urls_error">
            </td>
          </tr>
          <tr>
            <td>
              {a(() => addJob(), <button class="btn btn-default">Add</button>)}
            </td>
          </tr>
        </table>

      }
  }

}

case class Line(guid: String, time: DateTime, hit: Int, validTime: Boolean, validHit: Boolean)


class JobDetail {

  var times: List[Line] = List(newLine)
  var startDate: DateTime = null
  var startDateValid = false
  var endDate: DateTime = null
  var endDateValid = false
  val dateParser = DateTimeFormat.forPattern("dd-MM-yyyy")
  val timeParser = DateTimeFormat.forPattern("HH:mm")


  var new_url: String = ""


  private def mutateLine(guid: String)(f: Line => Line) {

    val head = times.filter(_.guid == guid).map(f)
    val rest = times.filter(_.guid != guid)
    times = (head ::: rest)
  }

  def deleteLine(theLine: Line) = {
    times = times.filterNot(_ == theLine).toList
    Remove(theLine.guid)
  }

  private def renderLine(theLine: Line): NodeSeq = {

    <tr id={theLine.guid}>
      <td>time:</td>
      <td>
        {ajaxText("",
        s => {
          var error: JsCmd = JsCmds.Noop
          mutateLine(theLine.guid) {
            l =>
              try {
                val tim = DateTime.parse(s, timeParser)
                error = SetHtml("error_" + l.guid, <span>"Ok"</span>)

                Line(l.guid, tim, l.hit, true, l.validHit)
              }
              catch {
                case _: Throwable => {
                  error = SetHtml("error_" + l.guid, <span>"Invalid time format"</span>)
                  Line(l.guid, l.time, l.hit, false, l.validHit)
                }
              }
          }
          error
        })}
      </td>
      <td id={"error_" + theLine.guid}></td>
      <td>hit:</td>
      <td>
        {ajaxText(theLine.hit.toString,
        s => {
          var error: JsCmd = JsCmds.Noop
          mutateLine(theLine.guid) {
            l =>
              val hit = asInt(s)
              if (hit.isDefined) {
                error = SetHtml("error_hit_" + l.guid, <span>"Ok"</span>)
                Line(l.guid, l.time, hit.get, l.validTime, true)
              }
              else {
                error = SetHtml("error_hit_" + l.guid, <span>"Invalid hit format"</span>)
                Line(l.guid, l.time, l.hit, l.validTime, false)
              }


          }
          error
        })}
      </td>

      <td id={"error_hit_" + theLine.guid}></td>
      <td>
        {a(() => deleteLine(theLine), <button>Delete</button>)}
      </td>
    </tr>

  }

  def addLine() = JqJsCmds.AppendHtml("schedulerTable", renderLine(appendLine))


  private def appendLine: Line = {
    val ret = newLine
    times = (ret :: times)
    ret
  }

  private def newLine = Line("time_line_" + StringHelpers.randomString(20), null, 0, false, true)

  def reRender = {
    SetHtml("content-body", S.runTemplate("jobdetail" :: Nil).get)
  }

  def deleteUrl(u: Url) {
    u.delete_!
    reRender
  }

  def getTable = {
    if (CurrentJob.get.isDefined) {
      CurrentJob.get.get.urls.map {
        u => <tr id={"job_urls_id_" + u.id}>
          <td>
            <span>
              {u.url}
            </span>
          </td>

          <td>
            {a(() => deleteUrl(u), <button class="btn btn-default">Delete</button>)}
          </td>
        </tr>
      }
    }
    else
      <span>No job Selected</span>
  }

  def addUrl = {
    Url.create.url(new_url).pid(CurrentJob.get.get).save
    reRender
  }

  def startJob = {
    ActorManager.router ! CurrentJob.get.get
  }

  def stopJob = {
    ActorManager.router ! StopJobById(CurrentJob.get.get)
  }

  def renderHead: NodeSeq = {
    <h2>
      {CurrentJob.get.get.name}
    </h2>
  }

  def renderFunction: NodeSeq = {
    <table>
      <tr>
        <td>
          {a(() => startJob, <button class="btn btn-default">Start</button>)}
        </td>
        <td>
          {a(() => stopJob, <button class="btn btn-default">Stop</button>)}
        </td>
        <td>
          {a(() => reRender, <button class="btn btn-default">Refresh</button>)}
        </td>
      </tr>
    </table>
  }

  def saveScheduler() = {
    if (startDateValid && endDateValid && times.filter(t => t.validHit == false || t.validTime == false).size == 0) {
      if (startDate.isBefore(endDate) || startDate.isEqual(endDate)) {
        val now = DateTime.now()
        var interval = new Interval(startDate, endDate)
        println(interval)
        val day = interval.toPeriod.getDays
        var days: List[DateTime] = List(startDate)

        Range(1, day + 1).foreach {
          d =>
            val new_date = startDate.plusDays(d)
            days = new_date :: days
        }
        times.map {
          t =>
            days.map {
              d =>
                Scheduler.create.time(d.plusHours(t.time.getHourOfDay).plusMinutes(t.time.getMinuteOfHour).toDate).hit(t.hit).job(CurrentJob.get.get).save()
                Scheduler.reload
            }
        }
        reRender
      }
      else
        Alert("the Start date must be less than or equal to the End date")
    }
    else
      Alert("Not Valid")


  }

  def renderSchedulerForm = {
    <h3>Scheduler Form</h3>
      <table id="schedulerTable">
        <tr>
          <td>Date: (date format DD-MM-YYYY)</td>
        </tr>
        <tr>
          <td>from:</td>
          <td>
            {ajaxText("", start => {

            try {
              startDate = DateTime.parse(start, dateParser)
              startDateValid = true
              SetHtml("error_start_date", <span>"Ok"</span>)
            }
            catch {
              case _: Throwable => {
                startDateValid = false
                SetHtml("error_start_date", <span>"Invalid format"</span>)
              }
            }
          }
          )}
          </td>
          <td id="error_start_date"></td>
        </tr>
        <tr>
          <td>to:</td>
          <td>
            {ajaxText("", end => {
            try {
              endDate = DateTime.parse(end, dateParser)
              endDateValid = true
              SetHtml("error_end_date", <span>"Ok"</span>)
            }
            catch {
              case _: Throwable => {
                endDateValid = false
                SetHtml("error_end_date", <span>"Invalid format"</span>)
              }
            }
          }
          )}
          </td>
          <td id="error_end_date"></td>

        </tr>
        <tr>
          <td>Times: (format hh:mm)</td>
          <td>
            {a(() => addLine(), <button>New Line</button>)}
          </td>
        </tr>{times.flatMap(renderLine)}

      </table>
      <span>
        {a(() => saveScheduler(), <button>Save</button>)}
      </span>


  }

  def renderUrls: NodeSeq = {
    <h3>Url Form</h3>
      <table>
        <tr>
          <td>
            <label>New Url</label>
          </td>
          <td>
            {ajaxText("", url => {
            new_url = url.trim
          }
          )}
          </td>
          <td id="name_error"></td>
        </tr>

        <tr>
          <td>
            {a(() => addUrl, <button class="btn btn-default">Add</button>)}
          </td>
        </tr>
      </table>
  }

  def deleteAllScheduler() = {
    CurrentJob.get.get.scheduler.delete_!
    reRender
  }

  def deleteScheduler(s: Scheduler) = {
    s.delete_!
    reRender
  }

  def renderScheduler: NodeSeq = {
    <h3>Scheduler</h3>
      <span>
        {a(() => deleteAllScheduler(), <button class="btn btn-default">Delete All</button>)}
      </span>
      <table>
        <thead>
          <tr>
            <th>
              Time
            </th>
            <th>
              hits
            </th>
          </tr>
        </thead>
        <tbody>
          {CurrentJob.get.get.scheduler.toList.flatMap {
          f =>
            <tr>
              <td>
                {f.time}
              </td>
              <td>
                {f.hit}
              </td>
              <td>
                {a(() => deleteScheduler(f), <button class="btn btn-default">Delete</button>)}
              </td>
            </tr>
        }}
        </tbody>
      </table>
  }


  def render = {
    "#jobdetail-urls-list-table" #> getTable &
      "#JobDetailManager *" #> {
        renderHead ++ renderFunction ++ renderSchedulerForm ++ renderScheduler ++ renderUrls
      }
  }

}