scalaVersion := "2.11.8"

crossScalaVersions := Seq("2.11.8", "2.12.1")

scalacOptions ++= Seq("-feature", "-deprecation")

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

sonatypeProfileName := "com.whisk"

organization := "com.whisk"

name := "scalapb-playjson"

version := "0.2.1"

val scalaPbVersion = "0.5.47"

Project.inConfig(Test)(sbtprotoc.ProtocPlugin.protobufConfigSettings)

PB.targets in Compile := Nil

PB.targets in Test := Seq(
  PB.gens.java -> (sourceManaged in Test).value,
  scalapb.gen(javaConversions = true) -> (sourceManaged in Test).value
)

val playVer = Def.setting[String] {
  if (scalaVersion.value startsWith "2.11.") "2.5.10"
  else "2.6.0-M1"
}

libraryDependencies ++= Seq(
  "com.trueaccord.scalapb" %% "scalapb-runtime" % scalaPbVersion,
  "com.typesafe.play" %% "play-json" % playVer.value,
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "com.google.protobuf" % "protobuf-java-util" % "3.1.0" % "test",
  "com.google.protobuf" % "protobuf-java" % "3.1.0" % "protobuf"
)

pomExtra in Global := {
  <url>https://github.com/whisklabs/scalapb-playjson</url>
    <scm>
        <connection>scm:git:github.com/whisklabs/scalapb-playjson.git</connection>
        <developerConnection>scm:git:git@github.com:whisklabs/scalapb-playjson.git</developerConnection>
        <url>github.com/whisklabs/scalapb-playjson.git</url>
    </scm>
    <developers>
        <developer>
            <id>viktortnk</id>
            <name>Viktor Taranenko</name>
            <url>https://github.com/viktortnk</url>
        </developer>
    </developers>
}
