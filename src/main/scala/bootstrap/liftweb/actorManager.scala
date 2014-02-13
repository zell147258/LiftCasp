package bootstrap.liftweb

import com.typesafe.config.ConfigFactory
import akka.actor.{Props, ActorSystem}
import akka.routing.RoundRobinRouter
import code.akka.{RetriveProxy, CasperSpider}

/**
 * Created by ivan on 26/12/13.
 */
object ActorManager {
  val config = ConfigFactory.load()
  implicit val system = ActorSystem("CasperSpider", config.getConfig("myapp1"))
  lazy val router = system.actorOf(Props(classOf[CasperSpider]), name = "router")


}
