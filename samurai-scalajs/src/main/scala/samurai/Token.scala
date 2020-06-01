package samurai

import Figure._

trait Token
trait InfluenceToken { 
    def influence: Int
}
trait CharacterToken

case class FigureToken(figure: Figure, influence: Int) extends Token with InfluenceToken
case class SamuraiToken(influence: Int) extends Token with InfluenceToken
case class ExchangeToken() extends Token
case class Ship(influence: Int) extends Token with InfluenceToken with CharacterToken
case class Ronin(influence: Int) extends Token with InfluenceToken with CharacterToken
case class FigureExchange() extends Token with CharacterToken