#! /bin/bash -eu
FILE=./prediction-job/target/orion.spark.connector.prediction.santander.bike-1.0.1.jar
/spark/bin/spark-submit --driver-memory 4g --class  org.fiware.cosmos.orion.spark.connector.prediction.TrainingJob --master  spark://spark-master:7077 --deploy-mode client $FILE --conf "spark.driver.extraJavaOptions=-Dlog4jspark.root.logger=WARN,console"