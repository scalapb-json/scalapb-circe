addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.27")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.11")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.4")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.2")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.0.0")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.20")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.9.0-M1"
