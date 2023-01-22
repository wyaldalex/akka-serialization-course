package serilization.avro

import akka.actor.ActorSystem
import akka.serialization.Serializer
import com.sksamuel.avro4s.{AvroInputStream, AvroOutputStream, AvroSchema}
import com.typesafe.config.ConfigFactory
import serilization.avro.AvroSerialization_Local.actorSelection
import serilization.custom.{SimpleActor, SimplePersistentActor}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

case class BankAccount(iban: String, bankCode: String, amount: Double, currency: String)
case class CompanyRegistry(name: String, accounts: Seq[BankAccount], activityCode: String, marketCap: Double)

class CompanyAvroSerializer extends Serializer {

  val companyRegistrySchema = AvroSchema[CompanyRegistry]

  override def identifier: Int = 99912

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case c: CompanyRegistry =>
      val baos = new ByteArrayOutputStream()
      val avroOutputStream = AvroOutputStream.binary[CompanyRegistry].to(baos).build(companyRegistrySchema)
      avroOutputStream.write(c)
      avroOutputStream.flush()
      avroOutputStream.close()
      baos.toByteArray
    case _ => throw new IllegalArgumentException("We only support CompnayRegistry Avro Serialization")


  }

  override def includeManifest: Boolean = true

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    val inputStream = AvroInputStream.binary[CompanyRegistry].from(new ByteArrayInputStream(bytes)).build(companyRegistrySchema)
    val companyRegistryIterator: Iterator[CompanyRegistry] = inputStream.iterator
    val companyRegistry = companyRegistryIterator.next()
    inputStream.close()

    companyRegistry
  }
}

object AvroSerialization_Local extends App {
  val config = ConfigFactory.parseString(
    """
      |akka.remote.artery.canonical.port = 2551
      |""".stripMargin)
    .withFallback(ConfigFactory.load("avroSerialization"))

  val system = ActorSystem("LocalSystem", config)
  val actorSelection = system.actorSelection("akka://RemoteSystemAvro@localhost:2552/user/remoteActorAvro")

  actorSelection ! CompanyRegistry("IBM",
    Seq(
      BankAccount("ibmBank","1212",12121.34,"USD"),
      BankAccount("ibmBankEu","1213",123121.34,"EUR")
    ),
    "IT",
    1029109201.13
  )
}

object AvroSerialization_Remote extends App {
  val config = ConfigFactory.parseString(
    """
      |akka.remote.artery.canonical.port = 2552
      |""".stripMargin)
    .withFallback(ConfigFactory.load("avroSerialization"))

  val system = ActorSystem("RemoteSystemAvro", config)
  val simpleActor = system.actorOf(SimpleActor.props,"remoteActorAvro")

}

object AvroSerialization_Persistence extends App {
  val config = ConfigFactory.load("persistentStores").getConfig("postgresStore")
    .withFallback(ConfigFactory.load("avroSerialization"))

  val system = ActorSystem("PersistentSystemAvro", config)
  val simplePersistentActor = system.actorOf(SimplePersistentActor.props("1234x121avro", true), "personAvroPersistentActor")
  simplePersistentActor ! CompanyRegistry("IBM",
    Seq(
      BankAccount("ibmBank","1212",12121.34,"USD"),
      BankAccount("ibmBankEu","1213",123121.34,"EUR")
    ),
    "IT",
    1029109201.13
  )


}
