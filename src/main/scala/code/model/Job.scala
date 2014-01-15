package code.model

import net.liftweb.mapper._


class Job extends LongKeyedMapper[Job] with IdPK with OneToMany[Long, Job] {
  def getSingleton = Job

  object name extends MappedString(this, 100)

  object pid extends MappedInt(this)

  object date extends MappedDateTime(this)

  object tread extends MappedInt(this)

  object useProxy extends MappedBoolean(this)

  object total extends MappedInt(this)

  object globalTotal extends MappedInt(this)

  object activeProcess extends MappedInt(this)

  object currentPages extends MappedInt(this)

  object totalPages extends MappedInt(this)




  object urls extends MappedOneToMany(Url,
    Url.pid, OrderBy(Url.id, Ascending)) with
  Owned[Url] with Cascade[Url]

  object scheduler extends MappedOneToMany(Scheduler,
    Scheduler.job, OrderBy(Scheduler.from, Ascending)) with
  Owned[Scheduler] with Cascade[Scheduler]

  object log extends MappedOneToMany(JobLog,
    JobLog.job, OrderBy(JobLog.timeStamp, Descending)) with
  Owned[JobLog] with Cascade[JobLog]

}

object Job extends Job with LongKeyedMetaMapper[Job] {
  override def dbTableName = "job"
}









