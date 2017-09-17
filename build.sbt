
val libdeps = Seq(
//  "org.apache.spark" % "spark-core_2.11" % "2.2.0",
  "org.apache.spark" % "spark-sql_2.11" % "2.2.0",
  "org.apache.spark" % "spark-streaming_2.11" % "2.2.0",
  "org.apache.spark" % "spark-streaming-kafka-0-10_2.11" % "2.2.0",
  "com.datastax.spark" % "spark-cassandra-connector_2.11" % "2.0.5",
  "com.typesafe" % "config" % "1.3.1",
  "com.typesafe.play" %% "play-json" % "2.6.3"
)



lazy val root = (project in file("."))
  .enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)
  .settings(
    Seq(
      name := "tradr-logger",
      organization := "tradr",
      version := "1.0.0",
      scalaVersion := "2.11.11",
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


