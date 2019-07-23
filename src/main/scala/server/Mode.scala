package server

/**
  * Application mode, either `Dev`, `Test`, or `Prod`.
  *
  * @see [[server.Mode]]
  */
sealed abstract class Mode(val asJava: server.core.utils.Mode)

object Mode {

  case object Dev extends server.Mode(server.core.utils.Mode.DEV)

  case object Test extends server.Mode(server.core.utils.Mode.TEST)

  case object Prod extends server.Mode(server.core.utils.Mode.PROD)

  lazy val values: Set[server.Mode] = Set(Dev, Test, Prod)
}
