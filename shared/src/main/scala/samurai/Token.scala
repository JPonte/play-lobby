package samurai

import samurai.Figure._
import samurai.PlayerState.PlayerId

sealed trait Token {
    def playerId: PlayerId
}
sealed trait InfluenceToken {
    def influence: Int
}
sealed trait CharacterToken

case class FigureToken(figure: Figure, influence: Int, playerId: PlayerId) extends Token with InfluenceToken
case class SamuraiToken(influence: Int, playerId: PlayerId) extends Token with InfluenceToken
case class ExchangeToken(influence: Int, playerId: PlayerId) extends Token with InfluenceToken
case class Ship(influence: Int, playerId: PlayerId) extends Token with InfluenceToken with CharacterToken
case class Ronin(influence: Int, playerId: PlayerId) extends Token with InfluenceToken with CharacterToken
case class FigureExchange(playerId: PlayerId) extends Token with CharacterToken