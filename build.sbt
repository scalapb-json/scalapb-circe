import com.trueaccord.scalapb.compiler.Version._
import sbtrelease.ReleaseStateTransformations._

val Scala211 = "2.11.12"

scalaVersion := Scala211

crossScalaVersions := Seq("2.12.4", Scala211, "2.10.7")

scalacOptions ++= Seq("-feature", "-deprecation")

description := "Json/Protobuf convertors for ScalaPB"

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

organization := "com.github.xuwei-k"

name := UpdateReadme.scalapbPlayJsonName

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

val tagName = Def.setting {
  s"v${if (releaseUseGlobalVersion.value) (version in ThisBuild).value else version.value}"
}
val tagOrHash = Def.setting {
  if (isSnapshot.value) sys.process.Process("git rev-parse HEAD").lineStream_!.head
  else tagName.value
}

scalacOptions in (Compile, doc) ++= {
  val t = tagOrHash.value
  Seq(
    "-sourcepath",
    (baseDirectory in LocalRootProject).value.getAbsolutePath,
    "-doc-source-url",
    s"https://github.com/xuwei-k/scalapb-playjson/tree/${t}€{FILE_PATH}.scala"
  )
}

ReleasePlugin.extraReleaseCommands

commands += Command.command("updateReadme")(UpdateReadme.updateReadmeTask)

releaseTagName := tagName.value

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  UpdateReadme.updateReadmeProcess,
  tagRelease,
  ReleaseStep(
    action = { state =>
      val extracted = Project extract state
      extracted
        .runAggregated(PgpKeys.publishSigned in Global in extracted.get(thisProjectRef), state)
    },
    enableCrossBuild = true
  ),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  UpdateReadme.updateReadmeProcess,
  pushChanges
)
