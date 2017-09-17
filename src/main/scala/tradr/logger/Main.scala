package tradr.logger

import com.datastax.spark.connector.SomeColumns
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import play.api.libs.json.{JsValue, Json}
import tradr.common.CurrencyPoint
import org.apache.spark.sql.cassandra._
import com.datastax.spark.connector._
import org.apache.spark.internal.Logging

import scala.collection.JavaConverters._

object Main extends App {


  val m = new Main()
  m.run

  def getKafkaStream(ssc: StreamingContext, conf: Config): InputDStream[ConsumerRecord[String, String]] = {

    val kafkaBootstrapServers = conf.getString("kafka.ip") +
      ":" + conf.getStringList("kafka.port").get(0)
    val kafkaGroupID = conf.getString("kafka.groupId")
    val kafkaTopics: Array[String] = conf
      .getStringList("tradr.trader.allSymbols")
      .asScala
      .toArray

    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> kafkaBootstrapServers,
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> kafkaGroupID,
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )

    KafkaUtils.createDirectStream[String, String](
      ssc,
      PreferConsistent,
      Subscribe[String, String](kafkaTopics, kafkaParams)
    )

  }

}

class Main extends Logging {
  import Main._

  val ssc = new StreamingContext(Spark.session.sparkContext, Seconds(1))
  val conf: Config = ConfigFactory.load()

  val keyspaceName = conf.getString("cassandra.currencyKeyspace")
  val tableName = conf.getString("cassandra.currencyTable")


  def run = {
    val kafkaStream = getKafkaStream(ssc, conf)

    kafkaStream
      .map(record => record.value())
      .map(jsonCurrencyPair => Json.parse(jsonCurrencyPair).as[CurrencyPoint])
      .map(point => {logInfo(point.toString); point})
      .foreachRDD((data: RDD[CurrencyPoint]) => data.saveToCassandra(
        keyspaceName = keyspaceName,
        tableName = tableName,
        columns = SomeColumns("timestamp", "currencyPair", "value")
      ))


    ssc.start()


  }
}
