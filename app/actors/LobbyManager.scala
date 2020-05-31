package actors

import actors.LobbyManager._
import akka.actor.{Actor, ActorRef}
import models.{LobbyCommand, PrivateCommand, SystemLobbyMessage, Username, WebSocketCommand}
import akka.pattern.pipe

import scala.concurrent.{ExecutionContext, Future}

class LobbyManager extends Actor {

  private var usersMap = Map.empty[Username, Set[ActorRef]]

  override def receive: Receive = {
    case UserList() =>
      implicit val ec: ExecutionContext = context.dispatcher
      Future(usersMap.keys).pipeTo(sender())
    case NewUser(username, actor) =>
      val existingActors = usersMap.getOrElse(username, Set())
      usersMap = usersMap + (username -> (existingActors + actor))
      if (existingActors.isEmpty) {
        self ! Command(SystemLobbyMessage(s"${username.value} joined the lobby"))
      }
    case UserLeft(username, actor) =>
      val updatedActors = usersMap.get(username).map(_ - actor).getOrElse(Set.empty[ActorRef])
      if (updatedActors.isEmpty) {
        usersMap = usersMap - username
        self ! Command(SystemLobbyMessage(s"${username.value} left the lobby"))
      } else {
        usersMap = usersMap + (username -> updatedActors)
      }
    case Command(command: LobbyCommand) =>
      usersMap.values.flatten.foreach(_ ! LobbyActor.SendCommand(command))
    case Command(command: PrivateCommand) =>
      usersMap.get(command.recipient).foreach(_.foreach(_ ! LobbyActor.SendCommand(command)))
    case m => println(s"Unknown message sent to ChatManager $m")
  }
}

object LobbyManager {
  case class NewUser(username: Username, actor: ActorRef)
  case class UserLeft(username: Username, actor: ActorRef)
  case class Command(command: WebSocketCommand)
  case class UserList()
}