package samurai

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html
import samurai.Board._

object MainApp {

  case class MousePosition(x: Double, y: Double)

  case class BoardDrawProps(hexRadius: Double, xStep: Double, yStep: Double)

  def main(args: Array[String]): Unit = {
    val canvas =
      document.getElementById("main-canvas").asInstanceOf[html.Canvas]
    val context =
      canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

    val sparseMatrix = twoPlayerBoard
      .map(_.zipWithIndex)
      .zipWithIndex
      .flatMap {
        case (row, y) =>
          row.filter(_._1 != 0).map(t => (t._2, y) -> t._1)
      }
      .toMap
    println(sparseMatrix)

    canvas.width = dom.window.innerWidth.toInt;
    canvas.height = dom.window.innerHeight.toInt;

    dom.window.onresize = { event =>
      canvas.width = dom.window.innerWidth.toInt;
      canvas.height = dom.window.innerHeight.toInt;
    }

    val board = twoPlayerBoard
    var mouse = MousePosition(0, 0)
    var clickPosition = Option(MousePosition(0, 0))
    var boardDrawProps = BoardDrawProps(0, 0, 0)

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

      val hexRadius = dom.window.innerWidth / (board.map(_.size).max * 2)
      val xStep = 2 * hexRadius * Math.cos(Math.PI / 6)
      val yStep = hexRadius + hexRadius * Math.sin(Math.PI / 6)
      boardDrawProps = BoardDrawProps(hexRadius, xStep, yStep)

      val hoveredHex = getHoveredHex(board, mouse, boardDrawProps)

      context.clearRect(0, 0, canvas.width, canvas.height);
      drawBoard(board, boardDrawProps, hoveredHex, canvas, context)

      clickPosition
        .map(mp => getHoveredHex(board, mp, boardDrawProps))
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

    board.zipWithIndex.foreach {
      case (row, y) =>
        row.zipWithIndex.foreach {
          case (tile, x) =>
            val centerX =
              if (y % 2 == 0) x * props.xStep + props.xStep
              else x * props.xStep + props.xStep / 2
            val centerY = y * props.yStep + props.yStep

            val isHoveredHex = hoveredHex.exists(hh => hh._1 == x && hh._2 == y)

            val drawRadius =
              if (isHoveredHex) props.hexRadius * 0.8 else props.hexRadius

            val color1 = tile match {
              case Tile.Empty => Option.empty[String]
              case Tile.Sea   => Some("#8ea5ff")
              case _          => Some("#dbc478")
            }

            val color2 = tile match {
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
        }
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

  def getHoveredHex(
      board: Board,
      mouse: MousePosition,
      props: BoardDrawProps
  ): Option[(Int, Int)] = {

    val row = (mouse.y - props.yStep) / props.yStep
    val col =
      if (Math.round(row) % 2 == 0)
        (mouse.x - props.xStep) / props.xStep
      else (mouse.x - props.xStep / 2) / props.xStep

    Some(Math.round(col).toInt, Math.round(row).toInt)
  }

  def squredDist(x1: Double, y1: Double, x2: Double, y2: Double) = {
    Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2)
  }

  def dist(x1: Double, y1: Double, x2: Double, y2: Double) = {
    Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2))
  }
}
