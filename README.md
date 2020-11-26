# scalapb-circe
[![scaladoc](https://javadoc-badge.appspot.com/io.github.scalapb-json/scalapb-circe_2.12.svg?label=scaladoc)](https://javadoc-badge.appspot.com/io.github.scalapb-json/scalapb-circe_2.12/scalapb_circe/index.html?javadocio=true)

The structure of this project is hugely inspired by [scalapb-json4s](https://github.com/scalapb/scalapb-json4s)

## Dependency

Include in your `build.sbt` file

### core

```scala
libraryDependencies += "io.github.scalapb-json" %% "scalapb-circe" % "0.7.2"
```

for scala-js

```scala
libraryDependencies += "io.github.scalapb-json" %%% "scalapb-circe" % "0.7.2"
```

### macros

```scala
libraryDependencies += "io.github.scalapb-json" %% "scalapb-circe-macros" % "0.7.2"
```

## Usage

There are four functions you can use directly to serialize/deserialize your messages:

```scala
JsonFormat.toJsonString(msg) // returns String
JsonFormat.toJson(msg) // returns Json

JsonFormat.fromJsonString(str) // return MessageType
JsonFormat.fromJson(json) // return MessageType
```

### Credits

- https://github.com/whisklabs/scalapb-playjson
- https://github.com/scalapb/scalapb-json4s
