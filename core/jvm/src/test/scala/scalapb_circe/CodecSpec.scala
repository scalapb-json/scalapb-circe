package scalapb_circe

import jsontest.anytests.AnyTest
import jsontest.custom_collection.{Guitar, Studio}
import jsontest.issue315.{Bar, Foo, Msg}
import org.scalatest.Assertion
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}


class CodecSpec extends AnyFlatSpec with Matchers {

  "GeneratedMessage" should "encode to same value via codec and JsonFormat" in {
    import io.circe.syntax._
    import scalapb_circe.codec._

    def check[M <: GeneratedMessage](m: M): Assertion = {
      val encodedWithPrinter = JsonFormat.printer.toJson(m)
      val encodedWithCodec = m.asJson
      encodedWithCodec mustBe encodedWithPrinter
    }

    check(AnyTest("foo"))
    check(Guitar(99))
    check(Studio(Set(Guitar(1), Guitar(2), Guitar(3))))
    check(Msg(
      baz = "bazzz",
      someUnion = Msg.SomeUnion.Foo(Foo("fooooo"))
    ))
    check(Msg(
      baz = "bazzz",
      someUnion = Msg.SomeUnion.Bar(Bar("fooooo"))
    ))
  }

  "GeneratedMessage" should "decode to same value via codec and JsonFormat" in {
    import io.circe.syntax._
    import scalapb_circe.codec._

    def check[M <: GeneratedMessage : GeneratedMessageCompanion](m: M): Assertion = {
      val decodedWithParser = JsonFormat.fromJson(JsonFormat.printer.toJson(m))
      val decodedWithCodec = m.asJson.as[M]
      decodedWithCodec mustBe Right(decodedWithParser)
    }

    check(AnyTest("foo"))
    check(Guitar(99))
    check(Studio(Set(Guitar(1), Guitar(2), Guitar(3))))
    check(Msg(
      baz = "bazzz",
      someUnion = Msg.SomeUnion.Foo(Foo("fooooo"))
    ))
    check(Msg(
      baz = "bazzz",
      someUnion = Msg.SomeUnion.Bar(Bar("fooooo"))
    ))
  }

//  "GeneratedMessage" should "encode to same value via codec with implicit printer and printer" in {
//    ???
//  }
//
//  "GeneratedMessage" should "decode to same value via codec with implicit printer and parser" in {
//    ???
//  }
//
//  "GeneratedEnum" should "encode to same value via codec and printer" in {
//    ???
//  }
//
//  "GeneratedEnum" should "decode to same value via codec and parser" in {
//    ???
//  }
//
//  "GeneratedEnum" should "encode to same value via codec with implicit printer and printer" in {
//    ???
//  }
//
//  "GeneratedEnum" should "decode to same value via codec with implicit printer and parser" in {
//    ???
//  }
//
//  "Case class with GeneratedMessage and GeneratedEnum" should "encode and decode via codec" in {
//    ???
//  }

}
