#! /bin/bash -eu

/spark/bin/spark-submit --packages org.mongodb.spark:mongo-spark-connector_2.11:2.4.3 --class  org.fiware.cosmos.orion.spark.connector.prediction.PredictionJob --master  spark://spark-master:7077 --deploy-mode client ./prediction-job/target/orion.spark.connector.prediction.santander.bike-1.0.1.jar --conf "spark.driver.extraJavaOptions=-Dlog4jspark.root.logger=WARN,console"

