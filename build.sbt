import scalapb.compiler.Version._
import sbtrelease.ReleaseStateTransformations._
import sbtcrossproject.CrossPlugin.autoImport.crossProject

val Scala212 = "2.12.21"
val circeVersion = settingKey[String]("")
val scalapbJsonCommonVersion = settingKey[String]("")

lazy val disableScala3 = Def.settings(
  libraryDependencies := {
    if (scalaBinaryVersion.value == "3") {
      Nil
    } else {
      libraryDependencies.value
    }
  },
  Seq(Compile, Test).map { x =>
    (x / sources) := {
      if (scalaBinaryVersion.value == "3") {
        Nil
      } else {
        (x / sources).value
      }
    }
  },
  Test / test := {
    if (scalaBinaryVersion.value == "3") {
      ()
    } else {
      (Test / test).value
    }
  },
  publish / skip := (scalaBinaryVersion.value == "3"),
)

val tagName = Def.setting {
  s"v${if (releaseUseGlobalVersion.value) (ThisBuild / version).value else version.value}"
}

val tagOrHash = Def.setting {
  if (isSnapshot.value) sys.process.Process("git rev-parse HEAD").lineStream_!.head
  else tagName.value
}

val unusedWarnings = Def.setting(
  Seq("-Ywarn-unused:imports")
)

val scalapbScala3Sources = Def.setting(
  scalaBinaryVersion.value match {
    case "2.12" =>
      false
    case _ =>
      true
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
    ),
  )
  .dependsOn(
    scalapbCirceJVM
  )

lazy val tests = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("tests"))
  .settings(
    commonSettings,
    noPublish,
  )
  .platformsSettings(JSPlatform, NativePlatform)(
    disableScala3,
  )
  .configure(_ dependsOn macros)
  .dependsOn(
    scalapbCirce % "test->test"
  )

val scalapbCirce = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("core"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    commonSettings,
    name := UpdateReadme.scalapbCirceName,
    libraryDependencies += "io.circe" %%% "circe-parser" % circeVersion.value,
    (Compile / packageSrc / mappings) ++= (Compile / managedSources).value.map { f =>
      // https://github.com/sbt/sbt-buildinfo/blob/v0.7.0/src/main/scala/sbtbuildinfo/BuildInfoPlugin.scala#L58
      val buildInfoDir = "sbt-buildinfo"
      val path = if (f.getAbsolutePath.contains(buildInfoDir)) {
        (file(buildInfoPackage.value) / f
          .relativeTo((Compile / sourceManaged).value / buildInfoDir)
          .get
          .getPath).getPath
      } else {
        f.relativeTo((Compile / sourceManaged).value).get.getPath
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
    (Test / PB.targets) ++= Seq[protocbridge.Target](
      PB.gens.java -> (Test / sourceManaged).value,
      scalapb.gen(javaConversions = true, scala3Sources = scalapbScala3Sources.value) -> (Test / sourceManaged).value
    ),
    libraryDependencies ++= Seq(
      "com.google.protobuf" % "protobuf-java-util" % "3.25.8" % "test",
      "com.google.protobuf" % "protobuf-java" % "3.25.8" % "protobuf"
    )
  )
  .jsSettings(
    buildInfoKeys ++= Seq[BuildInfoKey](
      "scalajsVersion" -> scalaJSVersion
    ),
    scalacOptions += {
      val a = (LocalRootProject / baseDirectory).value.toURI.toString
      val g = "https://raw.githubusercontent.com/scalapb-json/scalapb-circe/" + tagOrHash.value
      if (scalaBinaryVersion.value == "3") {
        "-scalajs-mapSourceURI:$a->$g/"
      } else {
        "-P:scalajs:mapSourceURI:$a->$g/"
      }
    },
  )
  .platformsSettings(JVMPlatform, JSPlatform)(
    scalapropsCoreSettings,
  )
  .platformsSettings(JSPlatform, NativePlatform)(
    (Test / PB.targets) ++= Seq[protocbridge.Target](
      scalapb.gen(javaConversions = false, scala3Sources = scalapbScala3Sources.value) -> (Test / sourceManaged).value
    )
  )

commonSettings

val noPublish = Seq(
  PgpKeys.publishLocalSigned := {},
  PgpKeys.publishSigned := {},
  publishLocal := {},
  publish := {},
  Compile / publishArtifact := false
)

noPublish

lazy val commonSettings = Def.settings(
  (Compile / unmanagedResources) += (LocalRootProject / baseDirectory).value / "LICENSE.txt",
  scalaVersion := Scala212,
  crossScalaVersions := Seq(Scala212, "2.13.18", "3.3.7"),
  scalacOptions ++= {
    if (scalaBinaryVersion.value == "3") {
      Nil
    } else {
      unusedWarnings.value
    }
  },
  scalacOptions ++= {
    scalaBinaryVersion.value match {
      case "2.12" =>
        Seq("-Xsource:3")
      case "2.13" =>
        Seq("-Xsource:3-cross")
      case _ =>
        Nil
    }
  },
  scalacOptions ++= {
    if (scalaBinaryVersion.value == "2.13") {
      Seq(
        // TODO https://github.com/scalapb/ScalaPB/issues/1576
        s"-Wconf:msg=inferred&cat=scala3-migration&src=target/scala-${scalaBinaryVersion.value}/.*:info",
      )
    } else {
      Nil
    }
  },
  Seq(Compile, Test).flatMap(c => (c / console / scalacOptions) --= unusedWarnings.value),
  scalacOptions ++= Seq("-feature", "-deprecation", "-language:existentials"),
  description := "Json/Protobuf convertors for ScalaPB",
  licenses += ("MIT", url("https://opensource.org/licenses/MIT")),
  organization := "io.github.scalapb-json",
  Project.inConfig(Test)(sbtprotoc.ProtocPlugin.protobufConfigSettings),
  Compile / PB.targets := Nil,
  (Test / PB.protoSources) := Seq(baseDirectory.value.getParentFile / "shared/src/test/protobuf"),
  scalapbJsonCommonVersion := "0.10.0",
  circeVersion := "0.14.15",
  libraryDependencies ++= Seq(
    "com.github.scalaprops" %%% "scalaprops" % "0.10.0" % "test",
    "io.circe" %%% "circe-generic" % circeVersion.value % "test",
    "io.github.scalapb-json" %%% "scalapb-json-common" % scalapbJsonCommonVersion.value,
    "com.thesamet.scalapb" %%% "scalapb-runtime" % scalapbVersion % "protobuf,test",
    "org.scalatest" %%% "scalatest-funspec" % "3.2.19" % "test",
    "org.scalatest" %%% "scalatest-flatspec" % "3.2.19" % "test",
    "org.scalatest" %%% "scalatest-freespec" % "3.2.19" % "test",
    "org.scalatest" %%% "scalatest-mustmatchers" % "3.2.19" % "test",
    "org.scalatest" %%% "scalatest-shouldmatchers" % "3.2.19" % "test",
  ),
  (Global / pomExtra) := {
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
  publishTo := (if (isSnapshot.value) None else localStaging.value),
  (Compile / doc / scalacOptions) ++= {
    val t = tagOrHash.value
    Seq(
      "-sourcepath",
      (LocalRootProject / baseDirectory).value.getAbsolutePath,
      "-doc-source-url",
      s"https://github.com/scalapb-json/scalapb-circe/tree/${t}€{FILE_PATH}.scala"
    )
  },
  compileOrder := {
    if (scalaBinaryVersion.value == "3") {
      // https://github.com/lampepfl/dotty/issues/10956
      // https://github.com/lampepfl/dotty/issues/6138
      CompileOrder.JavaThenScala
    } else {
      compileOrder.value
    }
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
        extracted.runAggregated(extracted.get(thisProjectRef) / (Global / PgpKeys.publishSigned), state)
      },
      enableCrossBuild = true
    ),
    releaseStepCommand("sonaRelease"),
    setNextVersion,
    commitNextVersion,
    UpdateReadme.updateReadmeProcess,
    pushChanges
  )
)

val scalapbCirceJVM = scalapbCirce.jvm
val scalapbCirceJS = scalapbCirce.js
val scalapbCirceNative = scalapbCirce.native
