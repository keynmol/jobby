import org.scalajs.linker.interface.Report
import org.scalajs.linker.interface.ModuleSplitStyle
import smithy4s.codegen.Smithy4sCodegenPlugin

Global / onChangedBuildSource := ReloadOnSourceChanges

val Versions = new {
  val http4sBlaze       = "0.23.14"
  val http4s            = "0.23.18"
  val Scala             = "3.2.2"
  val skunk             = "0.5.1"
  val upickle           = "2.0.0"
  val scribe            = "3.11.1"
  val http4sDom         = "0.2.7"
  val jwt               = "9.2.0"
  val Flyway            = "9.16.3"
  val Postgres          = "42.6.0"
  val TestContainers    = "0.40.15"
  val Weaver            = "0.8.3"
  val WeaverPlaywright  = "0.0.5"
  val Laminar           = "15.0.1"
  val waypoint          = "6.0.0"
  val scalacss          = "1.0.0"
  val monocle           = "3.2.0"
  val circe             = "0.14.5"
  val macroTaskExecutor = "1.1.1"
}

val Config = new {
  val DockerImageName = "jobby-smithy4s"
  val DockerBaseImage = "eclipse-temurin:17"
  val BasePackage     = "jobby"
}

lazy val root = project
  .in(file("."))
  .aggregate(backend.projectRefs*)
  .aggregate(shared.projectRefs*)
  .aggregate(frontend.projectRefs*)

resolvers +=
  "Sonatype S01 OSS Snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots"

lazy val app = projectMatrix
  .in(file("modules/app"))
  .dependsOn(backend)
  .defaultAxes(defaults*)
  .jvmPlatform(Seq(Versions.Scala))
  .enablePlugins(JavaAppPackaging)
  .settings(
    scalaVersion            := Versions.Scala,
    Compile / doc / sources := Seq.empty,
    dockerBaseImage         := Config.DockerBaseImage,
    Docker / packageName    := Config.DockerImageName,
    libraryDependencies ++= Seq(
      "org.http4s"    %% "http4s-blaze-server" % Versions.http4sBlaze,
      "org.http4s"    %% "http4s-ember-server" % Versions.http4s,
      "org.postgresql" % "postgresql"          % Versions.Postgres,
      "org.flywaydb"   % "flyway-core"         % Versions.Flyway
    ),
    Compile / resourceGenerators += {
      Def.task[Seq[File]] {
        copyAll(
          frontendBundle.value,
          (Compile / resourceManaged).value / "assets"
        )
      }
    },
    reStart / baseDirectory := (ThisBuild / baseDirectory).value,
    run / baseDirectory     := (ThisBuild / baseDirectory).value
  )

def copyAll(location: File, outDir: File) = {
  IO.listFiles(location).toList.map { file =>
    val (name, ext) = file.baseAndExt
    val out         = outDir / (name + "." + ext)

    IO.copyFile(file, out)

    out
  }
}

lazy val backend = projectMatrix
  .in(file("modules/backend"))
  .dependsOn(shared)
  .defaultAxes(defaults*)
  .jvmPlatform(Seq(Versions.Scala))
  .settings(
    scalaVersion            := Versions.Scala,
    Compile / doc / sources := Seq.empty,
    libraryDependencies ++= Seq(
      ("com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value),
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger" % smithy4sVersion.value,
      "com.github.jwt-scala" %% "jwt-upickle"  % Versions.jwt,
      "com.lihaoyi"          %% "upickle"      % Versions.upickle,
      "com.outr"             %% "scribe"       % Versions.scribe,
      "com.outr"             %% "scribe-cats"  % Versions.scribe,
      "com.outr"             %% "scribe-slf4j" % Versions.scribe,
      "org.tpolecat"         %% "skunk-core"   % Versions.skunk
    ),
    libraryDependencies ++=
      Seq(
        "com.dimafeng" %% "testcontainers-scala-postgresql" % Versions.TestContainers,
        "com.indoorvivants.playwright" %% "weaver" % Versions.WeaverPlaywright,
        "com.disneystreaming" %% "weaver-cats"         % Versions.Weaver,
        "com.disneystreaming" %% "weaver-scalacheck"   % Versions.Weaver,
        "org.http4s"          %% "http4s-blaze-server" % Versions.http4sBlaze,
        "org.http4s"          %% "http4s-blaze-client" % Versions.http4sBlaze,
        "org.http4s"          %% "http4s-ember-server" % Versions.http4s,
        "org.http4s"          %% "http4s-ember-client" % Versions.http4s,
        "org.postgresql"       % "postgresql"          % Versions.Postgres,
        "org.flywaydb"         % "flyway-core"         % Versions.Flyway
      ).map(_ % Test),
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    Test / fork          := true,
    Test / baseDirectory := (ThisBuild / baseDirectory).value,
    Test / resourceGenerators += {
      Def.task[Seq[File]] {
        copyAll(
          frontendBundle.value,
          (Test / resourceManaged).value / "assets"
        )
      }
    }
  )

lazy val shared = projectMatrix
  .in(file("modules/shared"))
  .defaultAxes(defaults*)
  .jvmPlatform(Seq(Versions.Scala))
  .jsPlatform(Seq(Versions.Scala))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %%% "smithy4s-http4s" % smithy4sVersion.value,
      "io.lemonlabs" %%% "scala-uri" % "4.0.3"
    ),
    Compile / doc / sources := Seq.empty
  )

lazy val frontend = projectMatrix
  .in(file("modules/frontend"))
  .customRow(
    Seq(Versions.Scala),
    axisValues = Seq(VirtualAxis.js, BuildStyle.SingleFile),
    Seq.empty
  )
  .customRow(
    Seq(Versions.Scala),
    axisValues = Seq(VirtualAxis.js, BuildStyle.Modules),
    Seq.empty
  )
  .defaultAxes((defaults :+ VirtualAxis.js)*)
  .dependsOn(shared)
  .enablePlugins(ScalaJSPlugin, BundleMonPlugin)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig := {
      val config = scalaJSLinkerConfig.value
      import org.scalajs.linker.interface.OutputPatterns
      if (virtualAxes.value.contains(BuildStyle.SingleFile)) config
      else
        config
          .withModuleSplitStyle(
            ModuleSplitStyle
              .SmallModulesFor(List(s"${Config.BasePackage}.frontend"))
          )
          .withModuleKind(ModuleKind.ESModule)
          .withOutputPatterns(OutputPatterns.fromJSFile("%s.mjs"))
    },
    libraryDependencies ++= Seq(
      "dev.optics"                   %%% "monocle-core" % Versions.monocle,
      "com.raquo"                    %%% "waypoint"     % Versions.waypoint,
      "com.github.japgolly.scalacss" %%% "core"         % Versions.scalacss,
      "com.raquo"                    %%% "laminar"      % Versions.Laminar,
      "com.lihaoyi"                  %%% "upickle"      % Versions.upickle,
      "io.circe"                     %%% "circe-core"   % Versions.circe,
      "io.circe"                     %%% "circe-parser" % Versions.circe,
      "org.http4s"                   %%% "http4s-dom"   % Versions.http4sDom,
      "org.scala-js" %%% "scala-js-macrotask-executor" % Versions.macroTaskExecutor
    )
  )

lazy val defaults =
  Seq(VirtualAxis.scalaABIVersion(Versions.Scala), VirtualAxis.jvm)

lazy val frontendModules = taskKey[(Report, File)]("")
ThisBuild / frontendModules := (Def.taskIf {
  def proj = frontend.finder(BuildStyle.Modules)(
    Versions.Scala
  )

  if (isRelease)
    (proj / Compile / fullLinkJS).value.data ->
      (proj / Compile / fullLinkJS / scalaJSLinkerOutputDirectory).value
  else
    (proj / Compile / fastLinkJS).value.data ->
      (proj / Compile / fastLinkJS / scalaJSLinkerOutputDirectory).value
}).value

lazy val frontendBundle = taskKey[File]("")
ThisBuild / frontendBundle := (Def.taskIf {
  def proj = frontend.finder(BuildStyle.SingleFile)(
    Versions.Scala
  )

  if (isRelease) {
    val res = (proj / Compile / fullLinkJS).value
    (proj / Compile / fullLinkJS / scalaJSLinkerOutputDirectory).value
  } else {
    val res = (proj / Compile / fastLinkJS).value
    (proj / Compile / fastLinkJS / scalaJSLinkerOutputDirectory).value
  }
}).value

lazy val isRelease = sys.env.get("RELEASE").contains("yesh")

addCommandAlias("publishDocker", "app/Docker/publishLocal")
addCommandAlias("stubTests", "backend/testOnly jobby.tests.stub.*")
addCommandAlias("unitTests", "backend/testOnly jobby.tests.unit.*")
addCommandAlias(
  "fastTests",
  "backend/testOnly jobby.tests.stub.* jobby.tests.unit.*"
)
addCommandAlias(
  "integrationTests",
  "backend/testOnly jobby.tests.integration.*"
)
addCommandAlias(
  "frontendTests",
  "backend/testOnly jobby.tests.frontend.*"
)

lazy val buildFrontend = taskKey[Unit]("")

buildFrontend := {
  val (_, folder) = frontendModules.value
  val buildDir    = (ThisBuild / baseDirectory).value / "frontend-build"

  val indexHtml = buildDir / "index.html"
  val out       = folder.getParentFile() / "index.html"

  import java.nio.file.Files

  if (!Files.exists(out.toPath) || IO.read(indexHtml) != IO.read(out)) {
    IO.copyFile(indexHtml, out)
  }
}

ThisBuild / concurrentRestrictions ++= {
  if (sys.env.contains("CI")) {
    Seq(
      Tags.limitAll(4)
    )
  } else Seq.empty
}
