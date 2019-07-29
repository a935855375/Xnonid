import java.lang.management.ManagementFactory

import server.{HttpServer, Logger}

object Main {
  def main(args: Array[String]): Unit = {
    /*var port = 8080

    if (args.length > 0) {
      port = args(0).toInt
    }

    new HttpServer(port).run()*/

    val logger = Logger(this.getClass)

    println(ManagementFactory.getRuntimeMXBean.getName)

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        println("GG")
      }
    })


    logger.info("asd")

    logger.error("asd")
  }
}
