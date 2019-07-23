package server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.kqueue.{KQueueEventLoopGroup, KQueueServerSocketChannel}
import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelInitializer, ChannelOption}
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.timeout.{ReadTimeoutHandler, WriteTimeoutHandler}

class HttpServer(val port: Int) {
  def run(): Unit = {
    val bossGroup = new KQueueEventLoopGroup()
    val workGroup = new KQueueEventLoopGroup()

    try {
      val b = new ServerBootstrap()
      b.group(bossGroup, workGroup)
        .channel(classOf[KQueueServerSocketChannel])
        .childHandler(new ChannelInitializer[SocketChannel] {
          override def initChannel(ch: SocketChannel): Unit = {
            ch.pipeline().addLast(
              new ReadTimeoutHandler(10),
              new WriteTimeoutHandler(10),
              new HttpServerCodec(),
              new HttpServerHandler())
          }
        })
        .option(ChannelOption.SO_BACKLOG.asInstanceOf[ChannelOption[Any]], 128)
        .childOption(ChannelOption.SO_KEEPALIVE.asInstanceOf[ChannelOption[Any]], true)

      val f = b.bind(port).sync()

      f.channel().closeFuture().sync()
    } finally {
      workGroup.shutdownGracefully()
      bossGroup.shutdownGracefully()
    }
  }
}

