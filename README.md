# scalapb-playjson [![Build Status](https://travis-ci.org/scalapb-json/scalapb-playjson.svg?branch=master)](https://travis-ci.org/scalapb-json/scalapb-playjson)
[![scaladoc](https://javadoc-badge.appspot.com/com.github.xuwei-k/scalapb-playjson_2.12.svg?label=scaladoc)](https://javadoc-badge.appspot.com/com.github.xuwei-k/scalapb-playjson_2.12/scalapb_playjson/index.html?javadocio=true)

The structure of this project is hugely inspired by [scalapb-json4s](https://github.com/scalapb/scalapb-json4s)

## Dependency

Include in your `build.sbt` file

```scala
libraryDependencies += "com.github.xuwei-k" %% "scalapb-playjson" % "0.5.0"
```

for scala-js

```scala
libraryDependencies += "com.github.xuwei-k" %%% "scalapb-playjson" % "0.5.0"
```

## Usage

There are four functions you can use directly to serialize/deserialize your messages:

```scala
JsonFormat.toJsonString(msg) // returns String
JsonFormat.toJson(msg): // returns JsObject

JsonFormat.fromJsonString(str) // return MessageType
JsonFormat.fromJson(json) // return MessageType
```

### Credits

fork from https://github.com/whisklabs/scalapb-playjson
