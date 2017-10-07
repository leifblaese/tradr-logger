
val libdeps = Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.19",
  "com.lightbend.akka" %% "akka-stream-alpakka-cassandra" % "0.11",
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.17",
  "org.slf4j" % "slf4j-simple" % "1.7.25",
  "tradr" %% "tradr-common" % "0.0.1"
)



lazy val root = (project in file("."))
  .enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)
  .settings(
    Seq(
      name := "tradr-logger",
      organization := "tradr",
      version := "0.0.1",
      scalaVersion := "2.12.2",
      libraryDependencies ++= libdeps,
      assemblyJarName in assembly := "tradr-logger.jar"
    )
  )


val productionConfFileSource = new File("/home/leifblaese/Dropbox/Privat/Tradr/production.conf")

dockerfile in docker := {
  val artifact: File = assembly.value
  val targetDir = "/opt/tradr-logger"

  new Dockerfile {
    from("java")
    runRaw("mkdir /opt/tradr-logger")
    add(artifact, s"${targetDir}/${artifact.getName}")
    copy(productionConfFileSource, "/opt/tradr-logger")
    runRaw("ls /opt/tradr-logger")
    runRaw("cat /opt/tradr-logger/production.conf")
    entryPoint("java", s"-Dconfig.file=/opt/tradr-logger/production.conf", "-jar", "/opt/tradr-logger/tradr-logger.jar")
  }

//  buildOptions in docker := BuildOptions(cache=false)
}


