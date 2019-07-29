package server.core.system

import java.util.concurrent.atomic.AtomicLong

private[server] object RequestIdProvider {
  private val requestIDs: AtomicLong = new AtomicLong(0)
  def freshId(): Long                = requestIDs.incrementAndGet()
}

