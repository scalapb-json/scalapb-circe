# scalapb-circe
[![scaladoc](https://javadoc-badge.appspot.com/io.github.scalapb-json/scalapb-circe_2.12.svg?label=scaladoc)](https://javadoc-badge.appspot.com/io.github.scalapb-json/scalapb-circe_2.12/scalapb_circe/index.html?javadocio=true)

The structure of this project is hugely inspired by [scalapb-json4s](https://github.com/scalapb/scalapb-json4s)

## Dependency

Include in your `build.sbt` file

### core

```scala
libraryDependencies += "io.github.scalapb-json" %% "scalapb-circe" % "0.11.1"
```

for scala-js

```scala
libraryDependencies += "io.github.scalapb-json" %%% "scalapb-circe" % "0.11.1"
```

### macros

```scala
libraryDependencies += "io.github.scalapb-json" %% "scalapb-circe-macros" % "0.11.1"
```

## Usage

### JsonFormat

There are four functions you can use directly to serialize/deserialize your messages:

```scala
JsonFormat.toJsonString(msg) // returns String
JsonFormat.toJson(msg) // returns Json

JsonFormat.fromJsonString(str) // return MessageType
JsonFormat.fromJson(json) // return MessageType
```

### Implicit Circe Codecs

You can also import codecs to support Circe's implicit syntax for objects of type `GeneratedMessage` and `GeneratedEnum`.

Assume a proto message:

```scala
message Guitar {
  int32 number_of_strings = 1;
}
```

```scala
import io.circe.syntax._
import io.circe.parser._
import scalapb_circe.codec._

Guitar(42).asJson.noSpaces // returns {"numberOfStrings":42}

decode[Guitar]("""{"numberOfStrings": 42}""") // returns Right(Guitar(42))
Json.obj("numberOfStrings" -> Json.fromInt(42)).as[Guitar] // returns Right(Guitar(42))
```

You can define an implicit `scalapb_circe.Printer` and/or `scalapb_circe.Parser` to control printing and parsing settings. 
For example, to include default values in Json:

```scala
import io.circe.syntax._
import io.circe.parser._
import scalapb_circe.codec._
import scalapb_circe.Printer

implicit val p: Printer = new Printer(includingDefaultValueFields = true)

Guitar(0).asJson.noSpaces // returns {"numberOfStrings": 0}
```

Finally, you can include scalapb `GeneratedMessage` and `GeneratedEnum`s in regular case classes with semi-auto derivation:

```scala
import io.circe.generic.semiauto._
import io.circe.syntax._
import io.circe._
import scalapb_circe.codec._ // IntelliJ might say this is unused.

case class Band(guitars: Seq[Guitar])
object Band {
  implicit val dec: Decoder[Band] = deriveDecoder[Band]
  implicit val enc: Encoder[Band] = deriveEncoder[Band]
}
Band(Seq(Guitar(42))).asJson.noSpaces // returns {"guitars":[{"numberOfStrings":42}]}
```


### Credits

- https://github.com/whisklabs/scalapb-playjson
- https://github.com/scalapb/scalapb-json4s
