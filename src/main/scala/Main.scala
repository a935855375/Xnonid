import server.HttpServer

object Main {
  def main(args: Array[String]): Unit = {
    var port = 8080

    if (args.length > 0) {
      port = args(0).toInt
    }

    new HttpServer(port).run()
  }
}
