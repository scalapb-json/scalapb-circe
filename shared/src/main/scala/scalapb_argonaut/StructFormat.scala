package scalapb_argonaut

import com.google.protobuf.struct.Value.Kind
import com.google.protobuf.struct
import argonaut._
import scalapb_json._

object StructFormat {
  def structValueWriter(v: struct.Value): Json = v.kind match {
    case Kind.Empty => Json.jNull
    case Kind.NullValue(_) => Json.jNull
    case Kind.NumberValue(value) => Json.jNumber(value)
    case Kind.StringValue(value) => Json.jString(value)
    case Kind.BoolValue(value) => Json.jBool(value)
    case Kind.StructValue(value) => structWriter(value)
    case Kind.ListValue(value) => Json.jArray(listValueWriter(value))
  }

  def structValueParser(v: Json): struct.Value = {
    val kind: struct.Value.Kind = v.fold(
      jsonNull = Kind.NullValue(struct.NullValue.NULL_VALUE),
      jsonBool = value => Kind.BoolValue(value = value),
      jsonNumber = x => Kind.NumberValue(value = x.toBigDecimal.toDouble),
      jsonString = x => Kind.StringValue(value = x),
      jsonArray = x => Kind.ListValue(listValueParser(x)),
      jsonObject = x => Kind.StructValue(value = structParser(x))
    )
    struct.Value(kind = kind)
  }

  def structParser(v: JsonObject): struct.Struct = {
    struct.Struct(fields = v.toMap.map(kv => (kv._1, structValueParser(kv._2))))
  }

  def structParser(v: Json): struct.Struct = v.obj match {
    case Some(x) => structParser(x)
    case None => throw new JsonFormatException("Expected an object")
  }

  def structWriter(v: struct.Struct): Json =
    Json.obj(v.fields.map {
      case (x, y) => x -> structValueWriter(y)
    }(collection.breakOut): _*)

  def listValueParser(v: Json.JsonArray): struct.ListValue = {
    com.google.protobuf.struct.ListValue(v.map(structValueParser))
  }

  def listValueParser(v: Json): struct.ListValue = v.array match {
    case Some(x) => listValueParser(x)
    case None => throw new JsonFormatException("Expected an array")
  }

  def listValueWriter(v: struct.ListValue): Json.JsonArray =
    v.values.map(structValueWriter).toList

  def nullValueParser(v: Json): struct.NullValue = {
    if(v.isNull)
      com.google.protobuf.struct.NullValue.NULL_VALUE
    else
      throw new JsonFormatException("Expected a null")
  }

}
