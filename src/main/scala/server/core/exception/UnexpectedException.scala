/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package server.core.exception

/**
 * Generic exception for unexpected error cases.
 */
case class UnexpectedException(message: Option[String] = None, unexpected: Option[Throwable] = None)
    extends PlayException(
      "Unexpected exception",
      message.getOrElse {
        unexpected.map(t => "%s: %s".format(t.getClass.getSimpleName, t.getMessage)).getOrElse("")
      },
      unexpected.orNull
    )
