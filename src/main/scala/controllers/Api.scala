package controllers

import com.google.inject.{Inject, Singleton}
import io.netty.buffer.Unpooled
import server.Action
import services.FooService

import scala.concurrent.{ExecutionContext, Future}
import io.netty.handler.codec.http.HttpResponseStatus.OK
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import io.netty.handler.codec.http._


@Singleton
class Api @Inject()(fooService: FooService)(implicit ec: ExecutionContext) {

  def test: Action = { implicit request =>
    fooService.sayHello.map { hello =>
      val response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(hello.getBytes("utf-8")))
      response.headers().set("Content-Type", "text/plain")
      response.headers().setInt("Content-Length", response.content().readableBytes())
      response
    }
  }

  def test2(id: Int): Action = { _ =>
    Future {
      val response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(id.toString.getBytes("utf-8")))
      response.headers().set("Content-Type", "text/plain")
      response.headers().setInt("Content-Length", response.content().readableBytes())
      response
    }
  }

  def tt = println("GG")
}
