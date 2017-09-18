package tradr.logger

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.{ActorMaterializer, KillSwitches, UniqueKillSwitch}
import akka.stream.alpakka.cassandra.scaladsl.CassandraSink
import akka.stream.scaladsl.{Keep, RunnableGraph, Sink}
import com.datastax.driver.core.{Cluster, PreparedStatement, Session}
import com.google.common.util.concurrent.ListenableFuture
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import play.api.libs.json.Json
import tradr.common.CurrencyPoint

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}


object CassandraLogger {

  private[this] def getConsumerSettings(conf: Config)(implicit system: ActorSystem) = {
    val kafkaBootstrapServers = conf.getString("kafka.ip") +
      ":" + conf.getStringList("kafka.port").get(0)

    val kafkaGroupID = conf.getString("kafka.groupId")

    ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers(kafkaBootstrapServers)
      .withGroupId("group1")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
  }

  private[this] def getCassandraSession(conf: Config): Session = {

    val cassandraContactPoint = conf.getString("cassandra.ip")
    val port = conf.getStringList("cassandra.port").get(0).toInt

    Cluster
      .builder
      .addContactPoint(cassandraContactPoint)
      .withPort(port)
      .build
      .connect()
  }

  private def getCassandraSink(conf: Config)(implicit ec: ExecutionContext): Sink[CurrencyPoint, Future[Done]] = {
    implicit val session = getCassandraSession(conf)

    val keyspaceName = conf.getString("cassandra.currencyKeyspace")
    val tableName = conf.getString("cassandra.currencyTable")



    val preparedStatement = session.prepare(s"INSERT INTO $keyspaceName.$tableName VALUES (?, ?)")
    val statementBinder =
      (point: CurrencyPoint, statement: PreparedStatement) =>
        statement.bind(
          point.timestamp, point.currencyPair, point.value
        )

    CassandraSink.apply[CurrencyPoint](parallelism = 2, preparedStatement, statementBinder)
  }

  def getStream(conf: Config, topics: Set[String])
               (implicit system: ActorSystem, ec: ExecutionContext) = {

    val consumerSettings = getConsumerSettings(conf)(system)
    val cassandraSink = getCassandraSink(conf)

    Consumer
      .committableSource(consumerSettings, Subscriptions.topics(topics))
      .map(msg => msg.record.value())
      .map(currencyPointString => Json.parse(currencyPointString).as[CurrencyPoint])
      .toMat(cassandraSink)(Keep.right)

  }
}

case class CassandraLogger(conf: Config) {
  import CassandraLogger._

  def startLogging = {
    implicit val system: ActorSystem = ActorSystem("tradr-logger")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContextExecutor = ExecutionContext.global

    val topics: Set[String] = conf
      .getStringList("tradr.trader.allSymbols")
      .asScala
      .toSet[String]

    val stream = getStream(conf, topics)
    stream.run()



  }

}
