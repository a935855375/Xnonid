import server.router.RouterParser

object Test extends App {
  val router = new RouterParser

  router.init(getClass.getResourceAsStream("routes"))
}
