package com.whisk.scalapb.playjson

import org.scalatest.{FlatSpec, MustMatchers}
import jsontest.test._
import jsontest.test3._
import com.google.protobuf.util.{JsonFormat => JavaJsonFormat}
import play.api.libs.json.Json

class JsonFormatSpec extends FlatSpec with MustMatchers {
  private val TestProto = MyTest().update(
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
    _.boolToString := Map(false -> "ff", true -> "tt"),
    _.optBool := false
  )

  private val TestJson =
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
      |  "boolToString": {"false": "ff", "true": "tt"},
      |  "optBool": false
      |}
      |""".stripMargin

  private val DefaultTestJson =
    """{
      |  "hello": "",
      |  "foobar": 0,
      |  "bazinga": 0,
      |  "primitiveSequence": [],
      |  "repMessage": [],
      |  "stringToInt32": {},
      |  "intToMytest": {},
      |  "repEnum": [],
      |  "optEnum": "UNKNOWN",
      |  "intToEnum": {},
      |  "boolToString": {},
      |  "stringToBool": {},
      |  "optBool": false
      |}""".stripMargin

  "Empty object" should "give empty json" in {
    JsonFormat.toJson(MyTest()) must be (Json.obj())
  }

  "Empty object" should "give empty json for MyTest3" in {
    JsonFormat.toJson(MyTest3()) must be (Json.obj())
  }

  "Zero maps" should "give correct json" in {
    JsonFormat.toJson(MyTest(
      stringToInt32 = Map("" -> 17),
      intToMytest = Map(0 -> MyTest()),
      fixed64ToBytes = Map(0L -> com.google.protobuf.ByteString.copyFromUtf8("foobar")))) must be (
      Json.parse("""|{
               |  "stringToInt32": {"": 17},
               |  "intToMytest": {"0": {}},
               |  "fixed64ToBytes": {"0": "Zm9vYmFy"}
               |}""".stripMargin))
  }

  "Zero maps" should "give correct json for MyTest3" in {
    JsonFormat.toJson(MyTest3(
      stringToInt32 = Map("" -> 17),
      intToMytest = Map(0 -> MyTest()),
      fixed64ToBytes = Map(0L -> com.google.protobuf.ByteString.copyFromUtf8("foobar")))) must be (
      Json.parse("""|{
               |  "stringToInt32": {"": 17},
               |  "intToMytest": {"0": {}},
               |  "fixed64ToBytes": {"0": "Zm9vYmFy"}
               |}""".stripMargin))
  }

  "Set treat" should "give correct json" in {
    JsonFormat.toJson(MyTest(trickOrTreat = MyTest.TrickOrTreat.Treat(MyTest()))) must be (
      Json.parse("""{"treat": {}}"""))
  }

  "TestProto" should "be TestJson when converted to Proto" in {
    JsonFormat.toJson(TestProto) must be (Json.parse(TestJson))
  }

  "TestJson" should "be TestProto when parsed from json" in {
    JsonFormat.fromJsonString[MyTest](TestJson) must be (TestProto)
  }

  "fromJsonString" should "read json produced by Java" in {
    val javaJson = JavaJsonFormat.printer().print(MyTest.toJavaProto(TestProto))
    JsonFormat.fromJsonString[MyTest](javaJson) must be (TestProto)
  }

  "Java parser" should "read json strings produced by us" in {
    val b = jsontest.Test.MyTest.newBuilder
    val javaTestProto = JavaJsonFormat.parser().merge(JsonFormat.toJsonString(TestProto), b)
    TestProto must be(MyTest.fromJavaProto(b.build))
  }

  "Empty object" should "give full json if including default values" in {
    new Printer(includingDefaultValueFields = true).toJson(MyTest()) must be(
      Json.parse(
        """{
          |  "hello": "",
          |  "foobar": 0,
          |  "bazinga": 0,
          |  "primitiveSequence": [],
          |  "repMessage": [],
          |  "stringToInt32": {},
          |  "intToMytest": {},
          |  "repEnum": [],
          |  "optEnum": "UNKNOWN",
          |  "intToEnum": {},
          |  "boolToString": {},
          |  "stringToBool": {},
          |  "optBs": "",
          |  "optBool": false,
          |  "trick": 0,
          |  "fixed64ToBytes": {}
          |}""".stripMargin)
    )
  }

  "Empty object" should "with preserve field names should work" in {
    new Printer(includingDefaultValueFields = true, preservingProtoFieldNames = true).toJson(MyTest()) must be(
      Json.parse(
        """{
          |  "hello": "",
          |  "foobar": 0,
          |  "bazinga": 0,
          |  "primitive_sequence": [],
          |  "rep_message": [],
          |  "string_to_int32": {},
          |  "int_to_mytest": {},
          |  "rep_enum": [],
          |  "opt_enum": "UNKNOWN",
          |  "int_to_enum": {},
          |  "bool_to_string": {},
          |  "string_to_bool": {},
          |  "opt_bs": "",
          |  "opt_bool": false,
          |  "trick": 0,
          |  "fixed64_to_bytes": {}
          |}""".stripMargin)
    )
  }

  "TestProto" should "format int64 as JSON number" in {
    new Printer().print(MyTest(bazinga = Some(642))) must be("""{"bazinga":642}""")
  }

  "TestProto" should "parse a number formatted as JSON string " in {
    new Parser().fromJsonString[MyTest]("""{"bazinga":642}""") must be(MyTest(bazinga = Some(642)))
  }
}