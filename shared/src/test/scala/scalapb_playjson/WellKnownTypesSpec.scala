package scalapb_playjson

import com.google.protobuf.duration.Duration
import com.google.protobuf.timestamp.Timestamp
import jsontest.test.WellKnownTest
import play.api.libs.json.Json.parse
import org.scalatest.{FlatSpec, MustMatchers}

class WellKnownTypesSpec extends FlatSpec with MustMatchers {

  val durationProto = WellKnownTest(duration = Some(Duration(146, 3455)))

  "duration" should "serialize and parse correctly" in {
    val durationJson = """{
                         |  "duration": "146.000003455s"
                         |}""".stripMargin
    JsonFormat.printer.toJson(durationProto) must be(parse(durationJson))
    JsonFormat.parser.fromJsonString[WellKnownTest](durationJson) must be(durationProto)
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
