package code.comet

import net.liftweb.http.CometActor
import net.liftweb.util.Schedule
import scala.xml.Text
import net.liftweb.util.Helpers._
import net.liftweb.http.js.JsCmds.SetHtml
import code.snippet.CurrentJob
import net.liftweb.http.SHtml._
import scala.Some
import code.model.Job

/**
 * Created by ivan on 06/01/14.
 */

case object Refresh

class JobInfo extends CometActor {
  // schedule a ping every 10 seconds so we redraw
  Schedule.schedule(this, Refresh, 2 seconds)

  def render = "#logTable *" #> {
    var job = Job.findByKey(CurrentJob.get.get.id.get).get
    <table>
    <tr>
    <td>Global total hits:</td>
      <td>{job.globalTotal}</td>
    </tr>
      <tr>
        <td>Current total hits:</td>
        <td>{job.total}</td>
      </tr>
      <tr>
        <td>Global total pages:</td>
        <td>{job.totalPages}</td>
      </tr>
      <tr>
        <td>Current total pages:</td>
        <td>{job.currentPages}</td>
      </tr>
      <tr>
        <td>Active process n.:</td>
        <td>{job.activeProcess}</td>
      </tr>
    </table>
      <table>
        <thead>
          <tr>
            <th>
              Time
            </th>
            <th>
              Message
            </th>
          </tr>
        </thead>
        <tbody>
          {job.log.map {
          l => <tr>
            <td>
              {l.timeStamp}
            </td>
            <td>
              {l.message}
            </td>
          </tr>
        }}
        </tbody>
      </table>
  }



  override def lowPriority = {
    case Refresh =>
      reRender(true)
      Schedule.schedule(this, Refresh, 10 seconds)
  }
}


//  initCometActor(initSession, initType, initName, initDefaultXml, initAttributes)}


case object Tick
