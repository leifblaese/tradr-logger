package tradr.logger

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

object Spark {

  val conf: SparkConf = new SparkConf()
    .setMaster("localhost[1]")
    .setAppName("tradr-logger")

  val session = SparkSession
    .builder()
    .config(conf)
    .getOrCreate()

}
