package org.fiware.cosmos.orion.spark.connector.prediction

import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.feature.{VectorAssembler, VectorIndexer, StringIndexer, OneHotEncoder}
import org.apache.spark.ml.regression._
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.types.{IntegerType, DoubleType, StructField, StructType}

object TrainingJob {

  def main(args: Array[String]): Unit = {
    train().write.overwrite().save("./prediction-job/model")

  }
  
  def train() = {
    // ocupation= round((1-available)*10)
    val schema = StructType(
      Array(StructField("bicicletas_disponibles", IntegerType),
            StructField("id_estacion", IntegerType),
            StructField("Ultima_medicion", IntegerType),
            StructField("Diezhora_anterior", IntegerType),
            StructField("dia", IntegerType),
            StructField("num_mes", IntegerType),
            StructField("hora", IntegerType),
            StructField("variacion_estaciones", DoubleType)                 
      ))
    val spark = SparkSession
      .builder
      .appName("TrainingBikeSantander")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._
    spark.sparkContext.setLogLevel("WARN")

    // convert to dataframe
    val data = spark.read.format("csv")
      .schema(schema)
      .option("header", "true")
      .option("delimiter", ",")
      .load("./prediction-job/santander_bike.csv")

              
    val vectorAssembler  = new VectorAssembler()
      .setInputCols(Array("id_estacion", "Ultima_medicion", "Diezhora_anterior","dia","num_mes","hora","variacion_estaciones"))
      .setOutputCol("features")



    val rfc = new DecisionTreeRegressor()
      .setMaxDepth(8)
      .setLabelCol("bicicletas_disponibles")
      .setFeaturesCol("features")

    val pipeline = new Pipeline().setStages(Array(vectorAssembler,rfc))
    val Array(trainingData,testData) = data.randomSplit(Array(0.8,0.2))
    val model = pipeline.fit(trainingData)
    val predictions = model.transform(testData)

    predictions.select("prediction","bicicletas_disponibles", "id_estacion", "dia","num_mes","hora").show(10)

    model
  }
}