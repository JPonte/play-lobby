package samurai

import org.scalajs.dom
import samurai.Figure.Figure

package object draw {
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
      case FigureToken(Figure.Helmet, i, _) => s"H $i"
      case FigureToken(Figure.Buddha, i, _) => s"B $i"
      case FigureToken(Figure.RiceField, i, _) => s"R $i"
      case SamuraiToken(i, _) => s"S $i"
      case Ship(i, _) => s"Sh $i"
      case ExchangeToken(i, _) => s"Ex $i"
      case FigureExchange(_) => "Fe"
      case Ronin(i, _) => s"Ro $i"
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

}
