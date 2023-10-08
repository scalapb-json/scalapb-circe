package scalapb_circe

import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import jsontest.anytests.AnyTest
import jsontest.custom_collection.{Guitar, Studio}
import jsontest.issue315.{Bar, Foo, Msg}
import jsontest.test.MyEnum
import jsontest.test3.MyTest3.MyEnum3
import org.scalatest.Assertion
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import scalapb.{GeneratedEnum, GeneratedEnumCompanion, GeneratedMessage, GeneratedMessageCompanion}
import scalapb_circe.codec._

class CodecSpec extends AnyFreeSpec with Matchers {

  "GeneratedMessage" - {
    "encode to same value via codec and JsonFormat" in {
      def check[M <: GeneratedMessage](m: M): Assertion = {
        val encodedWithJsonFormat = JsonFormat.toJson(m)
        val encodedImplicitly = m.asJson
        encodedImplicitly mustBe encodedWithJsonFormat
      }

      check(AnyTest("foo"))
      check(Guitar(99))
      check(Studio(Set(Guitar(1), Guitar(2), Guitar(3))))
      check(
        Msg(
          baz = "bazzz",
          someUnion = Msg.SomeUnion.Foo(Foo("fooooo"))
        )
      )
      check(
        Msg(
          baz = "bazzz",
          someUnion = Msg.SomeUnion.Bar(Bar("fooooo"))
        )
      )
    }

    "decode to same value via codec and JsonFormat" in {
      def check[M <: GeneratedMessage: GeneratedMessageCompanion](m: M): Assertion = {
        val decodedWithJsonFormat = JsonFormat.fromJson(JsonFormat.printer.toJson(m))
        val decodedImplicitly = m.asJson.as[M]
        decodedImplicitly mustBe Right(decodedWithJsonFormat)
      }

      check(AnyTest("foo"))
      check(Guitar(99))
      check(Studio(Set(Guitar(1), Guitar(2), Guitar(3))))
      check(
        Msg(
          baz = "bazzz",
          someUnion = Msg.SomeUnion.Foo(Foo("fooooo"))
        )
      )
      check(
        Msg(
          baz = "bazzz",
          someUnion = Msg.SomeUnion.Bar(Bar("fooooo"))
        )
      )
    }

    "encode using an implicit printer w/ non-standard settings" in {
      implicit val printer: Printer = new Printer(includingDefaultValueFields = true)

      // Create a proto with a default value (0).
      val g = Guitar(0)

      // Using regular JsonFormat yields an empty object because 0 is the default value.
      JsonFormat.toJson(g) mustBe Json.obj()

      // Using asJson with an implicit printer includes the default value.
      g.asJson mustBe Json.obj("numberOfStrings" -> Json.fromInt(0))
    }

    "decode using an implicit parser w/ non-standard settings" in {
      implicit val parser: Parser = new Parser(preservingProtoFieldNames = true)

      // Use the snake-case naming to define a Guitar Json object.
      val j = Json.obj("number_of_strings" -> Json.fromInt(42))

      // Using the regular JsonFormat parser decodes to the defaultInstance.
      JsonFormat.fromJson[Guitar](j) mustBe Guitar.defaultInstance

      // Using as[T] with an implicit parser decodes back to the original value (42).
      j.as[Guitar] mustBe Right(Guitar(42))
    }
  }

  "GeneratedEnum" - {
    "encode to same value via codec and JsonFormat" in {
      def check[E <: GeneratedEnum: GeneratedEnumCompanion](e: E): Assertion = {
        val encodedWithJsonFormat = JsonFormat.printer.serializeEnum(e.scalaValueDescriptor)
        val encodeImplicitly = e.asJson
        encodeImplicitly mustBe encodedWithJsonFormat
      }
      MyEnum.values.foreach(check(_: MyEnum))
      MyEnum3.values.foreach(check(_: MyEnum3))

      MyEnum.V1.asJson.as[MyEnum] mustBe Right(MyEnum.V1)
      MyEnum.V2.asJson.as[MyEnum] mustBe Right(MyEnum.V2)
      MyEnum.V3.asJson.as[MyEnum] mustBe Right(MyEnum.V3)

      MyEnum3.V1.asJson.as[MyEnum3] mustBe Right(MyEnum3.V1)
      MyEnum3.V2.asJson.as[MyEnum3] mustBe Right(MyEnum3.V2)
      MyEnum3.V3.asJson.as[MyEnum3] mustBe Right(MyEnum3.V3)
    }

    "decode to same value via codec and JsonFormat" in {
      def check[E <: GeneratedEnum: GeneratedEnumCompanion](e: E): Assertion = {
        val cmp = implicitly[GeneratedEnumCompanion[E]]
        val decodedWithJsonFormat = cmp.fromValue(
          JsonFormat.parser
            .defaultEnumParser(cmp.scalaDescriptor, JsonFormat.printer.serializeEnum(e.scalaValueDescriptor))
            .number
        )
        val decodedImplicitly = e.asJson.as[E]
        decodedImplicitly mustBe Right(decodedWithJsonFormat)
      }
      MyEnum.values.foreach(check(_: MyEnum))
      MyEnum3.values.foreach(check(_: MyEnum3))
    }

    "encode using an implicit printer w/ non-standard settings" in {
      implicit val printer = new Printer(formattingEnumsAsNumber = true)
      def check[E <: GeneratedEnum: GeneratedEnumCompanion](e: E): Assertion = {
        e.asJson mustBe Json.fromInt(e.value)
      }
      MyEnum.values.foreach(check(_: MyEnum))
      MyEnum3.values.foreach(check(_: MyEnum3))
    }

  }
  "Case class with GeneratedMessage and GeneratedEnum" - {
    "derive and use a semi-auto codec" in {

      import io.circe.generic.semiauto._
      import io.circe.syntax._

      case class Band(version: MyEnum, guitars: Seq[Guitar])

      object Band {
        implicit val dec: Decoder[Band] = deriveDecoder[Band]
        implicit val enc: Encoder[Band] = deriveEncoder[Band]
      }

      val band = Band(MyEnum.V1, Seq(Guitar(4), Guitar(5)))
      val json = Json.obj(
        "version" -> Json.fromString("V1"),
        "guitars" -> Json.arr(
          Json.obj("numberOfStrings" -> Json.fromInt(4)),
          Json.obj("numberOfStrings" -> Json.fromInt(5))
        )
      )
      band.asJson mustBe json
      json.as[Band] mustBe Right(band)
    }
  }

}
