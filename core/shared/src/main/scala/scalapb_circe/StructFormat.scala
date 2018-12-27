package scalapb_circe

import com.google.protobuf.struct.Value.Kind
import com.google.protobuf.struct
import io.circe._
import scalapb_json._

object StructFormat {
  def structValueWriter(v: struct.Value): Json = v.kind match {
    case Kind.Empty => Json.Null
    case Kind.NullValue(_) => Json.Null
    case Kind.NumberValue(value) => Json.fromDoubleOrString(value)
    case Kind.StringValue(value) => Json.fromString(value)
    case Kind.BoolValue(value) => Json.fromBoolean(value)
    case Kind.StructValue(value) => structWriter(value)
    case Kind.ListValue(value) => listValueWriter(value)
  }

  def structValueParser(v: Json): struct.Value = {
    val kind: struct.Value.Kind = v.fold(
      jsonNull = Kind.NullValue(struct.NullValue.NULL_VALUE),
      jsonBoolean = value => Kind.BoolValue(value = value),
      jsonNumber = x => Kind.NumberValue(value = x.toDouble),
      jsonString = x => Kind.StringValue(value = x),
      jsonArray = x => Kind.ListValue(listValueParser(x)),
      jsonObject = x => Kind.StructValue(value = structParser(x))
    )
    struct.Value(kind = kind)
  }

  def structParser(v: JsonObject): struct.Struct = {
    struct.Struct(fields = v.toMap.map(kv => (kv._1, structValueParser(kv._2))))
  }

  def structParser(v: Json): struct.Struct = v.asObject match {
    case Some(x) => structParser(x)
    case None => throw new JsonFormatException("Expected an object")
  }

  def structWriter(v: struct.Struct): Json =
    Json.obj(v.fields.map {
      case (x, y) => x -> structValueWriter(y)
    }(collection.breakOut): _*)

  def listValueParser(json: Seq[Json]): struct.ListValue =
    com.google.protobuf.struct.ListValue(json.map(structValueParser))

  def listValueParser(json: Json): struct.ListValue = json.asArray match {
    case Some(v) =>
      listValueParser(v)
    case None =>
      throw new JsonFormatException("Expected an array")
  }

  def listValueWriter(v: struct.ListValue): Json =
    Json.fromValues(v.values.map(structValueWriter))

  def nullValueParser(v: Json): struct.NullValue = {
    if (v.isNull)
      com.google.protobuf.struct.NullValue.NULL_VALUE
    else
      throw new JsonFormatException("Expected a null")
  }

}
