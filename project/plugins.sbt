addSbtPlugin(
  "com.disneystreaming.smithy4s" % "smithy4s-sbt-codegen" % sys.env
    .getOrElse("SMITHY_VERSION", "0.17.2")
)
addSbtPlugin("io.spray"         % "sbt-revolver"        % "0.9.1")
addSbtPlugin("com.github.sbt"   % "sbt-native-packager" % "1.9.13")
addSbtPlugin("com.eed3si9n"     % "sbt-projectmatrix"   % "0.9.0")
addSbtPlugin("org.scala-js"     % "sbt-scalajs"         % "1.12.0")
addSbtPlugin("com.timushev.sbt" % "sbt-updates"         % "0.6.4")
addSbtPlugin("org.scalameta"    % "sbt-scalafmt"        % "2.5.0")
addSbtPlugin("com.armanbilge"   % "sbt-bundlemon"       % "0.1.3")
