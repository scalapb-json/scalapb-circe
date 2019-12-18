package scalapb_circe

import com.google.protobuf.duration.Duration
import com.google.protobuf.timestamp.Timestamp
import jsontest.test.WellKnownTest
import io.circe.parser.parse
import EitherOps._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class WellKnownTypesSpec extends AnyFlatSpec with Matchers {

  val durationProto = WellKnownTest(duration = Some(Duration(146, 3455)))

  "duration" should "serialize and parse correctly" in {
    val durationJson = """{
                         |  "duration": "146.000003455s"
                         |}""".stripMargin
    JsonFormat.printer.toJson(durationProto) must be(parse(durationJson).getOrError)
    JsonFormat.parser.fromJsonString[WellKnownTest](durationJson) must be(durationProto)
  }

  "timestamp" should "serialize and parse correctly" in {
    val timestampJson = """{
                          |  "timestamp": "2016-09-16T12:35:24.375123456Z"
                          |}""".stripMargin
    val timestampProto =
      WellKnownTest(timestamp = Some(Timestamp(seconds = 1474029324, nanos = 375123456)))
    JsonFormat.parser.fromJsonString[WellKnownTest](timestampJson) must be(timestampProto)
    JsonFormat.printer.toJson(timestampProto) must be(parse(timestampJson).getOrError)
  }
}
