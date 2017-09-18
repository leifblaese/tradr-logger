
val libdeps = Seq(
  "com.typesafe" % "config" % "1.3.1",
  "com.typesafe.play" %% "play-json" % "2.6.3",
  "com.typesafe.akka" % "akka-actor_2.12" % "2.5.4",
  "com.lightbend.akka" %% "akka-stream-alpakka-cassandra" % "0.11",
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.17"
)



lazy val root = (project in file("."))
  .enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)
  .settings(
    Seq(
      name := "tradr-logger",
      organization := "tradr",
      version := "1.0.0",
      scalaVersion := "2.12.2",
      libraryDependencies ++= libdeps,
      assemblyJarName in assembly := "tradr-logger.jar"
    )
  )


val productionConfFileSource = new File("/home/leifblaese/Dropbox/Privat/Tradr/production.conf")
dockerfile in docker := {
  val appDir: File = stage.value
  val targetDir = "/opt/tradr-logger"
  new Dockerfile {
    from("java")
    copy(appDir, targetDir)
    copy(productionConfFileSource, targetDir)
    entryPoint(s"$targetDir/bin/${executableScriptName.value}", s"-Dconfig.file=$targetDir/production.conf")
  }
}


