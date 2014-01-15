package code.model

import net.liftweb.mapper._
/**
 * Created by ivan on 14/01/14.
 */
class UserAgent extends LongKeyedMapper[UserAgent] with IdPK {
  def getSingleton = UserAgent

  object userAgent extends MappedString(this, 10000)

}

object UserAgent extends UserAgent with LongKeyedMetaMapper[UserAgent] {
  override def dbTableName = "user_agent"
}