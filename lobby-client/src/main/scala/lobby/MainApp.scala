package lobby

import org.scalajs.dom.document


object MainApp {
  def main(args: Array[String]): Unit = {

    if (document.getElementById("lobby-container") != null) {
      Lobby.run()
    } else if (document.getElementById("party-lobby-container") != null) {
      Party.run()
    }
  }
}