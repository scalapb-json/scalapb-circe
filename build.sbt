import scalapb.compiler.Version._
import sbtrelease.ReleaseStateTransformations._
import sbtcrossproject.CrossPlugin.autoImport.crossProject

val Scala212 = "2.12.12"
val circeVersion = settingKey[String]("")
val scalapbJsonCommonVersion = settingKey[String]("")

val tagName = Def.setting {
  s"v${if (releaseUseGlobalVersion.value) (version in ThisBuild).value else version.value}"
}

val tagOrHash = Def.setting {
  if (isSnapshot.value) sys.process.Process("git rev-parse HEAD").lineStream_!.head
  else tagName.value
}

val unusedWarnings = Def.setting(
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 11)) =>
      Seq("-Ywarn-unused-import")
    case _ =>
      Seq("-Ywarn-unused:imports")
  }
)

lazy val macros = project
  .in(file("macros"))
  .settings(
    commonSettings,
    name := UpdateReadme.scalapbCirceMacrosName,
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-parser" % circeVersion.value, // don't use %%%
      "io.github.scalapb-json" %%% "scalapb-json-macros" % scalapbJsonCommonVersion.value,
    )
  )
  .dependsOn(
    scalapbCirceJVM
  )

lazy val tests = crossProject(JVMPlatform, JSPlatform)
  .in(file("tests"))
  .settings(
    commonSettings,
    noPublish,
  )
  .configure(_ dependsOn macros)
  .dependsOn(
    scalapbCirce % "test->test"
  )

val scalapbCirce = crossProject(JVMPlatform, JSPlatform)
  .in(file("core"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    commonSettings,
    name := UpdateReadme.scalapbCirceName,
    libraryDependencies += "io.circe" %%% "circe-parser" % circeVersion.value,
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

lazy val commonSettings = Def.settings(
  scalapropsCoreSettings,
  unmanagedResources in Compile += (baseDirectory in LocalRootProject).value / "LICENSE.txt",
  scalaVersion := Scala212,
  crossScalaVersions := Seq(Scala212, "2.13.3"),
  scalacOptions ++= unusedWarnings.value,
  Seq(Compile, Test).flatMap(c => scalacOptions in (c, console) --= unusedWarnings.value),
  scalacOptions ++= Seq("-feature", "-deprecation", "-language:existentials"),
  description := "Json/Protobuf convertors for ScalaPB",
  licenses += ("MIT", url("https://opensource.org/licenses/MIT")),
  organization := "io.github.scalapb-json",
  Project.inConfig(Test)(sbtprotoc.ProtocPlugin.protobufConfigSettings),
  PB.targets in Compile := Nil,
  PB.protoSources in Test := Seq(baseDirectory.value.getParentFile / "shared/src/test/protobuf"),
  scalapbJsonCommonVersion := "0.6.3",
  circeVersion := "0.13.0",
  libraryDependencies ++= Seq(
    "com.github.scalaprops" %%% "scalaprops" % "0.8.0" % "test",
    "io.github.scalapb-json" %%% "scalapb-json-common" % scalapbJsonCommonVersion.value,
    "com.thesamet.scalapb" %%% "scalapb-runtime" % scalapbVersion % "protobuf,test",
    "org.scalatest" %%% "scalatest" % "3.2.3" % "test"
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
  publishTo := sonatypePublishToBundle.value,
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
  releaseCrossBuild := true,
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
        extracted.runAggregated(PgpKeys.publishSigned in Global in extracted.get(thisProjectRef), state)
      },
      enableCrossBuild = true
    ),
    releaseStepCommand("sonatypeBundleRelease"),
    setNextVersion,
    commitNextVersion,
    UpdateReadme.updateReadmeProcess,
    pushChanges
  )
)

val scalapbCirceJVM = scalapbCirce.jvm
val scalapbCirceJS = scalapbCirce.js
