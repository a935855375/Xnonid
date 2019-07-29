/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package server.data.format

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.time.temporal._

private[server] object PlayDate {
  def parse(text: CharSequence, formatter: DateTimeFormatter): PlayDate = new PlayDate(formatter.parse(text))
}

private[server] class PlayDate(accessor: TemporalAccessor) {

  private[this] def getOrDefault(field: TemporalField, default: Int): Int = {
    if (accessor.isSupported(field)) accessor.get(field) else default
  }

  def toZonedDateTime(zoneId: ZoneId): ZonedDateTime = {
    val year: Int   = getOrDefault(ChronoField.YEAR, 1970)
    val month: Int  = getOrDefault(ChronoField.MONTH_OF_YEAR, 1)
    val day: Int    = getOrDefault(ChronoField.DAY_OF_MONTH, 1)
    val hour: Int   = getOrDefault(ChronoField.HOUR_OF_DAY, 0)
    val minute: Int = getOrDefault(ChronoField.MINUTE_OF_HOUR, 0)

    ZonedDateTime.of(LocalDateTime.of(year, month, day, hour, minute), zoneId)
  }

}
