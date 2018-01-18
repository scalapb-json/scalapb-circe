package scalapb_circe

import com.google.protobuf.any.{Any => PBAny}
import jsontest.anytests.{AnyTest, ManyAnyTest}
import org.scalatest.{FlatSpec, MustMatchers}
import io.circe.parser.parse
import scalapb_json._
import EitherOps._

class AnyFormatSpec extends FlatSpec with MustMatchers with JavaAssertions {
  val RawExample = AnyTest("test")

  val RawJson = parse(s"""{"field":"test"}""").getOrError

  val AnyExample = PBAny.pack(RawExample)

  val AnyJson = parse(s"""{"@type":"type.googleapis.com/jsontest.AnyTest","field":"test"}""").getOrError

  val CustomPrefixAny = PBAny.pack(RawExample, "example.com/")

  val CustomPrefixJson = parse(s"""{"@type":"example.com/jsontest.AnyTest","field":"test"}""").getOrError

  val ManyExample = ManyAnyTest(
    Seq(
      PBAny.pack(AnyTest("1")),
      PBAny.pack(AnyTest("2"))
    ))

  val ManyPackedJson = parse("""
      |{
      |  "@type": "type.googleapis.com/jsontest.ManyAnyTest",
      |  "fields": [
      |    {"@type": "type.googleapis.com/jsontest.AnyTest", "field": "1"},
      |    {"@type": "type.googleapis.com/jsontest.AnyTest", "field": "2"}
      |  ]
      |}
    """.stripMargin).getOrError

  override def registeredCompanions = Seq(AnyTest, ManyAnyTest)

  // For clarity
  def UnregisteredPrinter = JsonFormat.printer

  def UnregisteredParser = JsonFormat.parser

  "Any" should "fail to serialize if its respective companion is not registered" in {
    an[IllegalStateException] must be thrownBy UnregisteredPrinter.toJson(AnyExample)
  }

  "Any" should "fail to deserialize if its respective companion is not registered" in {
    a[JsonFormatException] must be thrownBy UnregisteredParser.fromJson[PBAny](AnyJson)
  }

  "Any" should "serialize correctly if its respective companion is registered" in {
    ScalaJsonPrinter.toJson(AnyExample) must be(AnyJson)
  }

  "Any" should "fail to serialize with a custom URL prefix if specified" in {
    an[IllegalStateException] must be thrownBy ScalaJsonPrinter.toJson(CustomPrefixAny)
  }

  "Any" should "fail to deserialize for a non-Google-prefixed type URL" in {
    a[JsonFormatException] must be thrownBy ScalaJsonParser.fromJson[PBAny](CustomPrefixJson)
  }

  "Any" should "deserialize correctly if its respective companion is registered" in {
    ScalaJsonParser.fromJson[PBAny](AnyJson) must be(AnyExample)
  }

  "Any" should "resolve printers recursively" in {
    val packed = PBAny.pack(ManyExample)
    ScalaJsonPrinter.toJson(packed) must be(ManyPackedJson)
  }

  "Any" should "resolve parsers recursively" in {
    ScalaJsonParser.fromJson[PBAny](ManyPackedJson).unpack[ManyAnyTest] must be(ManyExample)
  }
}
