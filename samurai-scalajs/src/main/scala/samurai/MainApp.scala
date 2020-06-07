package samurai

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html
import samurai.Board._
import samurai.BoardTile
import samurai.Figure._
import utils.MatrixPosition
import samurai.draw._

import scala.util.Random

object MainApp {

  case class CanvasPosition(x: Double, y: Double)

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

    val xStep = 2 * hexRadius * pi6cos
    val yStep = hexRadius + hexRadius * pi6sin

    val offsetX =
      xStep / 2 + (drawRect.width - hexRadius * pi6cos * (2 * columns + 1)) / 2
    val offsetY =
      hexRadius + (drawRect.height - hexRadius * (1.5 * rows + 0.5)) / 2
  }

  case class PlayerTokenDrawProps(drawRect: Rect, columns: Int, rows: Int) {
    val hexRadius: Double = Math
      .min(
        drawRect.width / (columns * 2),
        drawRect.height / (rows * 2)
      )

    val xStep = 2 * hexRadius
    val yStep = 2 * hexRadius

    val offsetX = (drawRect.width - (columns * xStep)) / 2 + xStep / 2
    val offsetY = (drawRect.height - (rows * yStep)) / 2 + yStep / 2
  }

  def main(args: Array[String]): Unit = {
    val canvas =
      document.getElementById("main-canvas").asInstanceOf[html.Canvas]
    val context =
      canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

    canvas.width = dom.window.innerWidth.toInt;
    canvas.height = dom.window.innerHeight.toInt;

    dom.window.onresize = { event =>
      canvas.width = dom.window.innerWidth.toInt;
      canvas.height = dom.window.innerHeight.toInt;
    }

    //STUB
    val board = twoPlayerBoard.map {
      case (coords, t) =>
        val inf = new Random().nextInt(3) + 1
        val playerId = new Random().nextInt(2) + 1
        val setTile = new Random().nextInt(4) > 2

        val token = None
        if (t == Tile.Land && setTile)
          Some(Ronin(inf, playerId))
        else if (t == Tile.Sea && setTile)
          Some(Ship(inf, playerId))
        else
          None

        val figures: Set[Figure] =
          if (t == Tile.Village) {
            Set(Figure.RiceField)
          } else if (t == Tile.City) {
            Set(Figure.RiceField, Figure.Helmet)
          } else if (t == Tile.Edo) {
            Set(
              Figure.RiceField,
              Figure.Helmet,
              Figure.Buddha
            )
          }
          else {
            Set()
          }

        coords -> BoardTile(t, figures, token)
    }
    val playerTokens = Seq(
      SamuraiToken(1, 1),
      SamuraiToken(1, 1),
      SamuraiToken(2, 1),
      SamuraiToken(3, 1),
      FigureToken(Figure.RiceField, 3, 1)
    )

    val cols = board.keys.map(_.column).max + 1
    val rows = board.keys.map(_.row).max + 1

    var mouse = CanvasPosition(0, 0)
    var clickPosition = Option(CanvasPosition(0, 0))
    var boardDrawProps = BoardDrawProps(
      Rect(0, 0, dom.window.innerWidth, dom.window.innerHeight),
      cols,
      rows
    )
    val maxPlayerTokens = 5
    var playerTokenDrawProps =
      PlayerTokenDrawProps(Rect(0, 0, 0, 0), maxPlayerTokens, 1)

    dom.window.onmousemove = { event =>
      val clientRect = canvas.getBoundingClientRect()
      mouse = CanvasPosition(event.clientX - clientRect.left, event.clientY - clientRect.top)
    }

    dom.window.onmouseup = { event =>
      val clientRect = canvas.getBoundingClientRect()
      clickPosition = Some(CanvasPosition(event.clientX - clientRect.left, event.clientY - clientRect.top))
    }

    var prevTime: Double = 0;
    def draw(time: Double) {
      var delta = time - prevTime
      prevTime = time

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

      val hoveredHex = getHoveredHex(mouse, boardDrawProps)
      val highlightedHexes = hoveredHex.map(Board.getNeighbours)
        .map(_.filter(n => board.get(n).exists(t => Tile.Settlements.contains(t.tile))))
        .getOrElse(Set()).map(_ -> "#eeeeff").toMap ++ hoveredHex.map(h => Map(h -> "#000000")).getOrElse(Map())
      val hoveredToken = getHoveredPlayerToken(mouse, playerTokenDrawProps)

      context.clearRect(0, 0, canvas.width, canvas.height)
      context.fillStyle = "#eee"
      context.fillRect(0, 0, canvas.width, canvas.height)
      drawBoard(board, boardDrawProps, context, highlightedHexes)

      drawPlayerTokens(
        playerTokens,
        playerTokenDrawProps,
        hoveredToken,
        context
      )

      clickPosition
        .map(mp => getHoveredHex(mp, boardDrawProps))
        .foreach(println)
      clickPosition = None
      dom.window.requestAnimationFrame(draw)
    }

    dom.window.requestAnimationFrame(draw)
  }

  def drawPlayerTokens(
                        tokens: Seq[Token],
                        props: PlayerTokenDrawProps,
                        hoveredToken: Option[CanvasPosition],
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

        hoveredToken.foreach {
          case CanvasPosition(`i`, 0) =>
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
                           ): Option[CanvasPosition] = {

    val row = (mouse.y - props.offsetY - props.drawRect.y) / props.yStep
    val col = (mouse.x - props.offsetX - props.drawRect.x) / props.xStep

    if (props.drawRect.isInside(mouse))
      Some(CanvasPosition(Math.round(col).toInt, Math.round(row).toInt))
    else
      None
  }
}
