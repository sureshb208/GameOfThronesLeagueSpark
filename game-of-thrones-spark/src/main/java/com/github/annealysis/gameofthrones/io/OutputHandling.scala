package com.github.annealysis.gameofthrones.io

import java.time.{LocalDate, ZoneId}

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.col

object OutputHandling {

  def combinePreviousScores(df: DataFrame, week: Int, bucketPath: String)(implicit spark: SparkSession): DataFrame = {

    val now = LocalDate.now(ZoneId.of("PST"))

    // rename columns to be more aesthetically pleasing for final results
    val renamedDF = df.withColumnRenamed("score", s"Episode $week Score")
      .withColumnRenamed("rank", s"Episode $week Rank")
      .withColumnRenamed("team", "Team")
      .withColumnRenamed("payType", "Iron Bank")

    if (week == 1) return renamedDF

    val oldResultsDF = spark.read.csv(bucketPath)
    val resultsFileSplit = bucketPath.split("\\.")(0)
    oldResultsDF.repartition(1).write
      .mode("overwrite").option("header", "true").csv(s"${resultsFileSplit}_$now.csv")

    val movementName = "Movement from Previous Episode"
    val oldResultsDroppedDF = oldResultsDF.drop(movementName)
    val combinedResultsDF = df.join(oldResultsDroppedDF, Seq("Team", "Iron Bank"), "left")
      .withColumn(movementName, col(s"Episode $week Rank") - col(s"Episode ${week - 1} Rank"))

    val colNames = combinedResultsDF.columns
    val firstHalfNames = colNames.slice(0, 3) :+ movementName
    val newNames = firstHalfNames ++ colNames.slice(3, colNames.length - 1)

    combinedResultsDF.select(newNames.head, newNames.tail: _*)

  }

  def writeScoresToFile(df: DataFrame, bucketPath: String)(implicit spark: SparkSession): Unit = {
    df.repartition(1).write.mode("overwrite").option("header", "true").csv(bucketPath)


  }

}
