package scalapb_playjson

import com.google.protobuf.util.JsonFormat.{TypeRegistry => JavaTypeRegistry}
import JsonFormat.GenericCompanion
import com.trueaccord.scalapb.{
  GeneratedMessage,
  GeneratedMessageCompanion,
  JavaProtoSupport,
  Message
}
import org.scalatest.MustMatchers

trait JavaAssertions {
  self: MustMatchers =>

  def registeredCompanions: Seq[GeneratedMessageCompanion[_]] = Seq.empty

  val JavaJsonTypeRegistry =
    registeredCompanions.foldLeft(JavaTypeRegistry.newBuilder())(_ add _.javaDescriptor).build()
  val JavaJsonPrinter =
    com.google.protobuf.util.JsonFormat.printer().usingTypeRegistry(JavaJsonTypeRegistry)
  val JavaJsonParser = com.google.protobuf.util.JsonFormat.parser()

  val ScalaTypeRegistry = registeredCompanions.foldLeft(TypeRegistry.empty)((r, c) =>
    r.addMessageByCompanion(c.asInstanceOf[GenericCompanion]))
  val ScalaJsonParser = new Parser(typeRegistry = ScalaTypeRegistry)
  val ScalaJsonPrinter = new Printer(typeRegistry = ScalaTypeRegistry)

  def assertJsonIsSameAsJava[T <: GeneratedMessage with Message[T]](v: T)(
    implicit cmp: GeneratedMessageCompanion[T]) = {
    val scalaJson = ScalaJsonPrinter.print(v)
    val javaJson = JavaJsonPrinter.print(
      cmp.asInstanceOf[JavaProtoSupport[T, com.google.protobuf.GeneratedMessageV3]].toJavaProto(v))

    import play.api.libs.json.Json.parse
    parse(scalaJson) must be(parse(javaJson))
    ScalaJsonParser.fromJsonString[T](scalaJson) must be(v)
  }

  def javaParse[T <: com.google.protobuf.GeneratedMessageV3.Builder[T]](
    json: String,
    b: com.google.protobuf.GeneratedMessageV3.Builder[T]) = {
    JavaJsonParser.merge(json, b)
    b.build()
  }
}
