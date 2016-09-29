package com.whisk.scalapb.playjson

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json._
import test.test.{MyEnum, MyTest}

class ScalaPBJsonSpec extends FlatSpec with Matchers {

  val TestProto = MyTest().update(
    _.hello := "Foo",
    _.foobar := 37,
    _.primitiveSequence := Seq("a", "b", "c"),
    _.repMessage := Seq(MyTest(), MyTest(hello = Some("h11"))),
    _.optMessage := MyTest().update(_.foobar := 39),
    _.stringToInt32 := Map("foo" -> 14, "bar" -> 19),
    _.intToMytest := Map(14 -> MyTest(), 35 -> MyTest(hello = Some("boo"))),
    _.repEnum := Seq(MyEnum.V1, MyEnum.V2, MyEnum.UNKNOWN),
    _.optEnum := MyEnum.V2,
    _.intToEnum := Map(32 -> MyEnum.V1, 35 -> MyEnum.V2),
    _.stringToBool := Map("ff" -> false, "tt" -> true),
    _.boolToString := Map(false -> "ff", true -> "tt")
  )

  val TestJson =
    """{
      |  "hello": "Foo",
      |  "foobar": 37,
      |  "primitiveSequence": ["a", "b", "c"],
      |  "repMessage": [{}, {"hello": "h11"}],
      |  "optMessage": {"foobar": 39},
      |  "stringToInt32": {"foo": 14, "bar": 19},
      |  "intToMytest": {"14": {}, "35": {"hello": "boo"}},
      |  "repEnum": ["V1", "V2", "UNKNOWN"],
      |  "optEnum": "V2",
      |  "intToEnum": {"32": "V1", "35": "V2"},
      |  "stringToBool": {"ff": false, "tt": true},
      |  "boolToString": {"false": "ff", "true": "tt"}
      |}
      |""".stripMargin


  "Empty object" should "give empty json" in {
    ScalaPBJson.toJson(MyTest()) shouldEqual Json.toJson(Map.empty[String, JsValue])
  }

  "Empty json" should "convert to base proto" in {
    ScalaPBJson.fromJsonString[MyTest]("{}") shouldEqual MyTest()
  }

  "TestProto" should "be TestJson when converted to Proto" in {
    ScalaPBJson.toJson(TestProto) shouldEqual Json.parse(TestJson)
  }

  "TestJson" should "be TestProto when parsed from json" in {
    ScalaPBJson.fromJsonString[MyTest](TestJson) shouldEqual TestProto
  }

  "MyEnum" should "have formats to convert" in {
    implicit val enumFmt: Format[MyEnum] = ScalaPBJson.enumFormat[MyEnum]

    Json.toJson(MyEnum.V1) shouldEqual JsString("V1")
    Json.toJson(MyEnum.UNKNOWN) shouldEqual JsString("UNKNOWN")
  }

  it should "be converted back from json string" in {
    implicit val enumFmt: Format[MyEnum] = ScalaPBJson.enumFormat[MyEnum]

    Json.fromJson[MyEnum](JsString("V1")) shouldEqual JsSuccess(MyEnum.V1)
  }
}
