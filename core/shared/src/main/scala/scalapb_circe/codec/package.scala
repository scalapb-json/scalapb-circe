package scalapb_circe

import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}
import scalapb.{GeneratedEnum, GeneratedEnumCompanion, GeneratedMessage, GeneratedMessageCompanion}

import scala.util.{Failure, Success, Try}

/**
 * Implicit circe [[Encoder]] and [[Decoder]]s for scalapb's [[GeneratedMessage]] and [[GeneratedEnum]] classes.
 */
package object codec {

  /**
   * Encoder for [[GeneratedMessage]] using a specific implicit [[Printer]].
   * The [[Printer]] class lets you control some details about the encoding,
   * such as whether to include fields with default values in the JSON.
   */
  implicit def generatedMessageEncoderWithPrinter[M <: GeneratedMessage](implicit p: Printer): Encoder[M] =
    (a: M) => p.toJson(a)

  implicit def generatedMessageEncoder[M <: GeneratedMessage]: Encoder[M] = {
    implicit val printer: Printer = JsonFormat.printer
    generatedMessageEncoderWithPrinter
  }

  /**
   * Decoder for [[GeneratedMessage]] using a specific implicit [[Parser]].
   * The [[Parser]] class lets you control some details about the decoding,
   * such as whether to preserve the raw field names.
   */
  implicit def generatedMessageDecoderWithParser[M <: GeneratedMessage: GeneratedMessageCompanion](implicit p: Parser): Decoder[M] =
    (c: HCursor) =>
      Try(p.fromJson[M](c.value)) match {
        case Failure(t) => Left(DecodingFailure(t.getMessage, List.empty))
        case Success(m) => Right(m)
      }

  implicit def generatedMessageDecoder[
    M <: GeneratedMessage: GeneratedMessageCompanion]: Decoder[M] = {
    implicit val parser: Parser = JsonFormat.parser
    generatedMessageDecoderWithParser
  }

  /**
   * Encoder for [[GeneratedEnum]] using a specific implicit [[Printer]].
   * The [[Printer]] class lets you control some details about the encoding,
   * such as whether to use strings or integers to represent an Enum.
   */
  implicit def generatedEnumEncoderWithPrinter[E <: GeneratedEnum](implicit p: Printer): Encoder[E] =
    (a: E) => p.serializeEnum(a.scalaValueDescriptor)

  implicit def generatedEnumEncoder[E <: GeneratedEnum]: Encoder[E] = {
    implicit val printer: Printer = JsonFormat.printer
    generatedEnumEncoderWithPrinter
  }

  /**
   * Decoder for [[GeneratedEnum]] using a specific implicit [[Parser]].
   * The [[Parser]] class lets you control some details about the decoding.
   */
  implicit def generatedEnumDecoderWithParser[E <: GeneratedEnum : GeneratedEnumCompanion](implicit p: Parser): Decoder[E] =
    (c: HCursor) => {
      val companion = implicitly[GeneratedEnumCompanion[E]]
      Try(p.defaultEnumParser(companion.scalaDescriptor, c.value)) match {
        case Success(e) => Right(companion.fromValue(e.index))
        case Failure(t) => Left(DecodingFailure(t.getMessage, List.empty))
      }
    }

  implicit def generatedEnumDecoder[E <: GeneratedEnum : GeneratedEnumCompanion]: Decoder[E] = {
    implicit val parser: Parser = JsonFormat.parser
    generatedEnumDecoderWithParser
  }



}
