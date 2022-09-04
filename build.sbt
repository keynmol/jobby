import org.scalajs.linker.interface.Report
import org.scalajs.linker.interface.ModuleSplitStyle
import smithy4s.codegen.Smithy4sCodegenPlugin

Global / onChangedBuildSource := ReloadOnSourceChanges

val Versions = new {
  val http4sBlaze    = "0.23.12"
  val http4s         = "0.23.15"
  val Scala          = "3.1.3"
  val skunk          = "0.3.1"
  val upickle        = "2.0.0"
  val scribe         = "3.10.3"
  val http4sDom      = "0.2.3"
  val jwt            = "9.1.1"
  val Flyway         = "9.2.2"
  val Postgres       = "42.5.0"
  val TestContainers = "0.40.10"
  val Weaver         = "0.7.15"
  val Laminar        = "0.14.2"
  val waypoint       = "0.5.0"
  val scalacss       = "1.0.0"
  val monocle        = "3.1.0"
  val circe          = "0.14.2"
}

lazy val root = project
  .in(file("."))
  .aggregate(
    (backend.projectRefs ++ shared.projectRefs ++ frontend.projectRefs)*
  )

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
    dockerBaseImage         := "eclipse-temurin:17",
    Docker / packageName    := "jobby-smithy4s",
    libraryDependencies ++= Seq(
      "org.http4s"    %% "http4s-blaze-server" % Versions.http4sBlaze,
      "org.http4s"    %% "http4s-ember-server" % Versions.http4s,
      "org.postgresql" % "postgresql"          % Versions.Postgres,
      "org.flywaydb"   % "flyway-core"         % Versions.Flyway
    ),
    Compile / resourceGenerators += {
      if (isRelease)
        Def.task[Seq[File]] {
          val (_, location) = (ThisBuild / frontendOutput).value

          val outDir = (Compile / resourceManaged).value / "assets"
          IO.listFiles(location).toList.map { file =>
            val (name, ext) = file.baseAndExt
            val out         = outDir / (name + "." + ext)

            IO.copyFile(file, out)

            out
          }
        }
      else Def.task { Seq.empty[File] }
    },
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    Test / fork             := true,
    reStart / baseDirectory := (ThisBuild / baseDirectory).value,
    run / baseDirectory     := (ThisBuild / baseDirectory).value
  )

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
    Test / fork := true
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
      "io.lemonlabs" %%% "scala-uri" % "4.0.2"
    ),
    Compile / doc / sources := Seq.empty
  )

lazy val frontend = projectMatrix
  .in(file("modules/frontend"))
  .jsPlatform(Seq(Versions.Scala))
  .defaultAxes(defaults*)
  .dependsOn(shared)
  .enablePlugins(ScalaJSPlugin, BundleMonPlugin)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= { config =>
      import org.scalajs.linker.interface.OutputPatterns
      if (isRelease) config
      else
        config
          .withModuleSplitStyle(
            ModuleSplitStyle.SmallModulesFor(List("frontend"))
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
      "org.http4s"                   %%% "http4s-dom"   % Versions.http4sDom
    )
  )

lazy val defaults =
  Seq(VirtualAxis.scalaABIVersion(Versions.Scala), VirtualAxis.jvm)

lazy val frontendOutput = taskKey[(Report, File)]("")

lazy val frontendJS = frontend.js(Versions.Scala)

ThisBuild / frontendOutput := {
  if (isRelease)
    (frontendJS / Compile / fullLinkJS).value.data ->
      (frontendJS / Compile / fullLinkJS / scalaJSLinkerOutputDirectory).value
  else
    (frontendJS / Compile / fastLinkJS).value.data ->
      (frontendJS / Compile / fastLinkJS / scalaJSLinkerOutputDirectory).value
}

lazy val isRelease = sys.env.get("RELEASE").contains("yesh")

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

lazy val buildFrontend = taskKey[Unit]("")

buildFrontend := {
  val (_, folder) = frontendOutput.value
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
