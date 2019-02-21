import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.hive.HiveContext

val conf = new SparkConf().setAppName("Test").setMaster("yarn-client")
val sc = new SparkContext(conf)
val sqlContext = new HiveContext(sc)
import sqlContext.implicits._

val resultDF = sqlContext.sql("select * from type1_test.contacts_target")
