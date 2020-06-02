val circeVersion = "0.12.3"

lazy val commonSettings = Seq(
  scalaVersion := "2.13.2",
  organization := "com.ponte",
  libraryDependencies ++= Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % circeVersion)
)

lazy val server = (project in file("server")).settings(
  commonSettings,
  name := "Server",
  scalaJSProjects := Seq(samurai),
  pipelineStages in Assets := Seq(scalaJSPipeline),
  compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
  libraryDependencies ++= Seq(
    guice,
    "com.vmunier" %% "scalajs-scripts" % "1.1.4",
    "com.typesafe.play" %% "play-slick" % "5.0.0",
    "org.postgresql" % "postgresql" % "42.2.12",
    "org.mindrot" %"jbcrypt" % "0.4",
    "com.typesafe.slick" %% "slick-codegen" % "3.3.2",
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
  )
).enablePlugins(PlayScala).dependsOn(shared.jvm)

lazy val samurai = (project in file("samurai-scalajs")).settings(
  commonSettings,
  name := "Samurai-Client",
  scalaJSUseMainModuleInitializer := true,
  libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "1.0.0"
).enablePlugins(ScalaJSPlugin, ScalaJSWeb)
  .dependsOn(shared.js)

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared")).
  settings(
    commonSettings,
    name := "Shared"
  )

onLoad in Global := (onLoad in Global).value andThen {s: State => "project server" :: s}
