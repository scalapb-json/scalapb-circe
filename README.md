# scalapb-playjson [![Build Status](https://travis-ci.org/xuwei-k/scalapb-playjson.svg?branch=master)](https://travis-ci.org/xuwei-k/scalapb-playjson)
[![scaladoc](https://javadoc-badge.appspot.com/com.github.xuwei-k/scalapb-playjson_2.12.svg?label=scaladoc)](https://javadoc-badge.appspot.com/com.github.xuwei-k/scalapb-playjson_2.12/scalapb_playjson/index.html?javadocio=true)

The structure of this project is hugely inspired by [scalapb-json4s](https://github.com/scalapb/scalapb-json4s)

## Dependency

Include in your `build.sbt` file

```scala
libraryDependencies += "com.github.xuwei-k" %% "scalapb-playjson" % "0.3.0"
```

## Usage

There are four functions you can use directly to serialize/deserialize your messages:

```scala
JsonFormat.toJsonString(msg) // returns String
JsonFormat.toJson(msg): // returns JsObject

JsonFormat.fromJsonString(str) // return MessageType
JsonFormat.fromJson(json) // return MessageType
```

Alternatively you can define play-json `Reads[T]`, `Writes[T]` or `Format[T]` and use serialization implicitly

```scala
import play.api.libs.json._

implicit val myMsgWrites: Writes[MyMsg] = JsonFormat.writes[MyMsg]

implicit val myMsgReads: Reads[MyMsg] = JsonFormat.reads[MyMsg]

implicit val myMsgFmt: Format[MyMsg] = JsonFormat.format[MyMsg]
```

There are helper methods for enums as well if necessary

```scala
JsonFormat.enumReads
JsonFormat.enumWrites
JsonFormat.enumFormat
```

### Credits

fork from https://github.com/whisklabs/scalapb-playjson
