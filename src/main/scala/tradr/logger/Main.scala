package tradr.logger

import com.typesafe.config.ConfigFactory


object Main extends App {

  val conf = ConfigFactory.load()
  val logger = new CassandraLogger(conf)
  logger.startLogging

}
