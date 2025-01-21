addSbtPlugin(
  "com.disneystreaming.smithy4s" % "smithy4s-sbt-codegen" % sys.env
    .getOrElse("SMITHY_VERSION", "0.18.29"),
)

addSbtPlugin("io.spray" % "sbt-revolver" % "0.10.0")

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.11.0")

addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.10.1")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.17.0")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.4")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.4")

addSbtPlugin("com.armanbilge" % "sbt-bundlemon" % "0.1.4")

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.0")

libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
