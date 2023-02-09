package scalapb_circe

import com.google.protobuf.util.JsonFormat.{printer => ProtobufJavaPrinter}
import jsontest.oneof_test.OneOf._
import jsontest.oneof_test.{OneOf, OneOfMessage}
import io.circe.parser.parse
import org.scalatest.prop._
import EitherOps._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class OneOfSpec extends AnyFlatSpec with Matchers with TableDrivenPropertyChecks {

  val examples = Table(
    ("message", "json"),
    (OneOf.defaultInstance, "{}"),
    (OneOf(Field.Empty), "{}"),
    (OneOf(Field.Primitive("")), """{"primitive":""}"""),
    (OneOf(Field.Primitive("test")), """{"primitive":"test"}"""),
    (OneOf(Field.Wrapper("")), """{"wrapper":""}"""),
    (OneOf(Field.Wrapper("test")), """{"wrapper":"test"}"""),
    (OneOf(Field.Message(OneOfMessage())), """{"message":{}}"""),
    (OneOf(Field.Message(OneOfMessage(Some("test")))), """{"message":{"field":"test"}}""")
  )

  forEvery(examples) { (message: OneOf, json: String) =>
    new Printer(includingDefaultValueFields = false).toJson(message) must be(parse(json).getOrError)
    new Printer(includingDefaultValueFields = false).toJson(message) must be(
      parse(
        ProtobufJavaPrinter().print(toJavaProto(message))
      ).getOrError
    )

    new Printer(includingDefaultValueFields = true).toJson(message) must be(parse(json).getOrError)
    new Printer(includingDefaultValueFields = true).toJson(message) must be(
      parse(
        ProtobufJavaPrinter().includingDefaultValueFields().print(toJavaProto(message))
      ).getOrError
    )
  }

}
