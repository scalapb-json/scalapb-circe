package scalapb_argonaut

import com.google.protobuf.ByteString
import com.google.protobuf.descriptor.FieldDescriptorProto
import com.google.protobuf.duration.Duration
import com.google.protobuf.struct.NullValue
import com.google.protobuf.timestamp.Timestamp
import com.trueaccord.scalapb._
import argonaut._
import scalapb_json._

import scala.collection.mutable
import scala.language.existentials
import scala.reflect.ClassTag
import _root_.scalapb.descriptors._
import scalapb_json.ScalapbJsonCommon.GenericCompanion
import scala.util.control.NonFatal

case class Formatter[T](writer: (Printer, T) => Json, parser: (Parser, Json) => T)

case class FormatRegistry(
  messageFormatters: Map[Class[_], Formatter[_]] = Map.empty,
  enumFormatters: Map[EnumDescriptor, Formatter[EnumValueDescriptor]] = Map.empty,
  registeredCompanions: Seq[GenericCompanion] = Seq.empty) {

  def registerMessageFormatter[T <: GeneratedMessage](
    writer: (Printer, T) => Json,
    parser: (Parser, Json) => T)(implicit ct: ClassTag[T]): FormatRegistry = {
    copy(messageFormatters = messageFormatters + (ct.runtimeClass -> Formatter(writer, parser)))
  }

  def registerEnumFormatter[E <: GeneratedEnum](
    writer: (Printer, EnumValueDescriptor) => Json,
    parser: (Parser, Json) => EnumValueDescriptor)(
    implicit cmp: GeneratedEnumCompanion[E]): FormatRegistry = {
    copy(enumFormatters = enumFormatters + (cmp.scalaDescriptor -> Formatter(writer, parser)))
  }

  def registerWriter[T <: GeneratedMessage: ClassTag](
    writer: T => Json,
    parser: Json => T): FormatRegistry = {
    registerMessageFormatter((p: Printer, t: T) => writer(t), (p: Parser, v: Json) => parser(v))
  }

  def getMessageWriter[T](klass: Class[_ <: T]): Option[(Printer, T) => Json] = {
    messageFormatters.get(klass).asInstanceOf[Option[Formatter[T]]].map(_.writer)
  }

  def getMessageParser[T](klass: Class[_ <: T]): Option[(Parser, Json) => T] = {
    messageFormatters.get(klass).asInstanceOf[Option[Formatter[T]]].map(_.parser)
  }

  def getEnumWriter(
    descriptor: EnumDescriptor): Option[(Printer, EnumValueDescriptor) => Json] = {
    enumFormatters.get(descriptor).map(_.writer)
  }

  def getEnumParser(
    descriptor: EnumDescriptor): Option[(Parser, Json) => EnumValueDescriptor] = {
    enumFormatters.get(descriptor).map(_.parser)
  }
}

class Printer(
  includingDefaultValueFields: Boolean = false,
  preservingProtoFieldNames: Boolean = false,
  formattingLongAsNumber: Boolean = false,
  formatRegistry: FormatRegistry = JsonFormat.DefaultRegistry,
  val typeRegistry: TypeRegistry = TypeRegistry.empty) {
  def print[A](m: GeneratedMessage): String = {
    toJson(m).toString()
  }

  private[this] type JField = (String, Json)
  private[this] type FieldBuilder = mutable.Builder[JField, List[JField]]

  private[this] def JField(key: String, value: Json) = (key, value)

  private def serializeMessageField(
    fd: FieldDescriptor,
    name: String,
    value: Any,
    b: FieldBuilder): Unit = {
    value match {
      case null =>
      // We are never printing empty optional messages to prevent infinite recursion.
      case Nil =>
        if (includingDefaultValueFields) {
          b += ((name, if (fd.isMapField) Json.obj() else Json.jEmptyArray))
        }
      case xs: Iterable[GeneratedMessage] @unchecked =>
        if (fd.isMapField) {
          val mapEntryDescriptor = fd.scalaType.asInstanceOf[ScalaType.Message].descriptor
          val keyDescriptor = mapEntryDescriptor.findFieldByNumber(1).get
          val valueDescriptor = mapEntryDescriptor.findFieldByNumber(2).get
          b += JField(
            name,
            Json.obj(xs.map { x =>
              val key = x.getField(keyDescriptor) match {
                case PBoolean(v) => v.toString
                case PDouble(v) => v.toString
                case PFloat(v) => v.toString
                case PInt(v) => v.toString
                case PLong(v) => v.toString
                case PString(v) => v
                case v => throw new JsonFormatException(s"Unexpected value for key: $v")
              }
              val value = if (valueDescriptor.protoType.isTypeMessage) {
                toJson(x.getFieldByNumber(valueDescriptor.number).asInstanceOf[GeneratedMessage])
              } else {
                serializeSingleValue(
                  valueDescriptor,
                  x.getField(valueDescriptor),
                  formattingLongAsNumber)
              }
              key -> value
            }.toList: _*)
          )
        } else {
          b += JField(name, Json.jArray(xs.map(toJson)(collection.breakOut)))
        }
      case msg: GeneratedMessage =>
        b += JField(name, toJson(msg))
      case v =>
        throw new JsonFormatException(v.toString)
    }
  }

  private def serializeNonMessageField(
    fd: FieldDescriptor,
    name: String,
    value: PValue,
    b: FieldBuilder) = {
    value match {
      case PEmpty =>
        if (includingDefaultValueFields) {
          b += JField(name, defaultJson(fd))
        }
      case PRepeated(xs) =>
        if (xs.nonEmpty || includingDefaultValueFields) {
          b += JField(
            name,
            Json.array(
              xs.map(serializeSingleValue(fd, _, formattingLongAsNumber)): _*
            )
          )
        }
      case v =>
        if (includingDefaultValueFields ||
          !fd.isOptional ||
          !fd.file.isProto3 ||
          (v != scalapb_json.ScalapbJsonCommon.defaultValue(fd))) {
          b += JField(name, serializeSingleValue(fd, v, formattingLongAsNumber))
        }
    }
  }

  def toJson[A <: GeneratedMessage](m: A): Json = {
    formatRegistry.getMessageWriter[A](m.getClass) match {
      case Some(f) => f(this, m)
      case None =>
        val b = List.newBuilder[JField]
        val descriptor = m.companion.scalaDescriptor
        b.sizeHint(descriptor.fields.size)
        descriptor.fields.foreach { f =>
          val name = if (preservingProtoFieldNames) f.name else scalapb_json.ScalapbJsonCommon.jsonName(f)
          if (f.protoType.isTypeMessage) {
            serializeMessageField(f, name, m.getFieldByNumber(f.number), b)
          } else {
            serializeNonMessageField(f, name, m.getField(f), b)
          }
        }
        Json.obj(b.result(): _*)
    }
  }


  private def defaultJson(fd: FieldDescriptor): Json =
    serializeSingleValue(fd, scalapb_json.ScalapbJsonCommon.defaultValue(fd), formattingLongAsNumber)

  private def unsignedLong(n: Long) =
    if (n < 0) BigDecimal(BigInt(n & 0x7FFFFFFFFFFFFFFFL).setBit(63)) else BigDecimal(n)

  private def formatLong(
    n: Long,
    protoType: FieldDescriptorProto.Type,
    formattingLongAsNumber: Boolean): Json = {
    val v =
      if (protoType.isTypeUint64 || protoType.isTypeFixed64) unsignedLong(n) else BigDecimal(n)
    if (formattingLongAsNumber) Json.jNumber(v) else Json.jString(v.toString())
  }

  def serializeSingleValue(
    fd: FieldDescriptor,
    value: PValue,
    formattingLongAsNumber: Boolean): Json = value match {
    case PEnum(e) =>
      formatRegistry.getEnumWriter(e.containingEnum) match {
        case Some(writer) => writer(this, e)
        case None => Json.jString(e.name)
      }
    case PInt(v) if fd.protoType.isTypeUint32 => Json.jNumber(ScalapbJsonCommon.unsignedInt(v))
    case PInt(v) if fd.protoType.isTypeFixed32 => Json.jNumber(ScalapbJsonCommon.unsignedInt(v))
    case PInt(v) => Json.jNumber(v)
    case PLong(v) => formatLong(v, fd.protoType, formattingLongAsNumber)
    case PDouble(v) => Json.jNumber(v)
    case PFloat(v) => Json.jNumber(v)
    case PBoolean(v) => Json.jBool(v)
    case PString(v) => Json.jString(v)
    case PByteString(v) => Json.jString(java.util.Base64.getEncoder.encodeToString(v.toByteArray))
    case _: PMessage | PRepeated(_) | PEmpty => throw new RuntimeException("Should not happen")
  }
}

class Parser(
  preservingProtoFieldNames: Boolean = false,
  formatRegistry: FormatRegistry = JsonFormat.DefaultRegistry,
  val typeRegistry: TypeRegistry = TypeRegistry.empty) {

  def fromJsonString[A <: GeneratedMessage with Message[A]](str: String)(
    implicit cmp: GeneratedMessageCompanion[A]): A = {
    fromJson(JsonParser.parse(str).fold(sys.error(_), identity))
  }

  def fromJson[A <: GeneratedMessage with Message[A]](value: Json)(
    implicit cmp: GeneratedMessageCompanion[A]): A = {
    cmp.messageReads.read(fromJsonToPMessage(cmp, value))
  }

  private def serializedName(fd: FieldDescriptor): String = {
    if (preservingProtoFieldNames) fd.asProto.getName else scalapb_json.ScalapbJsonCommon.jsonName(fd)
  }

  private def fromJsonToPMessage(cmp: GeneratedMessageCompanion[_], value: Json): PMessage = {

    def parseValue(fd: FieldDescriptor, value: Json): PValue = {
      if (fd.isMapField) {
        value.obj match {
          case Some(vals) =>
            val mapEntryDesc = fd.scalaType.asInstanceOf[ScalaType.Message].descriptor
            val keyDescriptor = mapEntryDesc.findFieldByNumber(1).get
            val valueDescriptor = mapEntryDesc.findFieldByNumber(2).get
            PRepeated(vals.toList.map {
              case (key, jValue) =>
                val keyObj = keyDescriptor.scalaType match {
                  case ScalaType.Boolean => PBoolean(java.lang.Boolean.valueOf(key))
                  case ScalaType.Double => PDouble(java.lang.Double.valueOf(key))
                  case ScalaType.Float => PFloat(java.lang.Float.valueOf(key))
                  case ScalaType.Int => PInt(java.lang.Integer.valueOf(key))
                  case ScalaType.Long => PLong(java.lang.Long.valueOf(key))
                  case ScalaType.String => PString(key)
                  case _ => throw new RuntimeException(s"Unsupported type for key for ${fd.name}")
                }
                PMessage(
                  Map(
                    keyDescriptor -> keyObj,
                    valueDescriptor -> parseSingleValue(
                      cmp.messageCompanionForFieldNumber(fd.number),
                      valueDescriptor,
                      jValue)))
            }(scala.collection.breakOut))
          case _ =>
            throw new JsonFormatException(
              s"Expected an object for map field ${serializedName(fd)} of ${fd.containingMessage.name}")
        }
      } else if (fd.isRepeated) {
        value.array match {
          case Some(vals) =>
            PRepeated(vals.map(parseSingleValue(cmp, fd, _)).toVector)
          case _ =>
            throw new JsonFormatException(
              s"Expected an array for repeated field ${serializedName(fd)} of ${fd.containingMessage.name}")
        }
      } else parseSingleValue(cmp, fd, value)
    }

    formatRegistry.getMessageParser(cmp.defaultInstance.getClass) match {
      case Some(p) => p(this, value).asInstanceOf[GeneratedMessage].toPMessage
      case None =>
        value.obj match {
          case Some(fields) =>
            val values: Map[String, Json] = fields.toMap

            val valueMap: Map[FieldDescriptor, PValue] = (for {
              fd <- cmp.scalaDescriptor.fields
              jsValue <- values.get(serializedName(fd)) if !jsValue.isNull
            } yield (fd, parseValue(fd, jsValue))).toMap

            PMessage(valueMap)
          case _ =>
            throw new JsonFormatException(s"Expected an object, found ${value}")
        }
    }
  }

  def defaultEnumParser(enumDescriptor: EnumDescriptor, value: Json): EnumValueDescriptor =
    value.number match {
      case Some(v) =>
        v.toInt.flatMap { i =>
          enumDescriptor.findValueByNumber(i)
        }.getOrElse(
          throw new JsonFormatException(
            s"Invalid enum value: ${v.toInt} for enum type: ${enumDescriptor.fullName}"))
      case _ =>
        value.string match {
          case Some(s) =>
            enumDescriptor.values
              .find(_.name == s)
              .getOrElse(throw new JsonFormatException(s"Unrecognized enum value '${s}'"))
          case _ =>
            throw new JsonFormatException(
              s"Unexpected value ($value) for enum ${enumDescriptor.fullName}")
        }
    }

  protected def parseSingleValue(
    containerCompanion: GeneratedMessageCompanion[_],
    fd: FieldDescriptor,
    value: Json): PValue = fd.scalaType match {
    case ScalaType.Enum(ed) =>
      PEnum(formatRegistry.getEnumParser(ed) match {
        case Some(parser) => parser(this, value)
        case None => defaultEnumParser(ed, value)
      })
    case ScalaType.Message(_) =>
      fromJsonToPMessage(containerCompanion.messageCompanionForFieldNumber(fd.number), value)
    case st =>
      JsonFormat.parsePrimitive(
        st,
        fd.protoType,
        value,
        throw new JsonFormatException(
          s"Unexpected value ($value) for field ${serializedName(fd)} of ${fd.containingMessage.name}"))
  }
}

object JsonFormat {
  import com.google.protobuf.wrappers
  import scalapb_json.ScalapbJsonCommon._

  val DefaultRegistry = FormatRegistry()
    .registerWriter(
      (d: Duration) => Json.jString(Durations.writeDuration(d)), {
        _.string match {
          case Some(str) =>
            Durations.parseDuration(str)
          case _ =>
            throw new JsonFormatException("Expected a string.")
        }
      }
    )
    .registerWriter(
      (t: Timestamp) => Json.jString(Timestamps.writeTimestamp(t)), {
        _.string match {
          case Some(str) =>
            Timestamps.parseTimestamp(str)
          case _ =>
            throw new JsonFormatException("Expected a string.")
        }
      }
    )
    .registerMessageFormatter[wrappers.DoubleValue](
      primitiveWrapperWriter,
      primitiveWrapperParser[wrappers.DoubleValue])
    .registerMessageFormatter[wrappers.FloatValue](
      primitiveWrapperWriter,
      primitiveWrapperParser[wrappers.FloatValue])
    .registerMessageFormatter[wrappers.Int32Value](
      primitiveWrapperWriter,
      primitiveWrapperParser[wrappers.Int32Value])
    .registerMessageFormatter[wrappers.Int64Value](
      primitiveWrapperWriter,
      primitiveWrapperParser[wrappers.Int64Value])
    .registerMessageFormatter[wrappers.UInt32Value](
      primitiveWrapperWriter,
      primitiveWrapperParser[wrappers.UInt32Value])
    .registerMessageFormatter[wrappers.UInt64Value](
      primitiveWrapperWriter,
      primitiveWrapperParser[wrappers.UInt64Value])
    .registerMessageFormatter[wrappers.BoolValue](
      primitiveWrapperWriter,
      primitiveWrapperParser[wrappers.BoolValue])
    .registerMessageFormatter[wrappers.BytesValue](
      primitiveWrapperWriter,
      primitiveWrapperParser[wrappers.BytesValue])
    .registerMessageFormatter[wrappers.StringValue](
      primitiveWrapperWriter,
      primitiveWrapperParser[wrappers.StringValue])
    .registerEnumFormatter[NullValue](
      (_, _) => Json.jNull,
      (parser, value) => {
        if (value.isNull) {
          NullValue.NULL_VALUE.scalaValueDescriptor
        } else {
          parser.defaultEnumParser(NullValue.scalaDescriptor, value)
        }
      }
    )
    .registerWriter[com.google.protobuf.struct.Value](
      StructFormat.structValueWriter,
      StructFormat.structValueParser)
    .registerWriter[com.google.protobuf.struct.Struct](
      StructFormat.structWriter,
      StructFormat.structParser)
    .registerWriter[com.google.protobuf.struct.ListValue](
      x => Json.array(StructFormat.listValueWriter(x): _*),
      StructFormat.listValueParser)
    .registerMessageFormatter[com.google.protobuf.any.Any](AnyFormat.anyWriter, AnyFormat.anyParser)

  def primitiveWrapperWriter[T <: GeneratedMessage with Message[T]](
    implicit cmp: GeneratedMessageCompanion[T]): ((Printer, T) => Json) = {
    val fieldDesc = cmp.scalaDescriptor.findFieldByNumber(1).get
    (printer, t) =>
      printer.serializeSingleValue(fieldDesc, t.getField(fieldDesc), formattingLongAsNumber = false)
  }

  def primitiveWrapperParser[T <: GeneratedMessage with Message[T]](
    implicit cmp: GeneratedMessageCompanion[T]): ((Parser, Json) => T) = {
    val fieldDesc = cmp.scalaDescriptor.findFieldByNumber(1).get
    (parser, jv) =>
      cmp.messageReads.read(
        PMessage(
          Map(
            fieldDesc -> JsonFormat.parsePrimitive(
              fieldDesc.scalaType,
              fieldDesc.protoType,
              jv,
              throw new JsonFormatException(s"Unexpected value for ${cmp.scalaDescriptor.name}")))))
  }

  val printer = new Printer()
  val parser = new Parser()

  def toJsonString[A <: GeneratedMessage](m: A): String = printer.print(m)

  def toJson[A <: GeneratedMessage](m: A): Json = printer.toJson(m)

  def fromJson[A <: GeneratedMessage with Message[A]: GeneratedMessageCompanion](
    value: Json): A = {
    parser.fromJson(value)
  }

  def fromJsonString[A <: GeneratedMessage with Message[A]: GeneratedMessageCompanion](
    str: String): A = {
    parser.fromJsonString(str)
  }

  implicit def protoToDecodeJson[T <: GeneratedMessage with Message[T]: GeneratedMessageCompanion]: DecodeJson[T] =
    DecodeJson { value =>
      try {
        DecodeResult.ok(parser.fromJson(value.focus))
      } catch {
        case NonFatal(e) =>
          DecodeResult.fail(e.toString, value.history)
      }
    }

  implicit def protoToEncodeJson[T <: GeneratedMessage with Message[T]]: EncodeJson[T] =
    EncodeJson(printer.toJson(_))

  def parsePrimitive(
    scalaType: ScalaType,
    protoType: FieldDescriptorProto.Type,
    value: Json,
    onError: => PValue
  ): PValue = {
    scalaType match {
      case ScalaType.Int => value.fold(
        jsonNull = onError,
        jsonBool = _ => onError,
        jsonNumber = {
          _.toInt match {
            case Some(i) => PInt(i)
            case None => onError
          }
        },
        jsonString = x =>
          if (protoType.isTypeInt32 || protoType.isTypeSint32) {
            parseInt32(x)
          } else {
            parseUint32(x)
          }
        ,
        jsonArray = x => onError,
        jsonObject = x => onError
      )
      case ScalaType.Long => value.fold(
        jsonNull = onError,
        jsonBool = _ => onError,
        jsonNumber = {
          _.toLong match {
            case Some(i) => PLong(i)
            case None => onError
          }
        }
        ,
        jsonString = x =>
          if (protoType.isTypeInt64 || protoType.isTypeSint64) {
            parseInt64(x)
          } else {
            parseUint64(x)
          }
        ,
        jsonArray = x => onError,
        jsonObject = x => onError
      )
      case ScalaType.Double => value.fold(
        jsonNull = onError,
        jsonBool = _ => onError,
        jsonNumber = {
          _.toDouble match {
            case Some(i) => PDouble(i)
            case None => onError
          }
        },
        jsonString = {
          case "NaN" => PDouble(Double.NaN)
          case "Infinity" => PDouble(Double.PositiveInfinity)
          case "-Infinity" => PDouble(Double.NegativeInfinity)
          case _ => onError
        },
        jsonArray = x => onError,
        jsonObject = x => onError
      )
      case ScalaType.Float => value.fold(
        jsonNull = onError,
        jsonBool = _ => onError,
        jsonNumber = {
          _.toFloat match {
            case Some(i) => PFloat(i)
            case None => onError
          }
        },
        jsonString = {
          case "NaN" => PFloat(Float.NaN)
          case "Infinity" => PFloat(Float.PositiveInfinity)
          case "-Infinity" => PFloat(Float.NegativeInfinity)
          case _ => onError
        },
        jsonArray = x => onError,
        jsonObject = x => onError
      )
      case ScalaType.Boolean =>
        value.bool match {
          case Some(i) => PBoolean(i)
          case None => onError
        }
      case ScalaType.String =>
        value.string match {
          case Some(i) => PString(i)
          case None => onError
        }
      case ScalaType.ByteString =>
        value.string match {
          case Some(s) =>
            PByteString(ByteString.copyFrom(java.util.Base64.getDecoder.decode(s)))
          case None =>
            onError
        }
      case _ =>
        onError
    }
  }

}
