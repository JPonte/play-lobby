package samurai

import core.{GameInfo, GameStatus, Username}
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import org.scalajs.dom
import org.scalajs.dom.{MessageEvent, document, html}
import samurai.Board._
import samurai.draw._
import utils._
import websocket._

import scala.util.{Random, Try}

object MainApp {

  case class CanvasPosition(x: Double, y: Double)

  case class MouseState(position: CanvasPosition, down: Boolean)

  case class Rect(x: Double, y: Double, width: Double, height: Double) {
    def isInside(position: CanvasPosition): Boolean =
      position.x >= x && position.y >= y && position.x <= x + width && position.y <= y + height
  }

  val pi6cos: Double = Math.cos(Math.PI / 6)
  val pi6sin: Double = 0.5

  case class BoardDrawProps(drawRect: Rect, columns: Int, rows: Int) {
    val hexRadius: Double = Math
      .min(
        drawRect.width / (pi6cos * (2 * columns + 1)),
        drawRect.height / (1.5 * rows + 0.5)
      )

    val xStep: Double = 2 * hexRadius * pi6cos
    val yStep: Double = hexRadius + hexRadius * pi6sin

    val offsetX: Double =
      xStep / 2 + (drawRect.width - hexRadius * pi6cos * (2 * columns + 1)) / 2
    val offsetY: Double =
      hexRadius + (drawRect.height - hexRadius * (1.5 * rows + 0.5)) / 2
  }

  case class PlayerTokenDrawProps(drawRect: Rect, columns: Int, rows: Int) {
    val hexRadius: Double = Math
      .min(
        drawRect.width / (columns * 2),
        drawRect.height / (rows * 2)
      )

    val xStep: Double = 2 * hexRadius
    val yStep: Double = 2 * hexRadius

    val offsetX: Double = (drawRect.width - (columns * xStep)) / 2 + xStep / 2
    val offsetY: Double = (drawRect.height - (rows * yStep)) / 2 + yStep / 2
  }

  def main(args: Array[String]): Unit = {
    val canvas =
      document.getElementById("main-canvas").asInstanceOf[html.Canvas]
    val context =
      canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

    val username = Username(document.getElementById("username").asInstanceOf[html.Input].value)
    val url = Try(document.getElementById("data-url").asInstanceOf[html.Input].value).getOrElse("")

    canvas.width = dom.window.innerWidth.toInt
    canvas.height = dom.window.innerHeight.toInt

    dom.window.onresize = { _ =>
      canvas.width = dom.window.innerWidth.toInt
      canvas.height = dom.window.innerHeight.toInt
    }

    var gameState = GameState.initialGameState(Seq(Username("Aonte"), Username("Bonte")), addFiguresAuto = true)
    var gameInfo = GameInfo(0, "Game", 2, None, Seq(Username("Aonte"), Username("Bonte")), GameStatus.Running)

    def selfPlayerId = {
      val i = gameInfo.players.indexOf(username)
      if (i < 0) 0 else i
    }

    def onSocketMessage(event: MessageEvent): Unit = {
      val jsonData = decode[ServerWebSocketMessage](event.data.toString)
      jsonData match {
        case Right(UpdatedGameState(gi, Some(state))) =>
          gameState = state
          gameInfo = gi
        case m => println(s"Couldn't process $m")
      }
    }

    val socket = new WebSocketWrapper(url, _ => onSocketMessage,
      s => { _ =>
        s.send(ClientRequestGameState.asInstanceOf[ClientWebSocketMessage].asJson.noSpaces)
      })

    while (gameState.figuresDeck.nonEmpty) {
      val unsetCities = gameState.board.filter {
        case (_, BoardTile(Tile.City, figures, _)) => figures.size < 2
        case _ => false
      }
      val unsetVillages = gameState.board.filter {
        case (_, BoardTile(Tile.Village, figures, _)) => figures.isEmpty
        case _ => false
      }
      val unsetTiles = if (unsetCities.nonEmpty) unsetCities else unsetVillages
      val tileIndex = Random.nextInt(unsetTiles.size)
      val tile = unsetTiles.toSeq(tileIndex)

      val availableFigures = gameState.figuresDeck.availableFigures -- tile._2.figures
      val figureIndex = Random.nextInt(availableFigures.size)
      val figure = availableFigures.toSeq(figureIndex)

      val gameMove = AddFigure(gameState.currentPlayer, figure, tile._1)
      gameState.play(gameMove).foreach(gameState = _)
    }

    val cols = gameState.board.keys.map(_.column).max + 1
    val rows = gameState.board.keys.map(_.row).max + 1

    var mouse = MouseState(CanvasPosition(0, 0), down = false)
    var clickPosition = Option(CanvasPosition(0, 0))
    var selectedToken = Option.empty[Int]

    var boardDrawProps = BoardDrawProps(
      Rect(0, 0, dom.window.innerWidth, dom.window.innerHeight),
      cols,
      rows
    )
    val maxPlayerTokens = 5
    var playerTokenDrawProps =
      PlayerTokenDrawProps(Rect(0, 0, 0, 0), maxPlayerTokens, 1)

    var endButtonRect = Rect(
      dom.window.innerWidth * 0.85,
      dom.window.innerHeight * 0.61,
      dom.window.innerWidth * 0.1,
      dom.window.innerHeight * 0.1
    )

    dom.window.onmousedown = { event =>
      val clientRect = canvas.getBoundingClientRect()
      mouse = MouseState(CanvasPosition(event.clientX - clientRect.left, event.clientY - clientRect.top), down = true)
    }

    dom.window.onmousemove = { event =>
      val clientRect = canvas.getBoundingClientRect()
      mouse = mouse.copy(position = CanvasPosition(event.clientX - clientRect.left, event.clientY - clientRect.top))
    }

    dom.window.onmouseup = { event =>
      val clientRect = canvas.getBoundingClientRect()
      mouse = MouseState(CanvasPosition(event.clientX - clientRect.left, event.clientY - clientRect.top), down = false)
      clickPosition = Some(CanvasPosition(event.clientX - clientRect.left, event.clientY - clientRect.top))
    }

    def draw() {

      boardDrawProps = BoardDrawProps(
        Rect(
          0,
          dom.window.innerHeight * 0.1,
          dom.window.innerWidth,
          dom.window.innerHeight * 0.5
        ),
        cols,
        rows
      )

      playerTokenDrawProps = PlayerTokenDrawProps(
        Rect(
          dom.window.innerWidth * 0.2,
          dom.window.innerHeight * 0.61,
          dom.window.innerWidth * 0.6,
          dom.window.innerHeight * 0.1
        ),
        maxPlayerTokens,
        1
      )

      endButtonRect = Rect(
        dom.window.innerWidth * 0.85,
        dom.window.innerHeight * 0.61,
        dom.window.innerWidth * 0.1,
        dom.window.innerHeight * 0.1
      )

      val hoveredHex = getHoveredHex(mouse.position, boardDrawProps)
      val highlightedHexes = hoveredHex.map(Board.getNeighbours)
        .map(_.filter(n => gameState.board.get(n).exists(t => Tile.Settlements.contains(t.tile))))
        .getOrElse(Set()).map(_ -> "#eeeeff").toMap ++ hoveredHex.map(h => Map(h -> "#000000")).getOrElse(Map())

      val hoveredToken = getHoveredPlayerToken(mouse.position, playerTokenDrawProps)

      clickPosition.flatMap(getHoveredPlayerToken(_, playerTokenDrawProps)).foreach { click =>
        println(click)
        val tokens = gameState.players(selfPlayerId).tokens
        if (tokens.size > click.column && click.column >= 0) {
          selectedToken = Some(click.column)
          println(selectedToken)
        }
      }

      clickPosition.flatMap(getHoveredHex(_, boardDrawProps)).foreach { click =>
        selectedToken.foreach { st =>
          socket.send(ClientSamuraiGameMove(AddToken(selfPlayerId, st, click)).asInstanceOf[ClientWebSocketMessage].asJson.noSpaces)
        }
      }

      clickPosition.foreach { pos =>
        if (endButtonRect.isInside(pos)) {
          socket.send(ClientSamuraiGameMove(EndTurn(selfPlayerId)).asInstanceOf[ClientWebSocketMessage].asJson.noSpaces)
        }
      }

      clickPosition = None

      val buttonHover = endButtonRect.isInside(mouse.position)
      val buttonPressed = buttonHover && mouse.down
      val buttonIsActive = gameState.currentPlayer == selfPlayerId && (gameState.fullTokensPlayed || gameState.characterTokensPlayed)

      context.clearRect(0, 0, canvas.width, canvas.height)
      context.fillStyle = "#eee"
      context.fillRect(0, 0, canvas.width, canvas.height)

      drawBoard(gameState.board, boardDrawProps, context, highlightedHexes)

      drawPlayerTokens(
        gameState.players(selfPlayerId).tokens,
        playerTokenDrawProps,
        hoveredToken,
        selectedToken,
        context
      )

      drawEndTurnButton(
        endButtonRect, active = buttonIsActive, buttonHover, buttonPressed, context
      )

      val fontSize = Math.min(canvas.width * 0.02f, canvas.height * 0.02f)
      drawPlayers(CanvasPosition(fontSize, 2 * fontSize), fontSize, gameInfo, gameState, context)

      clickPosition
        .map(mp => getHoveredHex(mp, boardDrawProps))
        .foreach(println)
      clickPosition = None
      dom.window.requestAnimationFrame(_ => draw())
    }

    dom.window.requestAnimationFrame(_ => draw())
  }

  def drawPlayers(position: CanvasPosition, fontSize: Float, gameInfo: GameInfo, gameState: GameState, context: dom.CanvasRenderingContext2D): Unit = {
    context.fillStyle = "#000000"
    context.font = s"${fontSize}px Arial"
    gameInfo.players.zipWithIndex.foreach { case (player, index) =>
      val state = gameState.players(index)
      val scoreText = s"${player.value}'s score:\tHelmets(${state.scoreDeck.helmets})\tRice(${state.scoreDeck.riceFields})\tBuddhas(${state.scoreDeck.buddhas})"
      context.fillText(scoreText, position.x, position.y + (index * fontSize * 1.5))
    }

    val currentPlayer = gameInfo.players(gameState.currentPlayer).value

    context.fillText(s"It's $currentPlayer's turn", position.x, position.y + (gameInfo.players.size * fontSize * 1.5) + 2 * fontSize)
  }

  def drawEndTurnButton(drawRect: Rect, active: Boolean, hover: Boolean, pressed: Boolean,
                        context: dom.CanvasRenderingContext2D): Unit = {

    val color = if (active && pressed) {
      "#1AC8DB"
    } else if (active) {
      "#0292B7"
    } else {
      "#DEE2EC"
    }

    val r = Math.min(drawRect.width / 2, drawRect.height / 2)

    drawButton(drawRect.x + drawRect.width / 2, drawRect.y + drawRect.height / 2, r, color, hover && active, context)
  }

  def drawPlayerTokens(
                        tokens: Seq[Token],
                        props: PlayerTokenDrawProps,
                        hoveredToken: Option[MatrixPosition],
                        selectedToken: Option[Int],
                        context: dom.CanvasRenderingContext2D
                      ): Unit = {

    // context.beginPath()
    // context.fillStyle = "#cccc55"
    // context.fillRect(
    //   props.drawRect.x,
    //   props.drawRect.y,
    //   props.drawRect.width,
    //   props.drawRect.height
    // )
    // context.fill()
    // context.closePath()

    tokens.zipWithIndex.foreach {
      case (token, i) =>
        val x = i % props.columns
        val y = i / props.columns
        val centerX = x * props.xStep + props.offsetX + props.drawRect.x
        val centerY = y * props.yStep + props.offsetY + props.drawRect.y

        if (selectedToken.contains(i)) {
          drawHex(centerX, centerY, props.hexRadius, "#fd9e61", None, context)
        }

        hoveredToken.foreach {
          case MatrixPosition(`i`, 0) =>
            drawHex(centerX, centerY, props.hexRadius, "#000000", None, context)
          case _ =>
        }

        drawToken(token, centerX, centerY, props.hexRadius * 0.8, context)
    }
  }

  def drawBoard(
                 board: Board,
                 props: BoardDrawProps,
                 context: dom.CanvasRenderingContext2D,
                 highlightedHexes: Map[MatrixPosition, String]
               ): Unit = {
    // context.beginPath()
    // context.fillStyle = "#cccccc"
    // context.fillRect(
    //   props.drawRect.x,
    //   props.drawRect.y,
    //   props.drawRect.width,
    //   props.drawRect.height
    // )
    // context.fill()
    // context.closePath()

    board.foreach {
      case (MatrixPosition(x, y), boardTile) =>
        val offsetX = if (y % 2 == 0) props.xStep / 2 else 0
        val centerX =
          x * props.xStep + props.offsetX + offsetX + props.drawRect.x
        val centerY = y * props.yStep + props.offsetY + props.drawRect.y

        val highlightColor = highlightedHexes.get(MatrixPosition(x, y))

        val drawRadius =
          if (highlightColor.nonEmpty) props.hexRadius * 0.8 else props.hexRadius

        val color1 = boardTile.tile match {
          case Tile.Empty => Option.empty[String]
          case Tile.Sea => Some("#8ea5ff")
          case _ => Some("#dbc478")
        }

        val color2 = boardTile.tile match {
          case Tile.Village => Some("#c078db")
          case Tile.City => Some("#d6405b")
          case Tile.Edo => Some("#f4d435")
          case _ => Option.empty[String]
        }

        color1.foreach { c =>
          highlightColor.foreach { cc =>
            drawHex(
              centerX,
              centerY,
              props.hexRadius,
              cc,
              None,
              context
            )
          }

          drawHex(
            centerX,
            centerY,
            drawRadius,
            c,
            None,
            context
          )
        }
        color2.foreach(c =>
          drawHex(
            centerX,
            centerY,
            props.hexRadius * 0.5,
            c,
            None,
            context
          )
        )
        if (boardTile.figures.nonEmpty)
          drawFigures(
            boardTile.figures,
            centerX,
            centerY,
            props.hexRadius,
            context
          )

        boardTile.token.foreach(
          drawToken(_, centerX, centerY, props.hexRadius * 0.8, context)
        )
    }
  }

  def getHoveredHex(
                     mouse: CanvasPosition,
                     props: BoardDrawProps
                   ): Option[MatrixPosition] = {

    val row = (mouse.y - props.offsetY - props.drawRect.y) / props.yStep
    val col =
      if (Math.round(row) % 2 == 0)
        (mouse.x - props.xStep / 2 - props.offsetX - props.drawRect.x) / props.xStep
      else (mouse.x - props.offsetX - props.drawRect.x) / props.xStep

    if (props.drawRect.isInside(mouse))
      Some(MatrixPosition(Math.round(col).toInt, Math.round(row).toInt))
    else
      None
  }

  def getHoveredPlayerToken(
                             mouse: CanvasPosition,
                             props: PlayerTokenDrawProps
                           ): Option[MatrixPosition] = {

    val row = (mouse.y - props.offsetY - props.drawRect.y) / props.yStep
    val col = (mouse.x - props.offsetX - props.drawRect.x) / props.xStep

    if (props.drawRect.isInside(mouse))
      Some(MatrixPosition(Math.round(col).toInt, Math.round(row).toInt))
    else
      None
  }
}
