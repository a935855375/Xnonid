package server

/**
  * Application mode, either `Dev`, `Test`, or `Prod`.
  *
  * @see [[server.Mode]]
  */
sealed abstract class Mode(val asJava: utils.Mode)

object Mode {

  case object Dev extends server.Mode(utils.Mode.DEV)

  case object Test extends server.Mode(utils.Mode.TEST)

  case object Prod extends server.Mode(utils.Mode.PROD)

  lazy val values: Set[server.Mode] = Set(Dev, Test, Prod)
}
