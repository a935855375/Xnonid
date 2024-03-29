package server.routing

/**
  * Information about a `Handler`, especially useful for loading the handler
  * with reflection.
  */
case class HandlerDef(
                       classLoader: ClassLoader,
                       routerPackage: String,
                       controller: String,
                       method: String,
                       parameterTypes: Seq[Class[_]],
                       verb: String,
                       path: String,
                       comments: String = "",
                       modifiers: Seq[String] = Seq.empty
                     ) extends server.j.routing.HandlerDef
