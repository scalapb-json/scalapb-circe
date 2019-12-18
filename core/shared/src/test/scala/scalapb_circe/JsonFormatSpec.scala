package scalapb_circe

import io.circe.Json
import io.circe.parser.parse
import org.scalatest.{Assertion, OptionValues}
import jsontest.test._
import jsontest.test3._
import com.google.protobuf.any.{Any => PBAny}
import com.google.protobuf.field_mask.FieldMask
import jsontest.custom_collection.{Guitar, Studio}
import scalapb_json._
import EitherOps._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class JsonFormatSpec extends AnyFlatSpec with Matchers with OptionValues {

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
    _.boolToString := Map(false -> "ff", true -> "tt"),
    _.optBool := false
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
      |  "boolToString": {"false": "ff", "true": "tt"},
      |  "optBool": false
      |}
      |""".stripMargin

  val TestJsonWithType =
    """{
      |  "@type": "type.googleapis.com/jsontest.MyTest",
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

  val DefaultTestJson =
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

  val PreservedTestJson =
    """{
      |  "hello": "Foo",
      |  "foobar": 37,
      |  "primitive_sequence": ["a", "b", "c"],
      |  "rep_message": [{}, {"hello": "h11"}],
      |  "opt_message": {"foobar": 39},
      |  "string_to_int32": {"foo": 14, "bar": 19},
      |  "int_to_mytest": {"14": {}, "35": {"hello": "boo"}},
      |  "rep_enum": ["V1", "V2", "UNKNOWN"],
      |  "opt_enum": "V2",
      |  "int_to_enum": {"32": "V1", "35": "V2"},
      |  "string_to_bool": {"ff": false, "tt": true},
      |  "bool_to_string": {"false": "ff", "true": "tt"},
      |  "opt_bool": false
      |}
      |""".stripMargin

  "Empty object" should "give empty json" in {
    JsonFormat.toJson(MyTest()) must be(Json.obj())
  }

  "Empty object" should "give empty json for MyTest3" in {
    JsonFormat.toJson(MyTest3()) must be(Json.obj())
  }

  "Zero maps" should "give correct json" in {
    JsonFormat.toJson(
      MyTest(
        stringToInt32 = Map("" -> 17),
        intToMytest = Map(0 -> MyTest()),
        fixed64ToBytes = Map(0L -> com.google.protobuf.ByteString.copyFromUtf8("foobar"))
      )
    ) must be(parse("""|{
                 |  "stringToInt32": {"": 17},
                 |  "intToMytest": {"0": {}},
                 |  "fixed64ToBytes": {"0": "Zm9vYmFy"}
                 |}""".stripMargin).getOrError)
  }

  "Zero maps" should "give correct json for MyTest3" in {
    JsonFormat.toJson(
      MyTest3(
        stringToInt32 = Map("" -> 17),
        intToMytest = Map(0 -> MyTest()),
        fixed64ToBytes = Map(0L -> com.google.protobuf.ByteString.copyFromUtf8("foobar"))
      )
    ) must be(parse("""|{
                 |  "stringToInt32": {"": 17},
                 |  "intToMytest": {"0": {}},
                 |  "fixed64ToBytes": {"0": "Zm9vYmFy"}
                 |}""".stripMargin).getOrError)
  }

  "Set treat" should "give correct json" in {
    JsonFormat.toJson(MyTest(trickOrTreat = MyTest.TrickOrTreat.Treat(MyTest()))) must be(
      parse("""{"treat": {}}""").getOrError
    )
  }

  "Parse treat" should "give correct proto with proto2" in {
    JsonFormat.fromJsonString[MyTest]("""{"treat": {"hello": "x"}}""") must be(
      MyTest(trickOrTreat = MyTest.TrickOrTreat.Treat(MyTest(hello = Some("x"))))
    )
    JsonFormat.fromJsonString[MyTest]("""{"treat": {}}""") must be(
      MyTest(trickOrTreat = MyTest.TrickOrTreat.Treat(MyTest()))
    )
  }

  "Parse treat" should "give correct proto with proto3" in {
    JsonFormat.fromJsonString[MyTest3]("""{"treat": {"s": "x"}}""") must be(
      MyTest3(trickOrTreat = MyTest3.TrickOrTreat.Treat(MyTest3(s = "x")))
    )
    JsonFormat.fromJsonString[MyTest3]("""{"treat": {}}""") must be(
      MyTest3(trickOrTreat = MyTest3.TrickOrTreat.Treat(MyTest3()))
    )
  }

  "JsonFormat" should "encode and decode enums for proto3" in {
    val v1Value = MyTest3(optEnum = MyTest3.MyEnum3.V1)
    assert(JsonFormat.toJson(v1Value) === parse("""{"optEnum": "V1"}""").getOrError)
    val defaultValue = MyTest3(optEnum = MyTest3.MyEnum3.UNKNOWN)
    assert(JsonFormat.toJson(defaultValue) === parse("""{}""").getOrError)
    JsonFormat.fromJsonString[MyTest3](JsonFormat.toJsonString(v1Value)) must be(v1Value)
    JsonFormat.fromJsonString[MyTest3](JsonFormat.toJsonString(defaultValue)) must be(defaultValue)
  }

  "parsing one offs" should "work correctly for issue 315" in {
    JsonFormat.fromJsonString[jsontest.issue315.Msg]("""
    {
          "baz" : "1",
          "foo" : {
            "cols" : "1"
          }
    }""") must be(
      jsontest.issue315
        .Msg(baz = "1", someUnion = jsontest.issue315.Msg.SomeUnion.Foo(jsontest.issue315.Foo(cols = "1")))
    )
  }

  "parsing null" should "give default value" in {
    JsonFormat.fromJsonString[jsontest.test.MyTest]("""
    {
          "optMessage" : null,
          "optBool": null,
          "optEnum": null,
          "repEnum": null
    }""") must be(jsontest.test.MyTest())
  }

  "TestProto" should "be TestJson when converted to Proto" in {
    JsonFormat.toJson(TestProto) must be(parse(TestJson).getOrError)
  }

  "TestJson" should "be TestProto when parsed from json" in {
    JsonFormat.fromJsonString[MyTest](TestJson) must be(TestProto)
  }

  "Empty object" should "give full json if including default values" in {
    new Printer(includingDefaultValueFields = true).toJson(MyTest()) must be(
      parse("""{
          |  "hello": "",
          |  "foobar": 0,
          |  "bazinga": "0",
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
          |  "fixed64ToBytes": {}
          |}""".stripMargin).getOrError
    )
  }

  "Empty object" should "with preserve field names should work" in {
    new Printer(includingDefaultValueFields = true, preservingProtoFieldNames = true).toJson(MyTest()) must be(
      parse("""{
          |  "hello": "",
          |  "foobar": 0,
          |  "bazinga": "0",
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
          |  "fixed64_to_bytes": {}
          |}""".stripMargin).getOrError
    )
  }

  "TestProto" should "format int64 as JSON string" in {
    new Printer().print(MyTest(bazinga = Some(642))) must be("""{"bazinga":"642"}""")
  }

  "TestProto" should "format int64 as JSON number" in {
    new Printer(formattingLongAsNumber = true).print(MyTest(bazinga = Some(642))) must be("""{"bazinga":642}""")
  }

  "TestProto" should "parse numbers formatted as JSON string" in {
    val parser = new Parser()
    def validateAccepts(json: String, expected: IntFields): Assertion = {
      parser.fromJsonString[IntFields](json) must be(expected)
    }
    def validateRejects(json: String): Assertion = {
      a[JsonFormatException] mustBe thrownBy { parser.fromJsonString[IntFields](json) }
    }

    // int32
    validateAccepts("""{"int":"642"}""", IntFields(int = Some(642)))
    validateAccepts("""{"int":"-1"}""", IntFields(int = Some(-1)))
    validateAccepts(s"""{"int":"${Integer.MAX_VALUE}"}""", IntFields(int = Some(Integer.MAX_VALUE)))
    validateAccepts(s"""{"int":"${Integer.MIN_VALUE}"}""", IntFields(int = Some(Integer.MIN_VALUE)))
    validateRejects(s"""{"int":"${Integer.MAX_VALUE.toLong + 1}"}""")
    validateRejects(s"""{"int":"${Integer.MIN_VALUE.toLong - 1}"}""")

    // int64
    validateAccepts("""{"long":"642"}""", IntFields(long = Some(642L)))
    validateAccepts("""{"long":"-1"}""", IntFields(long = Some(-1L)))
    validateAccepts(s"""{"long":"${Long.MaxValue}"}""", IntFields(long = Some(Long.MaxValue)))
    validateAccepts(s"""{"long":"${Long.MinValue}"}""", IntFields(long = Some(Long.MinValue)))
    validateRejects(s"""{"long":"${BigInt(Long.MaxValue) + 1}"}""")
    validateRejects(s"""{"long":"${BigInt(Long.MinValue) - 1}"}""")

    // uint32
    val uint32max: Long = (1L << 32) - 1
    validateAccepts(s"""{"uint":"$uint32max"}""", IntFields(uint = Some(uint32max.toInt)))
    validateRejects(s"""{"uint":"${uint32max + 1}"}""")
    validateRejects("""{"uint":"-1"}""")

    // uint64
    val uint64max: BigInt = (BigInt(1) << 64) - 1
    validateAccepts(s"""{"ulong":"$uint64max"}""", IntFields(ulong = Some(uint64max.toLong)))
    validateRejects(s"""{"ulong":"${uint64max + 1}"}""")
    validateRejects("""{"ulong":"-1"}""")

    // sint32
    validateAccepts(s"""{"sint":"${Integer.MAX_VALUE}"}""", IntFields(sint = Some(Integer.MAX_VALUE)))
    validateAccepts(s"""{"sint":"${Integer.MIN_VALUE}"}""", IntFields(sint = Some(Integer.MIN_VALUE)))
    validateRejects(s"""{"sint":"${Integer.MAX_VALUE.toLong + 1}"}""")
    validateRejects(s"""{"sint":"${Integer.MIN_VALUE.toLong - 1}"}""")

    // sint64
    validateAccepts(s"""{"slong":"${Long.MaxValue}"}""", IntFields(slong = Some(Long.MaxValue)))
    validateAccepts(s"""{"slong":"${Long.MinValue}"}""", IntFields(slong = Some(Long.MinValue)))
    validateRejects(s"""{"slong":"${BigInt(Long.MaxValue) + 1}"}""")
    validateRejects(s"""{"slong":"${BigInt(Long.MinValue) - 1}"}""")

    // fixed32
    validateAccepts(s"""{"fixint":"$uint32max"}""", IntFields(fixint = Some(uint32max.toInt)))
    validateRejects(s"""{"fixint":"${uint32max + 1}"}""")
    validateRejects("""{"fixint":"-1"}""")

    // fixed64
    validateAccepts(s"""{"fixlong":"$uint64max"}""", IntFields(fixlong = Some(uint64max.toLong)))
    validateRejects(s"""{"fixlong":"${uint64max + 1}"}""")
    validateRejects("""{"fixlong":"-1"}""")

  }

  "TestProto" should "produce valid JSON output for unsigned integers" in {
    val uint32max: Long = (1L << 32) - 1
    JsonFormat.toJson(IntFields(uint = Some(uint32max.toInt))) must be(parse(s"""{"uint":$uint32max}""").getOrError)
    JsonFormat.toJson(IntFields(uint = Some(1))) must be(parse(s"""{"uint":1}""").getOrError)
    JsonFormat.toJson(IntFields(fixint = Some(uint32max.toInt))) must be(parse(s"""{"fixint":$uint32max}""").getOrError)
    JsonFormat.toJson(IntFields(fixint = Some(1))) must be(parse(s"""{"fixint":1}""").getOrError)
    val uint64max: BigInt = (BigInt(1) << 64) - 1
    JsonFormat.toJson(IntFields(ulong = Some(uint64max.toLong))) must be(
      parse(s"""{"ulong":"$uint64max"}""").getOrError
    )
    JsonFormat.toJson(IntFields(ulong = Some(1))) must be(parse(s"""{"ulong":"1"}""").getOrError)
    JsonFormat.toJson(IntFields(fixlong = Some(uint64max.toLong))) must be(
      parse(s"""{"fixlong":"$uint64max"}""").getOrError
    )
    JsonFormat.toJson(IntFields(fixlong = Some(1))) must be(parse(s"""{"fixlong":"1"}""").getOrError)
  }

  "TestProto" should "parse an enum formatted as number" in {
    new Parser().fromJsonString[MyTest]("""{"optEnum":1}""") must be(MyTest(optEnum = Some(MyEnum.V1)))
    new Parser().fromJsonString[MyTest]("""{"optEnum":2}""") must be(MyTest(optEnum = Some(MyEnum.V2)))
  }

  "PreservedTestJson" should "be TestProto when parsed from json" in {
    new Parser(preservingProtoFieldNames = true).fromJsonString[MyTest](PreservedTestJson) must be(TestProto)
  }

  "DoubleFloatProto" should "parse NaNs" in {
    val i = s"""{
      "d": "NaN",
      "f": "NaN"
    }"""
    val out = JsonFormat.fromJsonString[DoubleFloat](i)
    out.d.value.isNaN must be(true)
    out.f.value.isNaN must be(true)
    JsonFormat.toJson(out).asObject.flatMap(_.apply("d")) must be(Some(Json.fromString(Double.NaN.toString)))
    JsonFormat.toJson(out).asObject.flatMap(_.apply("f")) must be(Some(Json.fromString(Double.NaN.toString)))
  }

  "DoubleFloatProto" should "parse Infinity" in {
    val i = s"""{
      "d": "Infinity",
      "f": "Infinity"
    }"""
    val out = JsonFormat.fromJsonString[DoubleFloat](i)
    out.d.value.isPosInfinity must be(true)
    out.f.value.isPosInfinity must be(true)
    JsonFormat.toJson(out).asObject.flatMap(_.apply("d")) must be(
      Some(Json.fromString(Double.PositiveInfinity.toString))
    )
    JsonFormat.toJson(out).asObject.flatMap(_.apply("f")) must be(
      Some(Json.fromString(Double.PositiveInfinity.toString))
    )
  }

  "DoubleFloatProto" should "parse -Infinity" in {
    val i = s"""{
      "d": "-Infinity",
      "f": "-Infinity"
    }"""
    val out = JsonFormat.fromJsonString[DoubleFloat](i)
    out.d.value.isNegInfinity must be(true)
    out.f.value.isNegInfinity must be(true)
    JsonFormat.toJson(out).asObject.flatMap(_.apply("d")) must be(
      Some(Json.fromString(Double.NegativeInfinity.toString))
    )
    JsonFormat.toJson(out).asObject.flatMap(_.apply("f")) must be(
      Some(Json.fromString(Double.NegativeInfinity.toString))
    )
  }

  val anyEnabledTypeRegistry = TypeRegistry.empty.addMessageByCompanion(TestProto.companion)
  val anyEnabledParser = new Parser(typeRegistry = anyEnabledTypeRegistry)
  val anyEnabledPrinter = new Printer(typeRegistry = anyEnabledTypeRegistry)

  "TestProto packed as any" should "give TestJsonWithType after JSON serialization" in {
    val any = PBAny.pack(TestProto)

    anyEnabledPrinter.toJson(any) must be(parse(TestJsonWithType).getOrError)
  }

  "TestJsonWithType" should "be TestProto packed as any when parsed from JSON" in {
    val out = anyEnabledParser.fromJson[PBAny](parse(TestJsonWithType).getOrError)
    out.unpack[MyTest] must be(TestProto)
  }

  "toJsonString" should "generate correct JSON for messages with custom collection type" in {
    val studio = Studio().addGuitars(Guitar(numberOfStrings = 12))
    val expectedStudioJsonString = """{"guitars":[{"numberOfStrings":12}]}"""
    val studioJsonString = JsonFormat.toJsonString(studio)
    studioJsonString must be(expectedStudioJsonString)
  }

  "fromJsonString" should "parse JSON correctly to message with custom collection type" in {
    val expectedStudio = Studio().addGuitars(Guitar(numberOfStrings = 12))
    val studioJsonString = """{"guitars":[{"numberOfStrings":12}]}"""
    import Studio.messageCompanion
    val studio = JsonFormat.fromJsonString(studioJsonString)
    studio must be(expectedStudio)
  }

  "formatEnumAsNumber" should "format enums as number" in {
    val p = MyTest().update(_.optEnum := MyEnum.V2)
    new Printer(formattingEnumsAsNumber = true).toJson(p) must be(parse(s"""{"optEnum":2}""").getOrError)
  }

  "FieldMask" should "parse and write" in {
    // https://github.com/google/protobuf/blob/47b7d2c7ca/java/util/src/test/java/com/google/protobuf/util/JsonFormatTest.java#L761-L770
    val message = TestFieldMask(Some(FieldMask(Seq("foo.bar", "baz", "foo_bar.baz"))))
    val json = """{"fieldMaskValue":"foo.bar,baz,fooBar.baz"}"""
    assert(JsonFormat.toJsonString(message) == json)
    assert(JsonFormat.fromJsonString[TestFieldMask](json) == message)
  }

  "booleans" should "be accepted as string" in {
    assert(
      JsonFormat.fromJsonString[MyTest]("""{"optBool": "true"}""") == MyTest(optBool = Some(true))
    )
    assert(
      JsonFormat.fromJsonString[MyTest]("""{"optBool": "false"}""") == MyTest(
        optBool = Some(false)
      )
    )
  }
}
