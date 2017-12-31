package scalapb_playjson

import com.google.protobuf.duration.Duration
import com.google.protobuf.timestamp.Timestamp
import jsontest.test.WellKnownTest
import play.api.libs.json.Json.parse
import org.scalatest.{FlatSpec, MustMatchers}

class WellKnownTypesSpec extends FlatSpec with MustMatchers {

  val durationProto = WellKnownTest(duration = Some(Duration(146, 3455)))

  "Duration serializer" should "work" in {
    Durations.writeDuration(Duration(146, 3455)) must be("146.000003455s")
    Durations.writeDuration(Duration(146, 3455000)) must be("146.003455s")
    Durations.writeDuration(Duration(146, 345500000)) must be("146.345500s")
    Durations.writeDuration(Duration(146, 345500000)) must be("146.345500s")
    Durations.writeDuration(Duration(146, 345000000)) must be("146.345s")
    Durations.writeDuration(Duration(146, 0)) must be("146s")
    Durations.writeDuration(Duration(-146, 0)) must be("-146s")
    Durations.writeDuration(Duration(-146, -345)) must be("-146.000000345s")
  }

  "Duration parser" should "work" in {
    Durations.parseDuration("146.000003455s") must be(Duration(146, 3455))
    Durations.parseDuration("146.003455s") must be(Duration(146, 3455000))
    Durations.parseDuration("146.345500s") must be(Duration(146, 345500000))
    Durations.parseDuration("146.345500s") must be(Duration(146, 345500000))
    Durations.parseDuration("146.345s") must be(Duration(146, 345000000))
    Durations.parseDuration("146s") must be(Duration(146, 0))
    Durations.parseDuration("-146s") must be(Duration(-146, 0))
    Durations.parseDuration("-146.000000345s") must be(Duration(-146, -345))
  }

  "duration" should "serialize and parse correctly" in {
    val durationJson = """{
                         |  "duration": "146.000003455s"
                         |}""".stripMargin
    JsonFormat.printer.toJson(durationProto) must be(parse(durationJson))
    JsonFormat.parser.fromJsonString[WellKnownTest](durationJson) must be(durationProto)
  }

  "Timestamp parser" should "work" in {
    val start = Timestamps.parseTimestamp("0001-01-01T00:00:00Z")
    val end = Timestamps.parseTimestamp("9999-12-31T23:59:59.999999999Z")
    start.seconds must be(Timestamps.TIMESTAMP_SECONDS_MIN)
    start.nanos must be(0)
    end.seconds must be(Timestamps.TIMESTAMP_SECONDS_MAX)
    end.nanos must be(999999999)

    Timestamps.writeTimestamp(start) must be("0001-01-01T00:00:00Z")
    Timestamps.writeTimestamp(end) must be("9999-12-31T23:59:59.999999999Z")

    Timestamps.parseTimestamp("1970-01-01T00:00:00Z") must be(Timestamp(0, 0))
    Timestamps.parseTimestamp("1969-12-31T23:59:59.999Z") must be(Timestamp(-1, 999000000))

    Timestamps.writeTimestamp(Timestamp(nanos = 10)) must be("1970-01-01T00:00:00.000000010Z")
    Timestamps.writeTimestamp(Timestamp(nanos = 10000)) must be("1970-01-01T00:00:00.000010Z")
    Timestamps.writeTimestamp(Timestamp(nanos = 10000000)) must be("1970-01-01T00:00:00.010Z")

    Timestamps.writeTimestamp(Timestamps.parseTimestamp("1970-01-01T00:00:00.010+08:35")) must be(
      "1969-12-31T15:25:00.010Z")
    Timestamps.writeTimestamp(Timestamps.parseTimestamp("1970-01-01T00:00:00.010-08:12")) must be(
      "1970-01-01T08:12:00.010Z")
  }

  "timestamp" should "serialize and parse correctly" in {
    val timestampJson = """{
                          |  "timestamp": "2016-09-16T12:35:24.375123456Z"
                          |}""".stripMargin
    val timestampProto =
      WellKnownTest(timestamp = Some(Timestamp(seconds = 1474029324, nanos = 375123456)))
    JsonFormat.parser.fromJsonString[WellKnownTest](timestampJson) must be(timestampProto)
    JsonFormat.printer.toJson(timestampProto) must be(parse(timestampJson))
  }
}
