package actors

import akka.actor.Actor

class GameActor(gameId: Int) extends Actor {

  println(s"GameActor for game $gameId started")

  override def receive: Receive = {
    case _ =>
  }
}