package samurai

import samurai.Figure._

trait Token {
    def playerId: Int
}
trait InfluenceToken { 
    def influence: Int
}
trait CharacterToken

case class FigureToken(figure: Figure, influence: Int, playerId: Int) extends Token with InfluenceToken
case class SamuraiToken(influence: Int, playerId: Int) extends Token with InfluenceToken
case class ExchangeToken(influence: Int, playerId: Int) extends Token with InfluenceToken
case class Ship(influence: Int, playerId: Int) extends Token with InfluenceToken with CharacterToken
case class Ronin(influence: Int, playerId: Int) extends Token with InfluenceToken with CharacterToken
case class FigureExchange(playerId: Int) extends Token with CharacterToken