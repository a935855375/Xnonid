package server

import io.netty.buffer.Unpooled
import io.netty.channel.{ChannelFutureListener, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.handler.codec.http.HttpResponseStatus.OK
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import io.netty.handler.codec.http._

import scala.concurrent.{ExecutionContext, Future}

object HttpServerHandler {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val demo: HttpRequest => Future[HttpResponse] = _ => Future {
    val response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer("GG".getBytes("utf-8")))
    response.headers().set("Content-Type", "text/plain")
    response.headers().setInt("Content-Length", response.content().readableBytes())
    response
  }
}


class HttpServerHandler extends ChannelInboundHandlerAdapter {

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {

    msg match {
      case request: HttpRequest =>

        implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

        HttpServerHandler.demo(request).map(response => {
          val keepAlive = HttpUtil.isKeepAlive(request)
          if (!keepAlive) {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
          } else {
            response.headers().set("Connection", "keep-alive")
            ctx.writeAndFlush(response)
          }
        })
      case _ =>
    }

  }

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    ctx.flush()
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    ctx.close()
  }
}
