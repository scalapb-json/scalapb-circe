package scalapb_circe

import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

import scala.util.{Failure, Success, Try}

package object codec {

  implicit def generatedMessageEncoder[M <: GeneratedMessage]: Encoder[M] =
    (a: M) => JsonFormat.toJson(a)

  implicit def generatedMessageDecoder[
      M <: GeneratedMessage: GeneratedMessageCompanion]: Decoder[M] = {
    implicit val parser: Parser = JsonFormat.parser
    generatedMessageDecoderWithParser[M]
  }

  /**
    * Encoder for [[GeneratedMessage]] using a specific implicit [[Printer]].
    * The [[Printer]] class lets you control some details about the encoding,
    * such as whether to include fields with default values in the JSON.
    */
  implicit def generatedMessageEncoderWithPrinter[M <: GeneratedMessage](
      implicit p: Printer): Encoder[M] =
    (a: M) => p.toJson(a)

  /**
    * Decoder for [[GeneratedMessage]] using a specific implicit [[Parser]].
    * The [[Parser]] class lets you control some details about the decoding,
    * such as whether to preserve the raw field names.
    */
  implicit def generatedMessageDecoderWithParser[
      M <: GeneratedMessage: GeneratedMessageCompanion](
      implicit p: Parser): Decoder[M] =
    (c: HCursor) =>
      Try(p.fromJson[M](c.value)) match {
        case Failure(t) => Left(DecodingFailure(t.getMessage, List.empty))
        case Success(m) => Right(m)
    }

}
