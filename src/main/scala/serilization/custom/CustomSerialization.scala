package serilization.custom

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.serialization.Serializer
import com.typesafe.config.ConfigFactory

//import spray everything
import spray.json._

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

class PersonJsonSerializer extends Serializer with DefaultJsonProtocol {

  implicit val personFormat = jsonFormat2(Person)

  override def identifier: Int = 43678

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case p: Person =>
      val json: String = p.toJson.prettyPrint
      println(s"Converting $p to json $json")
      json.getBytes()
    case _ => throw new IllegalArgumentException("only person type is supported for this serializer")
  }

  override def includeManifest: Boolean = false

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    val string = new String(bytes)
    val person = string.parseJson.convertTo[Person]
    println(s"Deserialized $string to $person")
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

object CustomSerialization_Persistence extends App {
  val config = ConfigFactory.load("persistentStores").getConfig("postgresStore")
    .withFallback(ConfigFactory.load("customSerialization"))

  val system = ActorSystem("PersistentSystem", config)
  val simplePersistentActor = system.actorOf(SimplePersistentActor.props("1234x121", true), "personJsonPersistentActor")
  simplePersistentActor ! Person("John Locke Persistent",60)


}


