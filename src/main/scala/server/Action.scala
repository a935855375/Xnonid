package server

import io.netty.handler.codec.http.{FullHttpResponse, HttpRequest}

import scala.concurrent.Future

trait Action extends (HttpRequest => Future[FullHttpResponse])