package com.spark2.hive

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.SaveMode

/*
		create EXTERNAL table employee_ext(empid Int, name String, dept String, salary double, nop Int) 
		PARTITIONED BY(dttime String)ROW FORMAT DELIMITED FIELDS TERMINATED BY "," stored as TEXTFILE 
		LOCATION '/user/centos/hive/emp_ext';
	*/

object Save_To_Hive_Partitioned_External_Table {

  case class Employee(empid: Int, name: String, dept: String, salary: Double, nop: Int, dttime: String)

  def main(args: Array[String]) {

    // warehouseLocation points to the default location for managed databases and tables
    val warehouseLocation = "file:${system:user.dir}/spark-warehouse"

    val spark = SparkSession.builder.
      master("local[2]")
      .appName("Save_To_Hive_Partitioned_External_Table-Example")
      .enableHiveSupport()
      .config("hive.exec.dynamic.partition", "true") //set this parameters for dynamic partitioning
      .config("hive.exec.dynamic.partition.mode", "nonstrict")
      .config("spark.sql.warehouse.dir", warehouseLocation)
      .config("hive.metastore.warehouse.dir", "/user/hive/warehouse")
      .getOrCreate()

    import spark.implicits._

    val empDF = Seq(
      Employee(123, "revanth", "cloud", 1000, 2, "07-06-2016-06-08-27"),
      Employee(124, "shyam", "finance", 3000, 2, "07-06-2016-06-08-27"),
      Employee(125, "hari", "TAG", 6000, 2, "07-06-2016-06-08-27"),
      Employee(126, "kiran", "cloud", 2000, 2, "08-06-2016-07-08-27"),
      Employee(127, "nandha", "sales", 1000, 2, "08-06-2016-07-08-27"),
      Employee(128, "pawan", "cloud", 1000, 2, "08-06-2016-07-08-27"),
      Employee(129, "kalyan", "conectivity", 1000, 2, "09-06-2016-08-08-27"),
      Employee(121, "satish", "finance", 1000, 2, "09-06-2016-08-08-27"),
      Employee(131, "arun", "cloud", 1000, 2, "09-06-2016-08-08-27"),
      Employee(132, "ram", "cloud", 1000, 2, "10-06-2016-08-08-27"),
      Employee(133, "suda", "conectivity", 1000, 2, "10-06-2016-08-08-27"),
      Employee(134, "sunder", "sales", 1000, 2, "10-06-2016-08-08-27"),
      Employee(135, "charan", "TAG", 1000, 2, "12-06-2016-08-08-27"),
      Employee(136, "ravi", "TAG", 1000, 2, "11-06-2016-08-08-27"),
      Employee(137, "arjun", "cloud", 1000, 2, "11-06-2016-08-08-27")).toDF()

    empDF.coalesce(1).write.mode(SaveMode.Append).insertInto("employee_ext")

    import spark.sql

    //loading the data from the table 
    sql("SELECT * FROM employee_ext").show()
    //or
    val employeeExtDf = spark.read.table("employee_ext")
    employeeExtDf.show

    // selecting data from a specific partition
    sql("select name,dept,dttime from employee_ext where dttime='07-06-2016-06-08-27'").show()
	  
    //To list the partitions of an existing table
    sql("SHOW PARTITIONS employee_ext").show()

    //If you want to add an PARTITION to the external table for rolling data 
    sql("ALTER TABLE employee_ext ADD IF NOT EXISTS PARTITION(dttime = '07-06-2016-06-06-02') LOCATION '/user/centos/hive/emp_ext/'")


  }
}
