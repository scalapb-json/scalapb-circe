package scalapb_circe

import com.google.protobuf.struct.Struct
import com.google.protobuf.struct.Value
import io.circe.JsonObject
import io.circe.JsonNumber
import io.circe.Json
import scalapb_json.ProtoMacrosCommon._
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}
import scala.quoted.*
import scala.reflect.NameTransformer.MODULE_INSTANCE_NAME
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.control.NonFatal

object ProtoMacrosCirce {
  implicit val toExprJsonNumber: ToExpr[JsonNumber] =
    new ToExpr[JsonNumber] {
      def apply(j: JsonNumber)(using Quotes) = '{
        JsonNumber.fromDecimalStringUnsafe(${ summon[ToExpr[String]].apply(j.toString) })
      }
    }

  implicit val toExprJsonObject: ToExpr[JsonObject] =
    new ToExpr[JsonObject] {
      def apply(j: JsonObject)(using Quotes) = '{
        JsonObject.fromIterable(${ summon[ToExpr[List[(String, Json)]]].apply(j.toList) })
      }
    }

  implicit val toExprJson: ToExpr[Json] =
    new ToExpr[Json] {
      def apply(j: Json)(using Quotes) = j.foldWith[Expr[Json]](new Json.Folder[Expr[Json]] {
        override def onNull = '{ Json.Null }
        override def onBoolean(value: Boolean) = {
          if (value) '{ Json.True }
          else '{ Json.False }
        }
        override def onNumber(value: JsonNumber) =
          '{ Json.fromJsonNumber(${ summon[ToExpr[JsonNumber]].apply(value) }) }
        override def onString(value: String) =
          '{ Json.fromString(${ summon[ToExpr[String]].apply(value) }) }
        override def onArray(value: Vector[Json]) =
          '{ Json.fromValues(${ summon[ToExpr[Seq[Json]]].apply(value) }) }
        override def onObject(value: JsonObject) =
          '{ Json.fromJsonObject(${ summon[ToExpr[JsonObject]].apply(value) }) }
      })
    }

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
    val c: GeneratedMessageCompanion[A] =
      clazz.getField(MODULE_INSTANCE_NAME).get(null).asInstanceOf[GeneratedMessageCompanion[A]]

    val jsonObj = io.circe.parser.parse(str).left.map(e => report.errorAndAbort(e.toString)).merge
    // check compile time
    JsonFormat.fromJson[A](jsonObj)(c)
    val expr = summon[ToExpr[Json]].apply(jsonObj)

    '{
      JsonFormat.fromJson[A]($expr)($companion)
    }
  }
}
