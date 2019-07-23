package server.router

import java.io.InputStream
import java.util.Scanner


object RouterParser {

  case class RouterMethodParam(name: String, `type`: String)

  case class RouterMeta(httpMethodName: String,
                        classPath: String,
                        callMethodName: String,
                        routerMetas: Array[RouterMeta]
                       )

}

class RouterParser {
  def init(configPath: InputStream): Unit = {

    val sc = new Scanner(configPath)

    while (sc.hasNextLine) {
      val line = sc.nextLine()
      if (line.length != 0)
        parseOneLine(line)
    }
  }

  def parseOneLine(line: String): Unit = {
    val list = line.split("\\s+")

    println(list.toList)
  }
}
