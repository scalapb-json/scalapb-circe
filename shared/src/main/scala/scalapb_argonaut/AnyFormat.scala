package scalapb_argonaut

import com.google.protobuf.any.{Any => PBAny}
import argonaut._
import scala.language.existentials
import scalapb_json._

object AnyFormat {
  val anyWriter: (Printer, PBAny) => Json = {
    case (printer, any) =>
      // Find the companion so it can be used to JSON-serialize the message. Perhaps this can be circumvented by
      // including the original GeneratedMessage with the Any (at least in memory).
      val cmp = printer.typeRegistry
        .findType(any.typeUrl)
        .getOrElse(throw new IllegalStateException(
          s"Unknown type ${any.typeUrl}; you may have to register it via FormatRegistry.registerCompanion"))

      // Unpack the message...
      val message = any.unpack(cmp)

      // ... and add the @type marker to the resulting JSON
      printer.toJson(message).obj match {
        case Some(fields) =>
          Json.obj(("@type" -> Json.jString(any.typeUrl)) :: fields.toList: _*)
        case value =>
          // Safety net, this shouldn't happen
          throw new IllegalStateException(
            s"Message of type ${any.typeUrl} emitted non-object JSON: $value")
      }
  }

  val anyParser: (Parser, Json) => PBAny = { case (parser, json) =>
    json.obj match {
      case Some(obj) =>
        obj.toMap.get("@type").flatMap(_.string) match {
          case Some(typeUrl) =>
            val cmp = parser.typeRegistry
              .findType(typeUrl)
              .getOrElse(throw new JsonFormatException(s"""Unknown type: "$typeUrl""""))
            val message = parser.fromJson(json)(cmp)
            PBAny(typeUrl = typeUrl, value = message.toByteString)

          case unknown =>
            throw new JsonFormatException(s"Expected string @type field, got $unknown")
        }

      case _ =>
        throw new JsonFormatException(s"Expected an object, got $json")
    }
  }
}
