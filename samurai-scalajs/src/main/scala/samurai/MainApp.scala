package samurai

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html
import samurai.Board._
import scala.util.Random

object MainApp {

  case class MousePosition(x: Double, y: Double)
  case class Rect(x: Double, y: Double, width: Double, height: Double)

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
        val setTile = new Random().nextBoolean()

        val token = None
        // if (t == Tile.Land && setTile)
        //   Some(Ronin(inf, playerId))
        // else if (t == Tile.Sea && setTile)
        //   Some(Ship(inf, playerId))
        // else
        //   None

        coords -> BoardTile(t, None, token)
    }
    val playerTokens = Seq(
      SamuraiToken(1, 1),
      SamuraiToken(1, 1),
      SamuraiToken(2, 1),
      SamuraiToken(3, 1),
      FigureToken(Figure.RiceField, 3, 1)
    )

    val cols = board.keys.map(_._1).max + 1
    val rows = board.keys.map(_._2).max + 1

    var mouse = MousePosition(0, 0)
    var clickPosition = Option(MousePosition(0, 0))
    var boardDrawProps = BoardDrawProps(
      Rect(0, 0, dom.window.innerWidth, dom.window.innerHeight),
      cols,
      rows
    )
    val maxPlayerTokens = 5
    var playerTokenDrawProps =
      PlayerTokenDrawProps(Rect(0, 0, 0, 0), maxPlayerTokens, 1)

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
      val hoveredToken = getHoveredPlayerToken(mouse, playerTokenDrawProps)

      context.clearRect(0, 0, canvas.width, canvas.height)
      context.fillStyle = "#eee"
      context.fillRect(0, 0, canvas.width, canvas.height)
      drawBoard(board, boardDrawProps, hoveredHex, canvas, context)

      drawPlayerTokens(
        playerTokens,
        playerTokenDrawProps,
        hoveredToken,
        canvas,
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
      hoveredToken: Option[(Int, Int)],
      canvas: html.Canvas,
      context: dom.CanvasRenderingContext2D
  ) = {

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
        val y = (i / props.columns).toInt
        val centerX = x * props.xStep + props.offsetX + props.drawRect.x
        val centerY = y * props.yStep + props.offsetY + props.drawRect.y

        hoveredToken.foreach {
          case (`i`, 0) =>
            drawHex(centerX, centerY, props.hexRadius, "#000000", None, context)
          case x => println(x)
        }

        drawToken(token, centerX, centerY, props.hexRadius * 0.8, context)
    }
  }

  def drawBoard(
      board: Board,
      props: BoardDrawProps,
      hoveredHex: Option[(Int, Int)],
      canvas: html.Canvas,
      context: dom.CanvasRenderingContext2D
  ) {
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
      case ((x, y), boardTile) =>
        val offsetX = if (y % 2 == 0) props.xStep / 2 else 0
        val centerX =
          x * props.xStep + props.offsetX + offsetX + props.drawRect.x
        val centerY = y * props.yStep + props.offsetY + props.drawRect.y

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
        boardTile.token.foreach(
          drawToken(_, centerX, centerY, props.hexRadius * 0.8, context)
        )
    }
  }

  def drawHex(
      centerX: Double,
      centerY: Double,
      r: Double,
      colorCode: String,
      accentColorCode: Option[String],
      context: dom.CanvasRenderingContext2D
  ) {

    accentColorCode match {
      case Some(accent) =>
        val gradient = context.createRadialGradient(
          centerX,
          centerY,
          0.65 * r,
          centerX,
          centerY,
          r * 1.5
        )
        gradient.addColorStop(0, colorCode)
        gradient.addColorStop(1, accent)
        context.fillStyle = gradient
      case _ => context.fillStyle = colorCode
    }

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

    drawHex(centerX, centerY, r, "#f9f8e5", Some(color), context)

    context.fillStyle = color
    context.font = s"${r / 2}px Arial"
    val metrics = context.measureText(text)
    context.fillText(text, centerX - metrics.width / 2, centerY + r / 4)
  }

  def getHoveredHex(
      mouse: MousePosition,
      props: BoardDrawProps
  ): Option[(Int, Int)] = {

    val row = (mouse.y - props.offsetY - props.drawRect.y) / props.yStep
    val col =
      if (Math.round(row) % 2 == 0)
        (mouse.x - props.xStep / 2 - props.offsetX - props.drawRect.x) / props.xStep
      else (mouse.x - props.offsetX - props.drawRect.x) / props.xStep

    Some(Math.round(col).toInt, Math.round(row).toInt)
  }

  def getHoveredPlayerToken(
      mouse: MousePosition,
      props: PlayerTokenDrawProps
  ): Option[(Int, Int)] = {

    val row = (mouse.y - props.offsetY - props.drawRect.y) / props.yStep
    val col = (mouse.x - props.offsetX - props.drawRect.x) / props.xStep

    Some(Math.round(col).toInt, Math.round(row).toInt)
  }

  def squredDist(x1: Double, y1: Double, x2: Double, y2: Double) = {
    Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2)
  }

  def dist(x1: Double, y1: Double, x2: Double, y2: Double) = {
    Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2))
  }
}
