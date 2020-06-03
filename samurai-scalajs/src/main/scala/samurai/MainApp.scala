package samurai

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html
import samurai.Board._
import samurai.BoardTile
import samurai.Figure._
import utils.MatrixPosition

import scala.util.Random

object MainApp {

  case class CanvasPosition(x: Double, y: Double)
  case class Rect(x: Double, y: Double, width: Double, height: Double) {
    def isInside(position: CanvasPosition): Boolean =
      position.x >= x && position.y >= y && position.x <=  x +width && position.y <= y + height
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
        val setTile = new Random().nextBoolean()

        val token = None
        // if (t == Tile.Land && setTile)
        //   Some(Ronin(inf, playerId))
        // else if (t == Tile.Sea && setTile)
        //   Some(Ship(inf, playerId))
        // else
        //   None

        val figures: Set[Figure] =
          if (t == Tile.Village) {
            Set(Figure.RiceField)
          } else if (t == Tile.City) {
            Set(Figure.RiceField, Figure.Helmet)
          } else if (t == Tile.Edo)(
            Set(
              Figure.RiceField,
              Figure.Helmet,
              Figure.Buddha
            )
          )
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
      val hoveredToken = getHoveredPlayerToken(mouse, playerTokenDrawProps)

      context.clearRect(0, 0, canvas.width, canvas.height)
      context.fillStyle = "#eee"
      context.fillRect(0, 0, canvas.width, canvas.height)
      drawBoard(board, boardDrawProps, hoveredHex, context)

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
      hoveredHex: Option[CanvasPosition],
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
      case (MatrixPosition(x, y), boardTile) =>
        val offsetX = if (y % 2 == 0) props.xStep / 2 else 0
        val centerX =
          x * props.xStep + props.offsetX + offsetX + props.drawRect.x
        val centerY = y * props.yStep + props.offsetY + props.drawRect.y

        val isHoveredHex = hoveredHex.exists(hh => hh.x == x && hh.y == y)

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

  def drawHex(
      centerX: Double,
      centerY: Double,
      r: Double,
      colorCode: String,
      accentColorCode: Option[String],
      context: dom.CanvasRenderingContext2D
  ): Unit =
    drawPoly(
      centerX,
      centerY,
      r,
      6,
      colorCode,
      context,
      accentColorCode = accentColorCode
    )

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

  def drawFigures(
      figures: Set[Figure],
      centerX: Double,
      centerY: Double,
      hexRadius: Double,
      context: dom.CanvasRenderingContext2D
  ): Unit = {
    val figCount = figures.size
    val figSize = hexRadius * 0.3

    figures.zipWithIndex.foreach {
      case (figure, index) =>
        val (offsetX: Double, offsetY: Double) = figCount match {
          case 3 =>
            val x = Math.cos(2 * index * Math.PI / 3 + Math.PI / 6) * figSize * 1.25
            val y = Math.sin(2 * index * Math.PI / 3 + Math.PI / 6) * figSize * 1.25
            (x, y)
          case 2 =>
            (index * 2 * figSize - figSize, 0.0)
          case _ =>
            (0.0, 0.0)
        }
        figure match {
          case Figure.Buddha =>
            drawBuddha(
              centerX + offsetX,
              centerY + offsetY,
              figSize,
              "#000",
              context
            )
          case Figure.Helmet =>
            drawCastle(
              centerX + offsetX,
              centerY + offsetY,
              figSize,
              "#000",
              context
            )
          case Figure.RiceField =>
            drawRiceField(
              centerX + offsetX,
              centerY + offsetY,
              figSize,
              "#000",
              context
            )
        }
    }
  }

  def drawRiceField(
      centerX: Double,
      centerY: Double,
      r: Double,
      colorCode: String,
      context: dom.CanvasRenderingContext2D
  ): Unit = drawPoly(centerX, centerY, r, 4, colorCode, context, rotation = Math.PI / 4)

  def drawBuddha(
      centerX: Double,
      centerY: Double,
      r: Double,
      colorCode: String,
      context: dom.CanvasRenderingContext2D
  ): Unit = drawPoly(centerX, centerY, r, 5, colorCode, context, rotation = 7 * Math.PI / 10)

  def drawCastle(
      centerX: Double,
      centerY: Double,
      r: Double,
      colorCode: String,
      context: dom.CanvasRenderingContext2D
  ): Unit = drawPoly(centerX, centerY, r, 3, colorCode, context)

  def drawPoly(
      centerX: Double,
      centerY: Double,
      r: Double,
      sides: Int,
      colorCode: String,
      context: dom.CanvasRenderingContext2D,
      rotation: Double = Math.PI / 6.0,
      accentColorCode: Option[String] = None
  ): Unit = {

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

    (0 to sides).foreach { a =>
      val x = Math.cos(2 * a * Math.PI / sides + rotation) * r
      val y = Math.sin(2 * a * Math.PI / sides + rotation) * r
      context.lineTo(x + centerX, y + centerY)
    }
    context.closePath()
    context.fill()
  }

  def getHoveredHex(
                     mouse: CanvasPosition,
                     props: BoardDrawProps
  ): Option[CanvasPosition] = {

    val row = (mouse.y - props.offsetY - props.drawRect.y) / props.yStep
    val col =
      if (Math.round(row) % 2 == 0)
        (mouse.x - props.xStep / 2 - props.offsetX - props.drawRect.x) / props.xStep
      else (mouse.x - props.offsetX - props.drawRect.x) / props.xStep

    if (props.drawRect.isInside(mouse))
      Some(CanvasPosition(Math.round(col).toInt, Math.round(row).toInt))
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
