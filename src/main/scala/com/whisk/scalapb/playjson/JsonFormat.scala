package com.whisk.scalapb.playjson

import com.fasterxml.jackson.core.Base64Variants
import com.google.protobuf.ByteString
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType
import com.google.protobuf.Descriptors.{EnumValueDescriptor, FieldDescriptor}
import com.trueaccord.scalapb._
import play.api.libs.json.{JsError, _}

import scala.language.existentials
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

case class JsonFormatException(msg: String, cause: Exception) extends Exception(msg, cause) {
  def this(msg: String) = this(msg, null)
}

case class FormatRegistry(mapClass: Map[Class[_], (_ => JsValue, JsValue => _)] = Map.empty) {
  def registerWriter[T <: GeneratedMessage](writer: T => JsValue, parser: JsValue => T)(
      implicit ct: ClassTag[T]): FormatRegistry = {
    copy(mapClass + (ct.runtimeClass -> (writer, parser)))
  }

  def getWriter[T](klass: Class[_ <: T]): Option[T => JsValue] = {
    mapClass.get(klass).map(_._1.asInstanceOf[T => JsValue])
  }

  def getParser[T](klass: Class[_ <: T]): Option[JsValue => T] = {
    mapClass.get(klass).map(_._2.asInstanceOf[JsValue => T])
  }
}

class Printer(includingDefaultValueFields: Boolean = false,
              preservingProtoFieldNames: Boolean = false,
              formatRegistry: FormatRegistry = JsonFormat.DefaultRegistry) {

  def print[A](m: GeneratedMessage): String = {
    Json.stringify(toJson(m))
  }

  def toJson[A <: GeneratedMessage](m: A): JsValue = {
    formatRegistry.getWriter[A](m.getClass) match {
      case Some(f) => f(m)
      case None =>
        val b = List.newBuilder[(String, JsValue)]
        m.getAllFields
        b.sizeHint(m.companion.javaDescriptor.getFields.size)
        val i = m.companion.javaDescriptor.getFields.iterator
        while (i.hasNext) {
          val f = i.next()
          if (f.getType != FieldDescriptor.Type.GROUP) {
            val name = if (preservingProtoFieldNames) f.getName else f.getJsonName
            m.getField(f) match {
              case null =>
                if (includingDefaultValueFields && f.getJavaType != JavaType.MESSAGE) {
                  // We are never printing empty optional messages to prevent infinite recursion.
                  b += (name -> JsonFormat.defaultJValue(f))
                }
              case Nil if f.isRepeated =>
                if (includingDefaultValueFields) {
                  b += (name -> (if (f.isMapField) Json.obj() else JsArray()))
                }
              case v => b += (name -> serializeField(f, v))
            }
          }
        }
        JsObject(b.result())
    }
  }

  @inline
  private def serializeField(fd: FieldDescriptor, value: Any): JsValue = {
    if (fd.isMapField) {
      JsObject(value.asInstanceOf[Seq[GeneratedMessage]].map { v =>
        val keyDescriptor = v.companion.javaDescriptor.findFieldByNumber(1)
        val key = Option(v.getField(keyDescriptor))
          .getOrElse(JsonFormat.defaultValue(keyDescriptor))
          .toString
        val valueDescriptor = v.companion.javaDescriptor.findFieldByNumber(2)
        val value =
          Option(v.getField(valueDescriptor)).getOrElse(JsonFormat.defaultValue(valueDescriptor))
        key -> serializeSingleValue(valueDescriptor, value)
      })
    } else if (fd.isRepeated) {
      JsArray(value.asInstanceOf[Seq[Any]].map(serializeSingleValue(fd, _)).toList)
    } else serializeSingleValue(fd, value)
  }

  @inline
  private def serializeSingleValue(fd: FieldDescriptor, value: Any): JsValue =
    fd.getJavaType match {
      case JavaType.ENUM => JsString(value.asInstanceOf[EnumValueDescriptor].getName)
      case JavaType.MESSAGE => toJson(value.asInstanceOf[GeneratedMessage])
      case JavaType.INT => JsNumber(value.asInstanceOf[Int])
      case JavaType.LONG => JsNumber(value.asInstanceOf[Long])
      case JavaType.DOUBLE => JsNumber(value.asInstanceOf[Double])
      case JavaType.FLOAT => JsNumber(value.asInstanceOf[Float].toDouble)
      case JavaType.BOOLEAN => JsBoolean(value.asInstanceOf[Boolean])
      case JavaType.STRING => JsString(value.asInstanceOf[String])
      case JavaType.BYTE_STRING =>
        JsString(
          Base64Variants.getDefaultVariant.encode(value.asInstanceOf[ByteString].toByteArray))
    }
}

object Printer {}

class Parser(formatRegistry: FormatRegistry = JsonFormat.DefaultRegistry) {
  def fromJsonString[A <: GeneratedMessage with Message[A]](str: String)(
      implicit cmp: GeneratedMessageCompanion[A]): A = {
    fromJson(Json.parse(str))
  }

  def fromJson[A <: GeneratedMessage with Message[A]](value: JsValue)(
      implicit cmp: GeneratedMessageCompanion[A]): A = {

    import scala.collection.JavaConverters._

    def parseValue(fd: FieldDescriptor, value: JsValue): Any = {
      if (fd.isMapField) {
        value match {
          case JsObject(vals) =>
            val mapEntryCmp = cmp.messageCompanionForField(fd)
            val keyDescriptor = fd.getMessageType.findFieldByNumber(1)
            val valueDescriptor = fd.getMessageType.findFieldByNumber(2)
            vals.map {
              case (key, jValue) =>
                val keyObj = keyDescriptor.getJavaType match {
                  case JavaType.BOOLEAN => java.lang.Boolean.valueOf(key)
                  case JavaType.DOUBLE => java.lang.Double.valueOf(key)
                  case JavaType.FLOAT => java.lang.Float.valueOf(key)
                  case JavaType.INT => java.lang.Integer.valueOf(key)
                  case JavaType.LONG => java.lang.Long.valueOf(key)
                  case JavaType.STRING => key
                  case _ =>
                    throw new RuntimeException(s"Unsupported type for key for ${fd.getName}")
                }
                mapEntryCmp.fromFieldsMap(
                  Map(keyDescriptor -> keyObj,
                      valueDescriptor -> parseSingleValue(mapEntryCmp, valueDescriptor, jValue)))
            }
          case _ =>
            throw new JsonFormatException(
              s"Expected an object for map field ${fd.getJsonName} of ${fd.getContainingType.getName}")
        }
      } else if (fd.isRepeated) {
        value match {
          case JsArray(vals) => vals.map(parseSingleValue(cmp, fd, _)).toVector
          case _ =>
            throw new JsonFormatException(
              s"Expected an array for repeated field ${fd.getJsonName} of ${fd.getContainingType.getName}")
        }
      } else parseSingleValue(cmp, fd, value)
    }

    formatRegistry.getParser(cmp.defaultInstance.getClass) match {
      case Some(p) => p(value)
      case None =>
        value match {
          case JsObject(values) =>
            val valueMap: Map[FieldDescriptor, Any] = (for {
              fd <- cmp.javaDescriptor.getFields.asScala
              jsValue <- values.get(fd.getJsonName)
            } yield (fd, parseValue(fd, jsValue))).toMap

            cmp.fromFieldsMap(valueMap)
          case _ =>
            throw new JsonFormatException(s"Expected an object, found $value")
        }
    }
  }

  protected def parseSingleValue(cmp: GeneratedMessageCompanion[_],
                                 fd: FieldDescriptor,
                                 value: JsValue): Any = (fd.getJavaType, value) match {
    case (JavaType.ENUM, JsString(s)) => fd.getEnumType.findValueByName(s)
    case (JavaType.MESSAGE, o: JsValue) =>
      // The asInstanceOf[] is a lie: we actually have a companion of some other message (not A),
      // but this doesn't matter after erasure.
      fromJson(o)(
        cmp
          .messageCompanionForField(fd)
          .asInstanceOf[GeneratedMessageCompanion[T] forSome {
            type T <: GeneratedMessage with Message[T]
          }])
    case (JavaType.INT, JsNumber(x)) => x.toInt
    case (JavaType.INT, JsNull) => 0
    case (JavaType.LONG, JsNumber(x)) => x.toLong
    case (JavaType.LONG, JsNull) => 0L
    case (JavaType.DOUBLE, JsNumber(x)) => x.toDouble
    case (JavaType.DOUBLE, JsNull) => 0.0
    case (JavaType.FLOAT, JsNumber(x)) => x.toFloat
    case (JavaType.FLOAT, JsNull) => 0.0f
    case (JavaType.BOOLEAN, JsBoolean(b)) => b
    case (JavaType.BOOLEAN, JsNull) => false
    case (JavaType.STRING, JsString(s)) => s
    case (JavaType.STRING, JsNull) => ""
    case (JavaType.BYTE_STRING, JsString(s)) =>
      ByteString.copyFrom(Base64Variants.getDefaultVariant.decode(s))
    case (JavaType.BYTE_STRING, JsNull) => ByteString.EMPTY
    case _ =>
      throw new JsonFormatException(
        s"Unexpected value ($value) for field ${fd.getJsonName} of ${fd.getContainingType.getName}")
  }
}

object JsonFormat {
  val DefaultRegistry = FormatRegistry()
    .registerWriter(WellKnownTypes.writeDuration, {
      case JsString(str) => WellKnownTypes.parseDuration(str)
      case _ => throw new JsonFormatException("Expected a string.")
    })

  val printer = new Printer()
  val parser = new Parser()

  def toJsonString[A <: GeneratedMessage](m: A): String = printer.print(m)

  def toJson[A <: GeneratedMessage](m: A): JsValue = printer.toJson(m)

  def fromJson[A <: GeneratedMessage with Message[A]: GeneratedMessageCompanion](
      value: JsValue): A = {
    parser.fromJson(value)
  }

  def fromJsonString[A <: GeneratedMessage with Message[A]: GeneratedMessageCompanion](
      str: String): A = {
    parser.fromJsonString(str)
  }

  def defaultValue(fd: FieldDescriptor): Any = {
    require(fd.isOptional)
    fd.getJavaType match {
      case JavaType.INT => 0
      case JavaType.LONG => 0L
      case JavaType.FLOAT | JavaType.DOUBLE => 0.0
      case JavaType.BOOLEAN => false
      case JavaType.STRING => ""
      case JavaType.BYTE_STRING => ByteString.EMPTY
      case JavaType.ENUM => fd.getEnumType.getValues.get(0)
      case JavaType.MESSAGE => throw new RuntimeException("No default value for message")
    }
  }

  def defaultJValue(fd: FieldDescriptor): JsValue = {
    require(fd.isOptional)
    fd.getJavaType match {
      case JavaType.INT | JavaType.LONG => JsNumber(0)
      case JavaType.FLOAT | JavaType.DOUBLE => JsNumber(0)
      case JavaType.BOOLEAN => JsBoolean(false)
      case JavaType.STRING => JsString("")
      case JavaType.BYTE_STRING => JsString("")
      case JavaType.ENUM => JsString(fd.getEnumType.getValues.get(0).getName)
      case JavaType.MESSAGE => throw new RuntimeException("No default value for message")
    }
  }

  def writes[A <: GeneratedMessage] = new Writes[A] {
    override def writes(o: A): JsValue = toJson(o)
  }

  def reads[A <: GeneratedMessage with Message[A]: GeneratedMessageCompanion] = new Reads[A] {
    override def reads(json: JsValue): JsResult[A] = Try[A](fromJson[A](json)) match {
      case Success(value) => JsSuccess(value)
      case Failure(f) => JsError(f.getMessage)
    }
  }

  def format[A <: GeneratedMessage with Message[A]: GeneratedMessageCompanion] =
    Format(reads[A], writes[A])

  def enumWrites[A <: GeneratedEnum]: Writes[A] = new Writes[A] {
    override def writes(o: A): JsValue = JsString(o.name)
  }

  def enumReads[A <: GeneratedEnum: GeneratedEnumCompanion] = new Reads[A] {
    override def reads(json: JsValue): JsResult[A] = json match {
      case JsString(value) =>
        implicitly[GeneratedEnumCompanion[A]].fromName(value) match {
          case None => JsSuccess(implicitly[GeneratedEnumCompanion[A]].fromValue(-1))
          case Some(v) => JsSuccess(v)
        }
      case JsNull =>
        JsSuccess(implicitly[GeneratedEnumCompanion[A]].fromValue(-1))
      case _ =>
        JsError("incompatible json format")
    }
  }

  def enumFormat[A <: GeneratedEnum: GeneratedEnumCompanion]: Format[A] =
    Format(enumReads[A], enumWrites[A])
}
