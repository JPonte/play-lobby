package actors

import actors.ChatActor.SendMessage
import akka.actor.{Actor, ActorRef, Props}
import models.Username

class ChatActor(username: Username, out: ActorRef, manager: ActorRef) extends Actor {

  manager ! ChatManager.NewUser(self)

  override def receive: Receive = {
    case message: String => manager ! ChatManager.Message(message)
    case SendMessage(message) => out ! message
    case m => println(s"Unhandled message: $m")
  }

  override def postStop(): Unit = {
    manager ! ChatManager.UserLeft(self)
    println(s"$username logged out")
  }

}

object ChatActor {
  def props(username: Username, out: ActorRef, manager: ActorRef): Props = Props(new ChatActor(username, out, manager))

  case class SendMessage(message: String)
}