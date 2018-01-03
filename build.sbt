import com.trueaccord.scalapb.compiler.Version._
import sbtrelease.ReleaseStateTransformations._
import sbtcrossproject.CrossPlugin.autoImport.crossProject

val Scala211 = "2.11.12"

val tagName = Def.setting {
  s"v${if (releaseUseGlobalVersion.value) (version in ThisBuild).value else version.value}"
}

val tagOrHash = Def.setting {
  if (isSnapshot.value) sys.process.Process("git rev-parse HEAD").lineStream_!.head
  else tagName.value
}

val unusedWarnings = Seq("-Ywarn-unused", "-Ywarn-unused-import")

val scalapbPlayJson = crossProject(JVMPlatform, JSPlatform)
  .in(file("."))
  .settings(
    commonSettings
  )
  .jvmSettings(
    PB.targets in Test := Seq(
      PB.gens.java -> (sourceManaged in Test).value,
      scalapb.gen(javaConversions = true) -> (sourceManaged in Test).value
    ),
    libraryDependencies ++= Seq(
      "com.google.protobuf" % "protobuf-java-util" % protobufVersion % "test",
      "com.google.protobuf" % "protobuf-java" % protobufVersion % "protobuf"
    )
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % "2.0.0-M12"
    ),
    PB.targets in Test := Seq(
      scalapb.gen(javaConversions = false) -> (sourceManaged in Test).value
    )
  )

commonSettings

val noPublish = Seq(
  PgpKeys.publishLocalSigned := {},
  PgpKeys.publishSigned := {},
  publishLocal := {},
  publish := {},
  publishArtifact in Compile := false
)

noPublish

lazy val commonSettings = Seq[Def.SettingsDefinition](
  scalaVersion := Scala211,
  crossScalaVersions := Seq("2.12.4", Scala211, "2.10.7"),
  scalacOptions ++= PartialFunction
    .condOpt(CrossVersion.partialVersion(scalaVersion.value)) {
      case Some((2, v)) if v >= 11 => unusedWarnings
    }
    .toList
    .flatten,
  Seq(Compile, Test).flatMap(c => scalacOptions in (c, console) --= unusedWarnings),
  scalacOptions ++= Seq("-feature", "-deprecation"),
  description := "Json/Protobuf convertors for ScalaPB",
  licenses += ("MIT", url("https://opensource.org/licenses/MIT")),
  organization := "io.github.scalapb-json",
  name := UpdateReadme.scalapbPlayJsonName,
  Project.inConfig(Test)(sbtprotoc.ProtocPlugin.protobufConfigSettings),
  PB.targets in Compile := Nil,
  PB.protoSources in Test := Seq(file("shared/src/test/protobuf")),
  libraryDependencies ++= Seq(
    "com.trueaccord.scalapb" %%% "scalapb-runtime" % scalapbVersion,
    "com.trueaccord.scalapb" %%% "scalapb-runtime" % scalapbVersion % "protobuf,test",
    "com.typesafe.play" %%% "play-json" % "2.6.8",
    "org.scalatest" %%% "scalatest" % "3.0.4" % "test"
  ),
  pomExtra in Global := {
    <url>https://github.com/scalapb-json/scalapb-playjson</url>
      <scm>
        <connection>scm:git:github.com/scalapb-json/scalapb-playjson.git</connection>
        <developerConnection>scm:git:git@github.com:scalapb-json/scalapb-playjson.git</developerConnection>
        <url>github.com/scalapb-json/scalapb-playjson.git</url>
        <tag>{tagOrHash.value}</tag>
      </scm>
      <developers>
        <developer>
          <id>xuwei-k</id>
          <name>Kenji Yoshida</name>
          <url>https://github.com/xuwei-k</url>
        </developer>
      </developers>
  },
  publishTo := Some(
    if (isSnapshot.value)
      Opts.resolver.sonatypeSnapshots
    else
      Opts.resolver.sonatypeStaging
  ),
  scalacOptions in (Compile, doc) ++= {
    val t = tagOrHash.value
    Seq(
      "-sourcepath",
      (baseDirectory in LocalRootProject).value.getAbsolutePath,
      "-doc-source-url",
      s"https://github.com/scalapb-json/scalapb-playjson/tree/${t}€{FILE_PATH}.scala"
    )
  },
  ReleasePlugin.extraReleaseCommands,
  commands += Command.command("updateReadme")(UpdateReadme.updateReadmeTask),
  releaseTagName := tagName.value,
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
        extracted.runAggregated(
          PgpKeys.publishSigned in Global in extracted.get(thisProjectRef),
          state)
      },
      enableCrossBuild = true
    ),
    setNextVersion,
    commitNextVersion,
    releaseStepCommand("sonatypeReleaseAll"),
    UpdateReadme.updateReadmeProcess,
    pushChanges
  )
).flatMap(_.settings)

val scalapbPlayJsonJVM = scalapbPlayJson.jvm
val scalapbPlayJsonJS = scalapbPlayJson.js
