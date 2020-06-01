package samurai

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html
import samurai.Board._
import scala.util.Random

object MainApp {

  case class MousePosition(x: Double, y: Double)
  case class Rect(x: Double, y: Double, width: Double, height: Double)

  case class BoardDrawProps(drawRect: Rect, columns: Int, rows: Int) {
    val hexRadius: Int = Math
      .min(drawRect.width / (columns * 2), drawRect.height / (rows * 1.5))
      .toInt
    val xStep = 2 * hexRadius * Math.cos(Math.PI / 6)
    val yStep = hexRadius + hexRadius * Math.sin(Math.PI / 6)
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
        val setTile = new Random().nextBoolean()

        val token =
          if (t == Tile.Land && setTile)
            Some(Ronin(inf, playerId))
          else if (t == Tile.Sea && setTile)
            Some(Ship(inf, playerId))
          else
            None

        coords -> BoardTile(t, None, token)
    }

    val cols = board.keys.map(_._1).max
    val rows = board.keys.map(_._2).max
    var mouse = MousePosition(0, 0)
    var clickPosition = Option(MousePosition(0, 0))
    var boardDrawProps = BoardDrawProps(
      Rect(0, 0, dom.window.innerWidth, dom.window.innerHeight),
      cols,
      rows
    )

    dom.window.onmousemove = { event =>
      mouse = MousePosition(event.pageX, event.pageY)
    }

    dom.window.onmouseup = { event =>
      clickPosition = Some(MousePosition(event.pageX, event.pageY))
    }

    var prevTime: Double = 0;
    def draw(time: Double) {
      var delta = time - prevTime
      prevTime = time

      boardDrawProps = BoardDrawProps(
        Rect(dom.window.innerWidth * 0.1, 0, dom.window.innerWidth * 0.8, dom.window.innerWidth * 0.5),
        cols,
        rows
      )

      val hoveredHex = getHoveredHex(mouse, boardDrawProps)

      context.clearRect(0, 0, canvas.width, canvas.height);
      drawBoard(board, boardDrawProps, hoveredHex, canvas, context)

      clickPosition
        .map(mp => getHoveredHex(mp, boardDrawProps))
        .foreach(println)
      clickPosition = None
      dom.window.requestAnimationFrame(draw)
    }

    dom.window.requestAnimationFrame(draw)
  }

  def drawBoard(
      board: Board,
      props: BoardDrawProps,
      hoveredHex: Option[(Int, Int)],
      canvas: html.Canvas,
      context: dom.CanvasRenderingContext2D
  ) {

    board.foreach {
      case ((x, y), boardTile) =>
        val offsetX = if (y % 2 == 0) props.xStep / 2 else 0
        val centerX = x * props.xStep + props.xStep / 2 + offsetX + props.drawRect.x
        val centerY = y * props.yStep + props.yStep + props.drawRect.y

        val isHoveredHex = hoveredHex.exists(hh => hh._1 == x && hh._2 == y)

        val drawRadius =
          if (isHoveredHex) props.hexRadius * 0.8 else props.hexRadius

        val color1 = boardTile.tile match {
          case Tile.Empty => Option.empty[String]
          case Tile.Sea   => Some("#8ea5ff")
          case _          => Some("#dbc478")
        }

        val color2 = boardTile.tile match {
          case Tile.Village => Some("#c078db")
          case Tile.City    => Some("#d6405b")
          case Tile.Edo     => Some("#f4d435")
          case _            => Option.empty[String]

        }

        color1.foreach { c =>
          if (isHoveredHex) {
            drawHex(
              centerX,
              centerY,
              props.hexRadius,
              "#000000",
              context
            )
          }

          drawHex(
            centerX,
            centerY,
            drawRadius,
            c,
            context
          )
        }
        color2.foreach(c =>
          drawHex(
            centerX,
            centerY,
            props.hexRadius * 0.5,
            c,
            context
          )
        )
        boardTile.token.foreach(
          drawToken(_, centerX, centerY, props.hexRadius, context)
        )
    }
  }

  def drawHex(
      centerX: Double,
      centerY: Double,
      r: Double,
      colorCode: String,
      context: dom.CanvasRenderingContext2D
  ) {
    context.fillStyle = colorCode
    context.beginPath()
    context.moveTo(centerX, centerY)

    (0 to 6).foreach { a =>
      val x = Math.cos(a * Math.PI / 3 + Math.PI / 6) * r
      val y = Math.sin(a * Math.PI / 3 + Math.PI / 6) * r
      context.lineTo(x + centerX, y + centerY)
    }
    context.closePath()
    context.fill()
  }

  def drawToken(
      token: Token,
      centerX: Double,
      centerY: Double,
      r: Double,
      context: dom.CanvasRenderingContext2D
  ) {
    val text = token match {
      case FigureToken(Figure.Helmet, i, _)    => s"H $i"
      case FigureToken(Figure.Buddha, i, _)    => s"B $i"
      case FigureToken(Figure.RiceField, i, _) => s"R $i"
      case SamuraiToken(i, _)                  => s"S $i"
      case Ship(i, _)                          => s"Sh $i"
      case ExchangeToken(i, _)                 => s"Ex $i"
      case FigureExchange(_)                   => "Fe"
      case Ronin(i, _)                         => s"Ro $i"
    }
    val color = token.playerId match {
      case 1 => "#ff0000"
      case 2 => "#00ff00"
      case 3 => "#0000ff"
      case 4 => "#cccc00"
    }

    drawHex(centerX, centerY, r * 0.8, "#f9f8e5", context)

    context.fillStyle = color
    context.font = s"${r / 2}px Arial"
    val metrics = context.measureText(text)
    context.fillText(text, centerX - metrics.width / 2, centerY + r / 4)
  }

  def getHoveredHex(
      mouse: MousePosition,
      props: BoardDrawProps
  ): Option[(Int, Int)] = {

    val row = (mouse.y - props.yStep - props.drawRect.y) / props.yStep
    val col =
      if (Math.round(row) % 2 == 0)
        (mouse.x - props.xStep - props.drawRect.x) / props.xStep
      else (mouse.x - props.xStep / 2 - props.drawRect.x) / props.xStep

    Some(Math.round(col).toInt, Math.round(row).toInt)
  }

  def squredDist(x1: Double, y1: Double, x2: Double, y2: Double) = {
    Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2)
  }

  def dist(x1: Double, y1: Double, x2: Double, y2: Double) = {
    Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2))
  }
}
