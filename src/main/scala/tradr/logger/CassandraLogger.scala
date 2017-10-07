package tradr.logger

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.ActorMaterializer
import akka.stream.alpakka.cassandra.scaladsl.CassandraSink
import akka.stream.scaladsl.{Keep, Sink}
import com.datastax.driver.core.{Cluster, PreparedStatement, Session}
import com.typesafe.config.Config
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import play.api.libs.json.Json
import tradr.common.PricingPoint

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}


object CassandraLogger {

  private[this] def getConsumerSettings(conf: Config)(implicit system: ActorSystem) = {
    val kafkaBootstrapServers = conf.getString("kafka.ip") +
      ":" + conf.getStringList("kafka.port").get(0)

    val kafkaGroupID = conf.getString("kafka.groupId")

    ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers(kafkaBootstrapServers)
      .withGroupId(kafkaGroupID)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
  }

  private[this] def getCassandraSession(conf: Config): Session = {

    val cassandraContactPoint = conf.getString("cassandra.ip")
    val port = conf.getString("cassandra.connectorPort").toInt

    Cluster
      .builder
      .addContactPoint(cassandraContactPoint)
      .withPort(port)
      .build
      .connect()
  }

  private def getCassandraSink(conf: Config)(implicit ec: ExecutionContext): Sink[PricingPoint, Future[Done]] = {
    implicit val session = getCassandraSession(conf)

    val keyspaceName = conf.getString("cassandra.keyspace")
    val tableName = conf.getString("cassandra.currencyTable")

    val preparedStatement = session.prepare(
      s"""INSERT INTO tradr.currencies ("timestamp", instrument, value) VALUES (?, ?, ?);"""
    )
    val statementBinder =
      (point: PricingPoint, statement: PreparedStatement) =>
        statement.bind(
          point.timestamp.asInstanceOf[AnyRef],
          point.currencyPair.asInstanceOf[AnyRef],
          point.value.asInstanceOf[AnyRef]
        )

    CassandraSink.apply[PricingPoint](parallelism = 2, preparedStatement, statementBinder)
  }

  /**
    * Subscribe to a stream from kafka and write it into a database
    * @param conf
    * @param topics
    * @param system
    * @param ec
    * @return
    */
  def getStream(conf: Config, topics: Set[String])
               (implicit system: ActorSystem, ec: ExecutionContext) = {

    val consumerSettings = getConsumerSettings(conf)(system)
    val cassandraSink = getCassandraSink(conf)

    Consumer
      .committableSource(consumerSettings, Subscriptions.topics(topics))
      .map(msg => msg.record.value())
      .map(currencyPointString => Json.parse(currencyPointString).as[PricingPoint])
      .toMat(cassandraSink)(Keep.right)

  }
}

case class CassandraLogger(conf: Config) {
  import CassandraLogger._

  def startLogging = {
    implicit val system: ActorSystem = ActorSystem("tradr-logger")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContextExecutor = ExecutionContext.global

    val topics = Set("EURUSD") // kafka topics to listen on
    val stream = getStream(conf, topics)
    stream.run()



  }

}
