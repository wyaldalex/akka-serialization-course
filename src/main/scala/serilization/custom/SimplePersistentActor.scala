package serilization.custom

import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor

object SimplePersistentActor {
  def props(persistenceId: String, shouldLog: Boolean = true): Props = Props(new SimplePersistentActor(persistenceId,shouldLog) )
}
class SimplePersistentActor(id: String, shouldLog: Boolean = true)
  extends PersistentActor with ActorLogging {

  override def persistenceId: String = id
  override def receiveCommand: Receive = {
    case message => persist(message) { _ =>
      if(shouldLog) log.info(s"Persisted $message")
    }
  }
  override def receiveRecover: Receive = {
    case event =>
      if(shouldLog) log.info(s"Recovered event $event")
  }
}
