package code.akka

import java.util.Date
import code.model.{JobLog, Scheduler, SchedulerTime, Job}
import org.joda.time.{Period, LocalTime, DateTime}
import java.sql.Time
import java.text.SimpleDateFormat

/**
 * Created by ivan on 15/01/14.
 */

case class SchedulerState(currentTime: Time, nextTime: Option[Time] = None, targetHits: Integer = 0, currentHits: Integer = 0, waitTime: Option[Int] = None) {
  def currentDateTime = new DateTime(currentTime).withYear(1970).withMonthOfYear(1).withDayOfMonth(1)

  def nextDateTime = new DateTime(nextTime.get).withYear(1970).withMonthOfYear(1).withDayOfMonth(1)
}

case class SingleTime(time: Time)

object SchedulerUtils {

  val df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

  def getScheduler(job: Job): Option[Scheduler] = {
    val curretDate = new Date
    var schedulers = job.scheduler.filter {
      s =>
        (s.from.get.before(curretDate) || s.from.get.equals(curretDate)) && (s.to.get.after(curretDate) || s.to.get.equals(curretDate))
    }.toList
    if (!schedulers.isEmpty) {
      return Some(schedulers.head)
    }
    return None
  }

  def getTimes(scheduler: Scheduler): (Option[SchedulerTime], Option[SchedulerTime]) = {
    val now = new Time(new LocalTime(new Date()).toDateTimeToday.withDate(1970, 1, 1).getMillis)
    val sTimes = scheduler.times.toList
    if (sTimes.isEmpty)
      return (None, None)
    else {
      val afterNow: List[SchedulerTime] = sTimes.filter(_.getTime.after(now))
      val times = sTimes.filterNot(t => afterNow.contains(t)).last :: afterNow
      if (afterNow.isEmpty)
        return (Some(times.head), None)
      else
        return (Some(times.head), Some(afterNow.head))
    }
  }

  def waitOneDay(): SchedulerState = {
    val wait = new Period(DateTime.now(), new DateTime().plusDays(1).withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(1)).toStandardSeconds.getSeconds
    SchedulerState(currentTime = new Time(new Date().getTime), waitTime = Some(wait))
  }

  def getNow = new DateTime().withYear(1970).withMonthOfYear(1).withDayOfMonth(1)

  def schedulerManager(job: Job, state: Option[SchedulerState] = None): Option[SchedulerState] = {
    val midnight = new DateTime(1970, 1, 1, 23, 59, 59, 59)



    val scheduler = getScheduler(job)
    if (scheduler.isDefined) {
      val times = getTimes(scheduler.get)
      if (times._1.isDefined) {
        val next = if (times._2.isDefined) times._2.get.getTime else null
        if (!state.isDefined) {
          if (next == null)
            JobLog.create.job(job).message("First Start. Prev Null. Prev set to: " + df.format(times._1.get.getTime) + ". Target hit: " + times._1.get.hit.get).timeStamp(new Date).save()
          else
            JobLog.create.job(job).message("First Start. Prev Null. Prev set to: " + times._1.get.getTime + ".Next is:" + next + ". Target hit: " + times._1.get.hit.get).timeStamp(new Date).save()

          return Some(SchedulerState(currentTime = times._1.get.getTime, nextTime = Some(next), targetHits = times._1.get.hit.get, currentHits = 0))
        }
        else if (state.get.currentTime != times._1.get.getTime) {
          if (next == null)
            JobLog.create.job(job).message("Prev set to: " + df.format(times._1.get.getTime) + ". Target hit: " + times._1.get.hit.get).timeStamp(new Date).save()
          else
            JobLog.create.job(job).message("Prev set to: " + times._1.get.getTime + ".Next is:" + next + ". Target hit: " + times._1.get.hit.get).timeStamp(new Date).save()
          return Some(SchedulerState(currentTime = times._1.get.getTime, nextTime = Some(next), targetHits = times._1.get.hit.get, currentHits = 0))
        }
        else {
          if (state.get.currentHits >= state.get.targetHits) {
            val nextTime = if (times._2.isDefined) new DateTime(times._2.get.getTime) else midnight
            val sleep = new Period(getNow, nextTime).toStandardSeconds.getSeconds
            return Some(SchedulerState(currentTime = new Time(new Date().getTime), waitTime = Some(sleep)))
          }
          else
            return state
        }
      }
      else
        return Some(waitOneDay)
    }
    None
  }
}


//      currentTimes = sTimes.filterNot(t => currentTimes.contains(t)).last :: currentTimes
//      if (currentTimes.size > 0) {
//        val currentSchedulerTime = currentTimes.head
//        val time = new Time(currentSchedulerTime.time.get.getTime)
//        var nextTime: Time = null
//        if (currentTimes.size > 1) nextTime = new Time(currentTimes(1).time.get.getTime)
//        if (state.isDefined && state.get.currentHits >= state.get.targetHits) {
//          var wait = 0
//          if (nextTime != null)
//            wait = new Period(DateTime.now(), new DateTime(nextTime)).toStandardSeconds.getSeconds
//          else
//            wait = new Period(DateTime.now(), new DateTime(midnight)).toStandardSeconds.getSeconds
//
//          return Some(state.get.copy(waitTime = Some(wait)))
//        }
//        if (state.get.currentTime != time || !state.isDefined)
//          return Some(schedulerState(currentTime = time, nextTime = Some(nextTime), targetHits = currentSchedulerTime.hit.get, currentHits = 0))
//        else
//          return state
//      }
//    }
//
//    null

//  }
//
//}


//    scheduler.times.filter(t => t.time.get.getTime<)
//    scheduler.foreach {
//      s =>
//        if (!after) {
//          val time = new Date(s.time.get.getTime)
//          if (time.before(now)) {
//            if (prev == null || (!prev.equals(time) && prev.before(time))) {
//              prev = time
//              newHit = s.hit.get
//              changed = true
//            }
//          }
//          else {
//            after = true
//            next = time
//
//          }
//          if (changed) {
//            hitsTarget = newHit
//            total = 0
//            currentTotalPages = 0
//            if (next == null)
//              newLog("Current time: " + df.format(prev) + ". New target: " + newHit)
//            else
//              newLog("Current time: " + df.format(prev) + ", next time: " + df.format(prev) + ". New target: " + newHit)
//          }
//        }
//    }
