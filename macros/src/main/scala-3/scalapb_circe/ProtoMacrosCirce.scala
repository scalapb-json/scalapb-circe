package scalapb_circe

import com.google.protobuf.struct.Struct
import com.google.protobuf.struct.Value
import io.circe.JsonObject
import scalapb_json.ProtoMacrosCommon._
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}
import scala.quoted.*
import scala.reflect.NameTransformer.MODULE_INSTANCE_NAME
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.control.NonFatal

object ProtoMacrosCirce {

  extension (inline s: StringContext) {
    inline def struct(): com.google.protobuf.struct.Struct =
      ${ structInterpolation('s) }
    inline def value(): com.google.protobuf.struct.Value =
      ${ valueInterpolation('s) }
  }

  extension [A <: GeneratedMessage](inline companion: GeneratedMessageCompanion[A]) {
    inline def fromJsonConstant(inline json: String): A =
      ${ ProtoMacrosCirce.fromJsonConstantImpl[A]('json, 'companion) }
  }

  extension [A <: GeneratedMessage](companion: GeneratedMessageCompanion[A]) {
    def fromJson(json: String): A =
      JsonFormat.fromJsonString[A](json)(companion)

    def fromJsonOpt(json: String): Option[A] =
      try {
        Some(fromJson(json))
      } catch {
        case NonFatal(_) =>
          None
      }

    def fromJsonEither(json: String): Either[Throwable, A] =
      try {
        Right(fromJson(json))
      } catch {
        case NonFatal(e) =>
          Left(e)
      }

    def fromJsonTry(json: String): Try[A] =
      try {
        Success(fromJson(json))
      } catch {
        case NonFatal(e) =>
          Failure(e)
      }
  }

  private[this] def structInterpolation(s: Expr[StringContext])(using quote: Quotes): Expr[Struct] = {
    import quote.reflect.report
    val Seq(str) = s.valueOrAbort.parts
    val json = io.circe.parser
      .parse(str)
      .flatMap(_.as[JsonObject])
      .left
      .map((e: io.circe.Error) => report.errorAndAbort(cats.Show[io.circe.Error].show(e)))
      .merge
    Expr(
      StructFormat.structParser(json)
    )
  }

  private[this] def valueInterpolation(s: Expr[StringContext])(using quote: Quotes): Expr[Value] = {
    import quote.reflect.report
    val Seq(str) = s.valueOrAbort.parts
    val json = io.circe.parser.parse(str).left.map(e => report.errorAndAbort(e.toString)).merge
    Expr(
      StructFormat.structValueParser(json)
    )
  }

  private[this] def fromJsonConstantImpl[A <: GeneratedMessage: Type](
    json: Expr[String],
    companion: Expr[GeneratedMessageCompanion[A]]
  )(using quote: Quotes): Expr[A] = {
    import quote.reflect.report
    val str = json.valueOrAbort
    val clazz = Class.forName(Type.show[A] + "$")
    implicit val c: GeneratedMessageCompanion[A] =
      clazz.getField(MODULE_INSTANCE_NAME).get(null).asInstanceOf[GeneratedMessageCompanion[A]]

    // check compile time
    JsonFormat.fromJsonString[A](str)

    '{
      JsonFormat.fromJsonString[A](${ Expr(str) })($companion)
    }
  }
}
