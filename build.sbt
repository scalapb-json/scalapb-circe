import com.trueaccord.scalapb.compiler.Version._
import sbtrelease.ReleaseStateTransformations._
import sbtcrossproject.CrossPlugin.autoImport.crossProject

val Scala211 = "2.11.12"
val circeVersion = settingKey[String]("")
val scalapbJsonCommonVersion = settingKey[String]("")

val tagName = Def.setting {
  s"v${if (releaseUseGlobalVersion.value) (version in ThisBuild).value else version.value}"
}

val tagOrHash = Def.setting {
  if (isSnapshot.value) sys.process.Process("git rev-parse HEAD").lineStream_!.head
  else tagName.value
}

val unusedWarnings = Seq("-Ywarn-unused", "-Ywarn-unused-import")

val scalapbCirce = crossProject(JVMPlatform, JSPlatform)
  .in(file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    commonSettings,
    mappings in (Compile, packageSrc) ++= (managedSources in Compile).value.map { f =>
      // https://github.com/sbt/sbt-buildinfo/blob/v0.7.0/src/main/scala/sbtbuildinfo/BuildInfoPlugin.scala#L58
      val buildInfoDir = "sbt-buildinfo"
      val path = if (f.getAbsolutePath.contains(buildInfoDir)) {
        (file(buildInfoPackage.value) / f
          .relativeTo((sourceManaged in Compile).value / buildInfoDir)
          .get
          .getPath).getPath
      } else {
        f.relativeTo((sourceManaged in Compile).value).get.getPath
      }
      (f, path)
    },
    buildInfoPackage := "scalapb_circe",
    buildInfoObject := "ScalapbCirceBuildInfo",
    buildInfoKeys := Seq[BuildInfoKey](
      "scalapbVersion" -> scalapbVersion,
      circeVersion,
      scalapbJsonCommonVersion,
      scalaVersion,
      version
    )
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
    buildInfoKeys ++= Seq[BuildInfoKey](
      "scalajsVersion" -> scalaJSVersion
    ),
    scalacOptions += {
      val a = (baseDirectory in LocalRootProject).value.toURI.toString
      val g = "https://raw.githubusercontent.com/scalapb-json/scalapb-circe/" + tagOrHash.value
      s"-P:scalajs:mapSourceURI:$a->$g/"
    },
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
  unmanagedResources in Compile += (baseDirectory in LocalRootProject).value / "LICENSE.txt",
  resolvers += Opts.resolver.sonatypeReleases,
  scalaVersion := Scala211,
  crossScalaVersions := Seq("2.12.4", Scala211, "2.10.7"),
  scalacOptions ++= PartialFunction
    .condOpt(CrossVersion.partialVersion(scalaVersion.value)) {
      case Some((2, v)) if v >= 11 => unusedWarnings
    }
    .toList
    .flatten,
  Seq(Compile, Test).flatMap(c => scalacOptions in (c, console) --= unusedWarnings),
  scalacOptions ++= Seq("-feature", "-deprecation", "-language:existentials"),
  description := "Json/Protobuf convertors for ScalaPB",
  licenses += ("MIT", url("https://opensource.org/licenses/MIT")),
  organization := "io.github.scalapb-json",
  name := UpdateReadme.scalapbCirceName,
  Project.inConfig(Test)(sbtprotoc.ProtocPlugin.protobufConfigSettings),
  PB.targets in Compile := Nil,
  PB.protoSources in Test := Seq(file("shared/src/test/protobuf")),
  scalapbJsonCommonVersion := "0.1.1",
  circeVersion := "0.9.0",
  libraryDependencies ++= Seq(
    "io.github.scalapb-json" %%% "scalapb-json-common" % scalapbJsonCommonVersion.value,
    "com.trueaccord.scalapb" %%% "scalapb-runtime" % scalapbVersion % "protobuf,test",
    "io.circe" %%% "circe-parser" % circeVersion.value,
    "org.scalatest" %%% "scalatest" % "3.0.4" % "test"
  ),
  pomExtra in Global := {
    <url>https://github.com/scalapb-json/scalapb-circe</url>
      <scm>
        <connection>scm:git:github.com/scalapb-json/scalapb-circe.git</connection>
        <developerConnection>scm:git:git@github.com:scalapb-json/scalapb-circe.git</developerConnection>
        <url>github.com/scalapb-json/scalapb-circe.git</url>
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
      s"https://github.com/scalapb-json/scalapb-circe/tree/${t}€{FILE_PATH}.scala"
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

val scalapbCirceJVM = scalapbCirce.jvm
val scalapbCirceJS = scalapbCirce.js
