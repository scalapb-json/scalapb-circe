scalaVersion := "2.11.8"

crossScalaVersions := Seq("2.11.8")

scalacOptions ++= Seq("-feature", "-deprecation")

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

sonatypeProfileName := "com.whisk"

organization := "com.whisk"

name := "scalapb-playjson"

version := "0.1"

val scalaPbVersion = "0.5.47"

Project.inConfig(Test)(sbtprotoc.ProtocPlugin.protobufConfigSettings)

PB.targets in Test := Seq(
  scalapb.gen() -> (sourceManaged in Test).value
)

libraryDependencies ++= Seq(
  "com.trueaccord.scalapb" %% "scalapb-runtime" % scalaPbVersion,
  "com.typesafe.play" %% "play-json" % "2.5.10",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
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
