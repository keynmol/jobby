import org.scalajs.linker.interface.{ModuleSplitStyle, Report}
import smithy4s.codegen.Smithy4sCodegenPlugin

Global / onChangedBuildSource := ReloadOnSourceChanges

val Versions = new {
  val http4sBlaze       = "0.23.17"
  val http4s            = "0.23.30"
  val Scala             = "3.6.3"
  val skunk             = "1.0.0-M8"
  val upickle           = "3.3.1"
  val scribe            = "3.16.0"
  val http4sDom         = "0.2.11"
  val jwt               = "10.0.1"
  val FlywayPG          = "11.1.1"
  val Postgres          = "42.7.5"
  val TestContainers    = "0.41.5"
  val Weaver            = "0.8.4"
  val WeaverPlaywright  = "0.0.5"
  val Laminar           = "17.2.0"
  val waypoint          = "9.0.0"
  val scalacss          = "1.0.0"
  val monocle           = "3.3.0"
  val circe             = "0.14.10"
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
      "org.http4s"    %% "http4s-blaze-server"        % Versions.http4sBlaze,
      "org.http4s"    %% "http4s-ember-server"        % Versions.http4s,
      "org.postgresql" % "postgresql"                 % Versions.Postgres,
      "org.flywaydb"   % "flyway-database-postgresql" % Versions.FlywayPG,
    ),
    Compile / resourceGenerators += {
      Def.task[Seq[File]] {
        copyAll(
          frontendBundle.value,
          (Compile / resourceManaged).value / "assets",
        )
      }
    },
    reStart / baseDirectory := (ThisBuild / baseDirectory).value,
    run / baseDirectory     := (ThisBuild / baseDirectory).value,
  )

def copyAll(location: File, outDir: File) = {
  IO.listFiles(location).toList.map { file =>
    val (name, ext) = file.baseAndExt
    val out         = outDir / (name + "." + ext)

    IO.copyFile(file, out)

    out
  }
}

val scalacSettings = Seq(
  scalacOptions += "-Wunused:all",
)

lazy val backend = projectMatrix
  .in(file("modules/backend"))
  .dependsOn(shared)
  .defaultAxes(defaults*)
  .jvmPlatform(Seq(Versions.Scala))
  .settings(
    scalaVersion := Versions.Scala,
    scalacSettings,
    Compile / doc / sources := Seq.empty,
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger" % smithy4sVersion.value,
      "com.github.jwt-scala" %% "jwt-upickle"  % Versions.jwt,
      "com.lihaoyi"          %% "upickle"      % Versions.upickle,
      "com.outr"             %% "scribe"       % Versions.scribe,
      "com.outr"             %% "scribe-cats"  % Versions.scribe,
      "com.outr"             %% "scribe-slf4j" % Versions.scribe,
      "org.tpolecat"         %% "skunk-core"   % Versions.skunk,
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
        "org.flywaydb" % "flyway-database-postgresql" % Versions.FlywayPG,
      ).map(_ % Test),
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    Test / fork          := true,
    Test / baseDirectory := (ThisBuild / baseDirectory).value,
    Test / resourceGenerators += {
      Def.task[Seq[File]] {
        copyAll(
          frontendBundle.value,
          (Test / resourceManaged).value / "assets",
        )
      }
    },
  )

lazy val shared = projectMatrix
  .in(file("modules/shared"))
  .defaultAxes(defaults*)
  .jvmPlatform(Seq(Versions.Scala))
  .jsPlatform(Seq(Versions.Scala))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalacSettings,
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %%% "smithy4s-http4s" % smithy4sVersion.value,
      "com.indoorvivants" %%% "scala-uri" % "4.1.0",
    ),
    Compile / doc / sources := Seq.empty,
  )

lazy val frontend = projectMatrix
  .in(file("modules/frontend"))
  .customRow(
    Seq(Versions.Scala),
    axisValues = Seq(VirtualAxis.js, BuildStyle.SingleFile),
    Seq.empty,
  )
  .customRow(
    Seq(Versions.Scala),
    axisValues = Seq(VirtualAxis.js, BuildStyle.Modules),
    Seq.empty,
  )
  .defaultAxes((defaults :+ VirtualAxis.js)*)
  .dependsOn(shared)
  .enablePlugins(ScalaJSPlugin, BundleMonPlugin)
  .settings(
    scalacSettings,
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig := {
      val config = scalaJSLinkerConfig.value
      import org.scalajs.linker.interface.OutputPatterns
      if (virtualAxes.value.contains(BuildStyle.SingleFile)) config
      else
        config
          .withModuleSplitStyle(
            ModuleSplitStyle
              .SmallModulesFor(List(s"${Config.BasePackage}.frontend")),
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
      "org.scala-js" %%% "scala-js-macrotask-executor" % Versions.macroTaskExecutor,
    ),
  )

lazy val defaults =
  Seq(VirtualAxis.scalaABIVersion(Versions.Scala), VirtualAxis.jvm)

lazy val frontendModules = taskKey[(Report, File)]("")
ThisBuild / frontendModules := Def.taskIf {
  def proj = frontend.finder(BuildStyle.Modules)(
    Versions.Scala,
  )

  if (isRelease)
    (proj / Compile / fullLinkJS).value.data ->
      (proj / Compile / fullLinkJS / scalaJSLinkerOutputDirectory).value
  else
    (proj / Compile / fastLinkJS).value.data ->
      (proj / Compile / fastLinkJS / scalaJSLinkerOutputDirectory).value
}.value

lazy val frontendBundle = taskKey[File]("")
ThisBuild / frontendBundle := Def.taskIf {
  def proj = frontend.finder(BuildStyle.SingleFile)(
    Versions.Scala,
  )

  if (isRelease) {
    val res = (proj / Compile / fullLinkJS).value
    (proj / Compile / fullLinkJS / scalaJSLinkerOutputDirectory).value
  } else {
    val res = (proj / Compile / fastLinkJS).value
    (proj / Compile / fastLinkJS / scalaJSLinkerOutputDirectory).value
  }
}.value

lazy val isRelease = sys.env.get("RELEASE").contains("yesh")

addCommandAlias("publishDocker", "app/Docker/publishLocal")
addCommandAlias("stubTests", "backend/testOnly jobby.tests.stub.*")
addCommandAlias("unitTests", "backend/testOnly jobby.tests.unit.*")
addCommandAlias(
  "fastTests",
  "backend/testOnly jobby.tests.stub.* jobby.tests.unit.*",
)
addCommandAlias(
  "integrationTests",
  "backend/testOnly jobby.tests.integration.*",
)
addCommandAlias(
  "frontendTests",
  "backend/testOnly jobby.tests.frontend.*",
)

val scalafixRules = Seq(
  "OrganizeImports",
  "DisableSyntax",
  "LeakingImplicitClassVal",
  "NoValInForComprehension",
).mkString(" ")

val CICommands = Seq(
  "clean",
  "scalafixEnable",
  "compile",
  "test",
  "scalafmtCheckAll",
  "scalafmtSbtCheck",
  s"scalafix --check $scalafixRules",
).mkString(";")

val PrepareCICommands = Seq(
  "scalafixEnable",
  s"scalafix --rules $scalafixRules",
  s"Test/scalafix --rules $scalafixRules",
  "scalafmtAll",
  "scalafmtSbt",
).mkString(";")

addCommandAlias("ci", CICommands)

addCommandAlias("preCI", PrepareCICommands)

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
      Tags.limitAll(4),
    )
  } else Seq.empty
}
