package actors

import actors.ChatManager._
import akka.actor.{Actor, ActorRef}

class ChatManager extends Actor {

  private var users = Set.empty[ActorRef]

  override def receive: Receive = {
    case NewUser(actor: ActorRef) =>
      users = users + actor
    case UserLeft(actor: ActorRef) =>
      users = users - actor
    case Message(message) =>
      users.foreach(_ ! ChatActor.SendMessage(message))
    case _ => println("Unknown message sent to ChatManager :(")
  }
}

object ChatManager {
  case class NewUser(actor: ActorRef)
  case class UserLeft(actor: ActorRef)
  case class Message(message: String)
}