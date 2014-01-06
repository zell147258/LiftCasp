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
    Scheduler.job, OrderBy(Scheduler.time, Ascending)) with
  Owned[Scheduler] with Cascade[Scheduler]

  object log extends MappedOneToMany(JobLog,
    JobLog.job, OrderBy(JobLog.timeStamp, Descending)) with
  Owned[JobLog] with Cascade[JobLog]

}

object Job extends Job with LongKeyedMetaMapper[Job] {
  override def dbTableName = "job"
}

class Url extends LongKeyedMapper[Url] with IdPK {
  def getSingleton = Url

  object url extends MappedString(this, 10000)

  object pid extends MappedLongForeignKey(this, Job)

}

object Url extends Url with LongKeyedMetaMapper[Url] {
  override def dbTableName = "url"
}

class Proxy extends LongKeyedMapper[Proxy] with IdPK {
  def getSingleton = Proxy

  object proxy extends MappedString(this, 100)
  object description extends MappedString(this, 10000)

}

object Proxy extends Proxy with LongKeyedMetaMapper[Proxy] {
  override def dbTableName = "proxy"
}

class UserAgent extends LongKeyedMapper[UserAgent] with IdPK {
  def getSingleton = UserAgent

  object userAgent extends MappedString(this, 10000)

}

object UserAgent extends UserAgent with LongKeyedMetaMapper[UserAgent] {
  override def dbTableName = "user_agent"
}

class Scheduler extends LongKeyedMapper[Scheduler] with IdPK {
  def getSingleton = Scheduler

  object time extends MappedDateTime(this)

  object job extends MappedLongForeignKey(this, Job)

  object hit extends MappedInt(this)

}

object Scheduler extends Scheduler with LongKeyedMetaMapper[Scheduler] {
  override def dbTableName = "scheduler"
}

class JobLog extends LongKeyedMapper[JobLog] with IdPK {
  def getSingleton = JobLog

  object timeStamp extends MappedDateTime(this)

  object job extends MappedLongForeignKey(this, Job)

  object message extends MappedString(this,100000)

}

object JobLog extends JobLog with LongKeyedMetaMapper[JobLog] {
  override def dbTableName = "job_log"
}