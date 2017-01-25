package com.whisk.scalapb.playjson

import com.google.protobuf.duration.Duration
import jsontest.test.WellKnownTest
import org.scalatest.{FlatSpec, MustMatchers}
import play.api.libs.json.{JsString, Json}

class WellKnownTypesSpec extends FlatSpec with MustMatchers {

  val durationProto = WellKnownTest(duration = Some(Duration(146, 3455)))

  "Duration serializer" should "work" in {
    WellKnownTypes.writeDuration(Duration(146, 3455)) must be(JsString("146.000003455s"))
    WellKnownTypes.writeDuration(Duration(146, 3455000)) must be(JsString("146.003455s"))
    WellKnownTypes.writeDuration(Duration(146, 345500000)) must be(JsString("146.345500s"))
    WellKnownTypes.writeDuration(Duration(146, 345500000)) must be(JsString("146.345500s"))
    WellKnownTypes.writeDuration(Duration(146, 345000000)) must be(JsString("146.345s"))
    WellKnownTypes.writeDuration(Duration(146, 0)) must be(JsString("146s"))
    WellKnownTypes.writeDuration(Duration(-146, 0)) must be(JsString("-146s"))
    WellKnownTypes.writeDuration(Duration(-146, -345)) must be(JsString("-146.000000345s"))
  }

  "Duration parser" should "work" in {
    WellKnownTypes.parseDuration("146.000003455s") must be(Duration(146, 3455))
    WellKnownTypes.parseDuration("146.003455s") must be(Duration(146, 3455000))
    WellKnownTypes.parseDuration("146.345500s") must be(Duration(146, 345500000))
    WellKnownTypes.parseDuration("146.345500s") must be(Duration(146, 345500000))
    WellKnownTypes.parseDuration("146.345s") must be(Duration(146, 345000000))
    WellKnownTypes.parseDuration("146s") must be(Duration(146, 0))
    WellKnownTypes.parseDuration("-146s") must be(Duration(-146, 0))
    WellKnownTypes.parseDuration("-146.000000345s") must be(Duration(-146, -345))
  }

  "duration" should "serialize and parse correctly" in {
    val durationJson = """{
                         |  "duration": "146.000003455s"
                         |}""".stripMargin
    JsonFormat.printer.toJson(durationProto) must be(Json.parse(durationJson))
    JsonFormat.parser.fromJsonString[WellKnownTest](durationJson) must be(durationProto)
  }
}
