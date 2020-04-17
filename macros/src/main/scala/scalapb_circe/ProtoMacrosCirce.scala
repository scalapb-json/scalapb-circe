package scalapb_circe

import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

import scala.reflect.macros.blackbox
import language.experimental.macros
import scala.util.Try

object ProtoMacrosCirce {

  implicit class ProtoContextCirce(private val c: StringContext) extends AnyVal {
    def struct(): com.google.protobuf.struct.Struct =
      macro ProtoMacrosCirce.protoStructInterpolation
    def value(): com.google.protobuf.struct.Value =
      macro ProtoMacrosCirce.protoValueInterpolation
  }

  implicit class FromJsonCirce[A <: GeneratedMessage](
    private val companion: GeneratedMessageCompanion[A]
  ) extends AnyVal {
    def fromJsonConstant(json: String): A =
      macro ProtoMacrosCirce.fromJsonConstantImpl0[A]

    def fromJson(json: String): A =
      macro ProtoMacrosCirce.fromJsonImpl[A]

    def fromJsonDebug(json: String): A =
      macro ProtoMacrosCirce.fromJsonDebugImpl

    def fromJsonOpt(json: String): Option[A] =
      macro ProtoMacrosCirce.fromJsonOptImpl[A]

    def fromJsonEither(json: String): Either[Throwable, A] =
      macro ProtoMacrosCirce.fromJsonEitherImpl[A]

    def fromJsonTry(json: String): Try[A] =
      macro ProtoMacrosCirce.fromJsonTryImpl[A]
  }
}

class ProtoMacrosCirce(override val c: blackbox.Context) extends scalapb_json.ProtoMacrosCommon(c) {

  import c.universe._

  override def fromJsonImpl[A: c.WeakTypeTag](json: c.Tree): c.Tree = {
    val A = weakTypeTag[A]
    q"_root_.scalapb_circe.JsonFormat.fromJsonString[$A]($json)"
  }

  override def fromJsonConstantImpl[A <: GeneratedMessage: c.WeakTypeTag: GeneratedMessageCompanion](
    string: String
  ): c.Tree = {
    val A = weakTypeTag[A]
    scalapb_circe.JsonFormat.fromJsonString[A](string)
    q"_root_.scalapb_circe.JsonFormat.fromJsonString[$A]($string)"
  }

  private[this] def parseJson(json: String): io.circe.Json = {
    io.circe.parser.parse(json).fold(throw _, identity)
  }

  override protected[this] def protoString2Struct(string: String): c.Tree = {
    val json = parseJson(string)
    val struct = StructFormat.structParser(json)
    q"$struct"
  }

  override protected[this] def protoString2Value(string: String): c.Tree = {
    val json = parseJson(string)
    val value = StructFormat.structValueParser(json)
    q"$value"
  }
}
