package scalapb_playjson

import com.google.protobuf.struct.Value.Kind
import com.google.protobuf.struct
import play.api.libs.json._

object StructFormat {
  def structValueWriter(v: struct.Value): JsValue = v.kind match {
    case Kind.Empty => JsNull
    case Kind.NullValue(_) => JsNull
    case Kind.NumberValue(value) => JsNumber(value)
    case Kind.StringValue(value) => JsString(value)
    case Kind.BoolValue(value) => JsBoolean(value)
    case Kind.StructValue(value) => structWriter(value)
    case Kind.ListValue(value) => listValueWriter(value)
  }

  def structValueParser(v: JsValue): struct.Value = {
    val kind: struct.Value.Kind = v match {
      case JsNull => Kind.NullValue(struct.NullValue.NULL_VALUE)
      case JsString(s) => Kind.StringValue(value = s)
      case JsNumber(num) => Kind.NumberValue(value = num.toDouble)
      case JsBoolean(value) => Kind.BoolValue(value = value)
      case obj: JsObject => Kind.StructValue(value = structParser(obj))
      case arr: JsArray => Kind.ListValue(listValueParser(arr))
    }
    struct.Value(kind = kind)
  }

  def structParser(v: JsValue): struct.Struct = v match {
    case JsObject(fields) =>
      struct.Struct(fields = fields.map(kv => (kv._1, structValueParser(kv._2))).toMap)
    case _ => throw new JsonFormatException("Expected an object")
  }

  def structWriter(v: struct.Struct): JsValue =
    JsObject(v.fields.mapValues(structValueWriter).toList)

  def listValueParser(v: JsValue): struct.ListValue = v match {
    case JsArray(elems) =>
      com.google.protobuf.struct.ListValue(elems.map(structValueParser))
    case _ => throw new JsonFormatException("Expected a list")
  }

  def listValueWriter(v: struct.ListValue): JsArray =
    JsArray(v.values.map(structValueWriter).toList)

  def nullValueParser(v: JsValue): struct.NullValue = v match {
    case JsNull =>
      com.google.protobuf.struct.NullValue.NULL_VALUE
    case _ => throw new JsonFormatException("Expected a null")
  }

  def nullValueWriter(v: struct.NullValue): JsValue = JsNull
}
