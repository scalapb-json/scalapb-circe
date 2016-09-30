# scalapb-playjson

The structure of this project is hugely inspired by [scalapb-json4s](https://github.com/trueaccord/scalapb-json4s)

## Dependency

Artifacts are available for Scala 2.11 only (because of play-json 2.5 dependency)

Include in your `build.sbt` file

```scala
libraryDependencies += "com.whisk" % "scalapb-playjson_2.11" % "0.1"
```

## Usage

There are four functions you can use directly to serialize/deserialize your messages:

```scala
ScalaPBJson.toJsonString(msg) // returns String
ScalaPBJson.toJson(msg): // returns JsObject

ScalaPBJson.fromJsonString(str) // return MessageType
ScalaPBJson.fromJson(json) // return MessageType
```

Alternatively you can define play-json `Reads[T]`, `Writes[T]` or `Format[T]` and use serialization implicitly

```scala
import play.api.libs.json._

implicit val myMsgWrites: Writes[MyMsg] = ScalaPBJson.writes[MyMsg]

implicit val myMsgReads: Reads[MyMsg] = ScalaPBJson.reads[MyMsg]

implicit val myMsgFmt: Format[MyMsg] = ScalaPBJson.format[MyMsg]
```

There are helper methods for enums as well if necessary

```scala
ScalaPBJson.enumReads
ScalaPBJson.enumWrites
ScalaPBJson.enumFormat
```
