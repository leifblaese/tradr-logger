
assemblyMergeStrategy in assembly := {
  case PathList("org", "apache", "commons", xs @ _*) => MergeStrategy.first
  case PathList("org", "apache", "spark", "unused", xs @ _*) => MergeStrategy.first
  case PathList("org", "apache", "hadoop", "yarn", xs @ _*) => MergeStrategy.first
  case PathList("org", "slf4j", xs @ _*) => MergeStrategy.first
  case PathList("org", "aopalliance", xs @ _*) => MergeStrategy.first
  case PathList("overview.html", xs @ _*) => MergeStrategy.first
  case PathList("javax", "inject", xs @ _*) => MergeStrategy.first
//  case PathList("shadeio", "common", xs @ _*) => MergeStrategy.last
  case PathList("META-INF", "io.netty.versions.properties", xs @ _*) => MergeStrategy.last
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}