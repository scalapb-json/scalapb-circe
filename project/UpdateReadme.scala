import sbt._, Keys._
import sbtrelease.ReleasePlugin.autoImport.ReleaseStep
import sbtrelease.Git

object UpdateReadme {

  val scalapbCirceName = "scalapb-circe"

  val updateReadmeTask = { state: State =>
    val extracted = Project.extract(state)
    val v = extracted get version
    val org = extracted get organization
    val modules = scalapbCirceName :: Nil
    val readme = "README.md"
    val readmeFile = file(readme)
    val newReadme = Predef
      .augmentString(IO.read(readmeFile))
      .lines
      .map { line =>
        val matchReleaseOrSnapshot = line.contains("SNAPSHOT") == v.contains("SNAPSHOT")
        if (line.startsWith("libraryDependencies") && matchReleaseOrSnapshot) {
          val i = modules.map("\"" + _ + "\"").indexWhere(line.contains)
          if (line.contains(" %%% ")) {
            s"""libraryDependencies += "$org" %%% "${modules(i)}" % "$v""""
          } else {
            s"""libraryDependencies += "$org" %% "${modules(i)}" % "$v""""
          }
        } else {
          line
        }
      }
      .mkString("", "\n", "\n")
    IO.write(readmeFile, newReadme)
    val git = new Git(extracted get baseDirectory)
    git.add(readme) ! state.log
    git.commit(message = "update " + readme, sign = false) ! state.log
    sys.process.Process("git diff HEAD^") ! state.log
    state
  }

  val updateReadmeProcess: ReleaseStep = updateReadmeTask
}
