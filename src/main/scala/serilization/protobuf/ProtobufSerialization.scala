package serilization.protobuf

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import serialization.protobuf.Datamodel
import serialization.protobuf.Datamodel.OnlineStoreUser
import serilization.custom.{SimpleActor, SimplePersistentActor}


/*
      Command used to generate protobuf Model:
      .\main\exec\protoc.exe --java_out=main/java .\main\proto\datamodel.proto
 */

object protoSerialization_Local extends App {

  val config = ConfigFactory.parseString(
    """
      |akka.remote.artery.canonical.port = 2551
    """.stripMargin)
    .withFallback(ConfigFactory.load("protoSerialization"))

  val system = ActorSystem("LocalSystem", config)
  val actorSelection = system.actorSelection("akka://RemoteSystem@localhost:2552/user/remoteActor")

  val onlineStoreUser: OnlineStoreUser = OnlineStoreUser.newBuilder()
    .setStoreId(45621)
    .setUserName("daniel-rockthejvm")
    .setUserEmail("daniel@rockthejvm.com")
    .build()

  actorSelection ! onlineStoreUser
}

object protoSerialization_Remote extends App {
  val config = ConfigFactory.parseString(
    """
      |akka.remote.artery.canonical.port = 2552
    """.stripMargin)
    .withFallback(ConfigFactory.load("protoSerialization"))

  val system = ActorSystem("RemoteSystem", config)
  val simpleActor = system.actorOf(Props[SimpleActor], "remoteActor")
}

object protoSerialization_Persistence extends App {
  val config = ConfigFactory.load("persistentStores").getConfig("postgresStore")
    .withFallback(ConfigFactory.load("protoSerialization"))

  val system = ActorSystem("PersistenceSystem", config)
  val simplePersistentActor = system.actorOf(SimplePersistentActor.props("protobuf-actor"), "protobufActor")

  val onlineStoreUser: OnlineStoreUser = OnlineStoreUser.newBuilder()
    .setStoreId(47281)
    .setUserName("martin-rockthejvm")
    .setUserEmail("martin@rockthejvm.com")
    .build()

  //simplePersistentActor ! onlineStoreUser
}
