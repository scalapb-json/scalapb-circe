package scalapb_playjson

import java.text.ParseException

import com.google.protobuf.duration.Duration

object Durations {
  val DURATION_SECONDS_MIN = -315576000000L
  val DURATION_SECONDS_MAX = 315576000000L

  def checkValid(duration: com.google.protobuf.duration.Duration) = {
    val secondsInRange = (duration.seconds >= DURATION_SECONDS_MIN &&
      duration.seconds <= DURATION_SECONDS_MAX)
    val nanosInRange = duration.nanos >= -999999999L && duration.nanos <= Timestamps.NANOS_PER_SECOND
    val sameSign =
      !((duration.seconds < 0 || duration.nanos < 0) && (duration.seconds > 0 || duration.nanos > 0))
    require(secondsInRange && nanosInRange && sameSign, "Duration is not valid.")
  }

  def writeDuration(duration: com.google.protobuf.duration.Duration) = {
    checkValid(duration)
    val result = new StringBuilder
    val (seconds, nanos) = if (duration.seconds < 0 || duration.nanos < 0) {
      result.append("-")
      (-duration.seconds, -duration.nanos)
    } else (duration.seconds, duration.nanos)

    result.append(seconds)
    if (nanos != 0) {
      result.append(".")
      result.append(Timestamps.formatNanos(nanos))
    }
    result.append("s")
    result.result()
  }

  def parseNanos(value: String): Int = {
    val h = value.take(9)
    if (!h.forall(_.isDigit)) {
      throw new ParseException("Invalid nanoseconds.", 0)
    }
    h.padTo(9, '0').toInt
  }

  def parseDuration(value: String): Duration = {
    if (!value.endsWith("s")) {
      throw new ParseException("Invalid duration string: " + value, 0)
    }
    val (negative, number) = if (value.startsWith("-")) {
      (true, value.substring(1, value.length - 1))
    } else (false, value.substring(0, value.length - 1))

    val pointPosition = number.indexOf('.')
    val (secondsStr, nanosStr) = if (pointPosition != -1) {
      (number.substring(0, pointPosition), number.substring(pointPosition + 1))
    } else {
      (number, "")
    }
    val seconds = secondsStr.toLong
    val nanos =
      if (nanosStr.isEmpty) 0
      else
        parseNanos(nanosStr)

    if (seconds < 0) {
      throw new ParseException("Invalid duration string: " + value, 0)
    }

    // TODO(thesamet): normalizedDuration?

    com.google.protobuf.duration.Duration(
      seconds = if (negative) -seconds else seconds,
      nanos = if (negative) -nanos else nanos)
  }
}
