package code.model

import net.liftweb.mapper._
/**
  * Created by ivan on 14/01/14.
  */
class JobLog extends LongKeyedMapper[JobLog] with IdPK {
  def getSingleton = JobLog

  object timeStamp extends MappedDateTime(this)

  object job extends MappedLongForeignKey(this, Job)

  object message extends MappedString(this,100000)

}

object JobLog extends JobLog with LongKeyedMetaMapper[JobLog] {
  override def dbTableName = "job_log"
}