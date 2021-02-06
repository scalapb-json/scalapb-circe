package scalapb_circe

import jsontest.test3.MyTest3.MyEnum3

object Playground extends App {

  import io.circe._
  import io.circe.syntax._
  import scalapb_circe.codec._

  case class Thing(foo: String, bar: Int)

  implicit val encodeFoo: Encoder[Thing] = new Encoder[Thing] {
    final def apply(a: Thing): Json = Json.obj(
      ("foo", Json.fromString(a.foo)),
      ("bar", Json.fromInt(a.bar))
    )
  }

  implicit val decodeFoo: Decoder[Thing] = new Decoder[Thing] {
    final def apply(c: HCursor): Decoder.Result[Thing] =
      for {
        foo <- c.downField("foo").as[String]
        bar <- c.downField("bar").as[Int]
      } yield {
        new Thing(foo, bar)
      }
  }

  val t = Thing("foo", 99)
  println(t.asJson.noSpaces)

  println(MyEnum3.UNKNOWN.asJson)

}
