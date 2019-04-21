package com.github.annealysis.gameofthrones

import com.github.annealysis.gameofthrones.calculations.Calculations
import com.github.annealysis.gameofthrones.common.Spark
import com.github.annealysis.gameofthrones.io.{InputHandling, OutputHandling}
import com.typesafe.scalalogging.StrictLogging

class Score extends StrictLogging with Spark {

  import Score._

  def run(bucket: String, week: Int, createAnswerFlag: Boolean): Unit = {

    logger.info(s"This is episode ${week}.")

    logger.info("Reading in responses...")
    val (responsesDF, questionsDF) = InputHandling(s"$bucket/$responsesFile")

    responsesDF.show(5)
    questionsDF.show(5)

    if (createAnswerFlag) {
      logger.info("Creating template for correct answers to be populated... ")
      questionsDF.write.csv(s"$bucket/$answerStructureFile")
    }

    logger.info("Reading in correct answers...")
    val correctAnswerDF = spark.read.csv(s"$bucket/$correctAnswersFile")

    logger.info("Scoring the responses... ")
    val scoredDF = Calculations(responsesDF, correctAnswerDF)

    logger.info("Combining previous weeks' scores, if applicable ... ")
    val combinedWeeksScoreDF = OutputHandling.combinePreviousScores(scoredDF)

    logger.info("Writing output to file... ")
    OutputHandling.writeScoresToFile(combinedWeeksScoreDF)

    logger.info("Done! ")

  }

}

object Score extends Score with App {

  val responsesFile = "FantasyGameofThronesResponses.csv"
  val correctAnswersFile = "answer_truth.xlsx"
  val resultsFile = "Results.csv"
  val answerStructureFile = "answer_structure.csv"


  run(
    bucket = args(0),
    week = args(1).toInt,
    createAnswerFlag = args(2).toBoolean
  )


}