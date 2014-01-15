package code.model

import net.liftweb.mapper._

/**
 * Created by ivan on 14/01/14.
 */
class Scheduler extends LongKeyedMapper[Scheduler] with IdPK with OneToMany[Long, Scheduler] {
  def getSingleton = Scheduler

  object from extends MappedDate(this)

  object to extends MappedDate(this)

  object job extends MappedLongForeignKey(this, Job)

  object times extends MappedOneToMany(SchedulerTime,
    SchedulerTime.scheduler, OrderBy(SchedulerTime.time, Ascending)) with
  Owned[SchedulerTime] with Cascade[SchedulerTime]

}

object Scheduler extends Scheduler with LongKeyedMetaMapper[Scheduler] {
  override def dbTableName = "scheduler"
}