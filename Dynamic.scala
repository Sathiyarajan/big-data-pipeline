package com.dynamiclists

import scala.collection.JavaConverters.mutableMapAsJavaMapConverter
import scala.collection.mutable.LinkedHashMap

object Dynamic {
  def main(args: Array[String]): Unit = {

    val l1 = List("key1", "val1", "key2", "val2")
    val l2 = List("key1", "val1", "key2", "val2")

    var a = ""
    var nodes = new LinkedHashMap[String, Any]().asJava
    for (a <- 0 to l1.length - 1) {
      var label = "VALUE" ++ Integer.toString(a)
      nodes.put(label, new scala.collection.mutable.ArrayBuffer[String]().toList.addString(new   StringBuilder(l1(a))))
      println(nodes.get(label))
    }
    for (a <- 0 to l2.length - 1) {
      var label = "VALUE" ++ Integer.toString(a)
      nodes.put(label, new scala.collection.mutable.ArrayBuffer[String]().toList.addString(new   StringBuilder(l2(a))))
      println(nodes.get(label))
    }
    println(nodes.keySet.toString())
  }
}