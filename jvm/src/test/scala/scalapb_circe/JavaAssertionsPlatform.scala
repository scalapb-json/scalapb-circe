package scalapb_circe

import com.google.protobuf.util.JsonFormat.{TypeRegistry => JavaTypeRegistry}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, JavaProtoSupport, Message}
import org.scalatest.MustMatchers

trait JavaAssertionsPlatform {
  self: MustMatchers with JavaAssertions =>

  def registeredCompanions: Seq[GeneratedMessageCompanion[_]]

  val JavaJsonTypeRegistry =
    registeredCompanions.foldLeft(JavaTypeRegistry.newBuilder())(_ add _.javaDescriptor).build()
  val JavaJsonPrinter =
    com.google.protobuf.util.JsonFormat.printer().usingTypeRegistry(JavaJsonTypeRegistry)
  val JavaJsonParser = com.google.protobuf.util.JsonFormat.parser()

  def assertJsonIsSameAsJava[T <: GeneratedMessage with Message[T]](v: T, checkRoundtrip: Boolean = true)(
    implicit cmp: GeneratedMessageCompanion[T]) = {
    val scalaJson = ScalaJsonPrinter.print(v)
    val javaJson = JavaJsonPrinter.print(
      cmp.asInstanceOf[JavaProtoSupport[T, com.google.protobuf.GeneratedMessageV3]].toJavaProto(v))

    import io.circe.parser.parse
    parse(scalaJson).isRight must be(true)
    parse(scalaJson) must be(parse(javaJson))
    if (checkRoundtrip) {
      ScalaJsonParser.fromJsonString[T](scalaJson) must be(v)
    }
  }

  def javaParse[T <: com.google.protobuf.GeneratedMessageV3.Builder[T]](
    json: String,
    b: com.google.protobuf.GeneratedMessageV3.Builder[T]) = {
    JavaJsonParser.merge(json, b)
    b.build()
  }
}
