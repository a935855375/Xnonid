package server.core


import java.util.concurrent.ForkJoinPool

import server.Server

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.{Failure, Success}

/**
  * Provides access to Play's internal ExecutionContext.
  */
private[server] object Execution {

  /**
    * @return the actorsystem's execution context
    */
  @deprecated("Use an injected execution context", "2.6.0")
  def internalContext: ExecutionContextExecutor = {
    Server.privateMaybeApplication match {
      case Success(app) => app.actorSystem.dispatcher
      case Failure(_) => common
    }
  }

  def trampoline = server.libs.streams.Execution.trampoline

  object Implicits {
    implicit def trampoline = Execution.trampoline
  }

  /**
    * Use this as a fallback when the application is unavailable.
    * The ForkJoinPool implementation promises to create threads on-demand
    * and clean them up when not in use (standard is when idle for 2
    * seconds).
    */
  private val common = ExecutionContext.fromExecutor(new ForkJoinPool())

}

