package code.model

import net.liftweb.mapper._

/**
 * Created by ivan on 14/01/14.
 */
class Url extends LongKeyedMapper[Url] with IdPK {
  def getSingleton = Url

  object url extends MappedString(this, 10000)

  object pid extends MappedLongForeignKey(this, Job)

}

object Url extends Url with LongKeyedMetaMapper[Url] {
  override def dbTableName = "url"
}