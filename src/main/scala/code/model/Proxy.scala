package code.model

import net.liftweb.mapper._
/**
  * Created by ivan on 14/01/14.
  */
class Proxy extends LongKeyedMapper[Proxy] with IdPK {
  def getSingleton = Proxy

  object proxy extends MappedString(this, 100)
  object description extends MappedString(this, 10000)

}

object Proxy extends Proxy with LongKeyedMetaMapper[Proxy] {
  override def dbTableName = "proxy"
}