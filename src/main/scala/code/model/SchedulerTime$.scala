package code.model

import net.liftweb.mapper._
import java.sql.Time

/**
 * Created by ivan on 14/01/14.
 */
class SchedulerTime extends LongKeyedMapper[SchedulerTime] with IdPK {
  def getSingleton = SchedulerTime

  object time extends MappedTime(this)


  object scheduler extends MappedLongForeignKey(this, Scheduler)

  object hit extends MappedInt(this)

  def getTime = new Time(time.get.getTime)

}

object SchedulerTime extends SchedulerTime with LongKeyedMetaMapper[SchedulerTime] {
  override def dbTableName = "scheduler_time"
}


