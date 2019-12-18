package scalapb_circe

import com.google.protobuf.struct._
import jsontest.test3.StructTest
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class StructFormatSpec extends AnyFlatSpec with Matchers with JavaAssertions {
  "Empty value" should "be serialized to null" in {
    JsonFormat.toJsonString(Value()) must be("null")
  }

  "NullValue" should "be serialized and parsed from JSON correctly" in {
    JsonFormat.fromJsonString[StructTest]("""{"nv": null}""") must be(StructTest())
    JsonFormat.fromJsonString[StructTest]("""{"nv": "NULL_VALUE"}""") must be(StructTest())
    JsonFormat.fromJsonString[StructTest]("""{"nv": 0}""") must be(StructTest())
    JsonFormat.fromJsonString[StructTest]("""{"repNv": [null, 0, null]}""") must be(
      StructTest(repNv = Seq(NullValue.NULL_VALUE, NullValue.NULL_VALUE, NullValue.NULL_VALUE))
    )
  }
}
