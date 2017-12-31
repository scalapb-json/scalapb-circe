import com.trueaccord.scalapb.compiler.Version._

val Scala211 = "2.11.12"

scalaVersion := Scala211

crossScalaVersions := Seq("2.12.4", Scala211, "2.10.7")

scalacOptions ++= Seq("-feature", "-deprecation")

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

organization := "com.github.xuwei-k"

name := "scalapb-playjson"

Project.inConfig(Test)(sbtprotoc.ProtocPlugin.protobufConfigSettings)

PB.targets in Compile := Nil

PB.targets in Test := Seq(
  PB.gens.java -> (sourceManaged in Test).value,
  scalapb.gen(javaConversions = true) -> (sourceManaged in Test).value
)

libraryDependencies ++= Seq(
  "com.trueaccord.scalapb" %% "scalapb-runtime" % scalapbVersion,
  "com.typesafe.play" %% "play-json" % "2.6.8",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
  "com.google.protobuf" % "protobuf-java-util" % protobufVersion % "test",
  "com.google.protobuf" % "protobuf-java" % protobufVersion % "protobuf"
)

pomExtra in Global := {
  <url>https://github.com/xuwei-k/scalapb-playjson</url>
    <scm>
      <connection>scm:git:github.com/xuwei-k/scalapb-playjson.git</connection>
      <developerConnection>scm:git:git@github.com:xuwei-k/scalapb-playjson.git</developerConnection>
      <url>github.com/xuwei-k/scalapb-playjson.git</url>
    </scm>
    <developers>
      <developer>
        <id>xuwei-k</id>
        <name>Kenji Yoshida</name>
        <url>https://github.com/xuwei-k</url>
      </developer>
    </developers>
}

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)
