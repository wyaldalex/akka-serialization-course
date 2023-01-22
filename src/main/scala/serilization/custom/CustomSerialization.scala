package serilization.custom

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.serialization.Serializer
import com.typesafe.config.ConfigFactory

case class Person(name: String, age: Int)

object SimpleActor {
  def props: Props = Props(new SimpleActor)
}
class SimpleActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case x => log.info(s"Received something ${x.toString}")
  }
}

object PersonSerializer {
  val SEPARATOR = "//"
}
class PersonSerializer extends Serializer {
  import PersonSerializer._
  override def identifier: Int = 74328

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case person @ Person(name,age) =>
      println(s"Serializing person $person")
      s"[$name$SEPARATOR$age]".getBytes()
    case _ =>
      throw new IllegalArgumentException("only person type is supported for this serializer")

  }

  override def includeManifest: Boolean = false

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    val string = new String(bytes)
    val values = string.substring(1, string.length - 1).split(SEPARATOR)
    val name = values(0)
    val age = values(1).toInt

    val person = Person(name,age)
    println(s"Deserialized $person")
    person
  }
}

object CustomSerialization_Local extends App {
  val config = ConfigFactory.parseString(
    """
      |akka.remote.artery.canonical.port = 2551
      |""".stripMargin)
    .withFallback(ConfigFactory.load("customSerialization"))

  val system = ActorSystem("LocalSystem", config)
  val actorSelection = system.actorSelection("akka://RemoteSystem@localhost:2552/user/remoteActor")

  actorSelection ! Person("John Locke", 45)
}

object CustomSerialization_Remote extends App {
  val config = ConfigFactory.parseString(
    """
      |akka.remote.artery.canonical.port = 2552
      |""".stripMargin)
    .withFallback(ConfigFactory.load("customSerialization"))

  val system = ActorSystem("RemoteSystem", config)
  val simpleActor = system.actorOf(SimpleActor.props,"remoteActor")

}


