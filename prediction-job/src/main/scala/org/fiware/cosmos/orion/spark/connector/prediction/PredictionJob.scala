package org.fiware.cosmos.orion.spark.connector.prediction

import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.fiware.cosmos.orion.spark.connector.{ContentType, HTTPMethod, NGSILDReceiver, OrionSink, OrionSinkObject}
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.feature.{VectorAssembler}
import org.apache.spark.ml.regression.{DecisionTreeRegressor}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.DataStreamReader
import java.util.{Date, TimeZone}
import java.text.SimpleDateFormat

import org.mongodb.scala.model.Filters.{equal, gt, and}
import com.mongodb.client.MongoClients
import com.mongodb.client.model.Sorts

case class PredictionResponse(socketId: String, predictionId: String, predictionValue: Int, idStation: Int, weekday: Int, hour: Int, month: Int) {
  override def toString :String = s"""{
  "socketId": { "value": "${socketId}", "type": "Property"},
  "predictionId": { "value":"${predictionId}", "type": "Property"},
  "predictionValue": { "value":${predictionValue}, "type": "Property"},
  "idStation": { "value":"${idStation}", "type": "Property"},
  "weekday": { "value":${weekday}, "type": "Property"},
  "hour": { "value": ${hour}, "type": "Property"},
  "month": { "value": ${month}, "type": "Property"}
  }""".trim()
}
case class PredictionRequest(id_estacion: Int, Ultima_medicion: Int, Diezhora_anterior: Int, variacion_estaciones: Double, dia: Int, hora: Int, num_mes: Int, socketId: String, predictionId: String)

object PredictionJob {

  final val variationStations = Map("0" -> 0.2777777778,"1" -> 0.094488189,"2" -> 0.062992126,"3" -> 0.0157480315,"4" -> 0.0236220472,"5" -> 0.0,"6" -> 0.0078740157,"7" -> 0.4881889764,"8" -> 0.188976378,"9" -> 0.7716535433,"10" -> 0.8031496063,"11" -> 1.2834645669,"12" -> 0.8333333333,"13" -> 0.7380952381,"14" -> 1.0952380952,"15" -> 0.9285714286,"16" -> 1.4444444444,"17" -> 0.9606299213,"18" -> 0.5590551181,"19" -> 0.4409448819,"20" -> 0.2992125984,"21" -> 0.1023622047,"22" -> 0.1102362205,"23" -> 0.08,"24" -> 0.0793650794,"25" -> 0.0078740157,"26" -> 0.0157480315,"27" -> 0.0,"28" -> 0.0,"29" -> 0.0,"30" -> 0.0078740157,"31" -> 0.0551181102,"32" -> 0.0236220472,"33" -> 0.0157480315,"34" -> 0.1417322835,"35" -> 0.2834645669,"36" -> 0.0555555556,"37" -> 0.246031746,"38" -> 0.1904761905,"39" -> 0.2698412698,"40" -> 0.3015873016,"41" -> 0.2440944882,"42" -> 0.0787401575,"43" -> 0.062992126,"44" -> 0.031496063,"45" -> 0.0157480315,"46" -> 0.0078740157,"47" -> 0.008,"48" -> 0.0555555556,"49" -> 0.0078740157,"50" -> 0.0157480315,"51" -> 0.0157480315,"52" -> 0.0236220472,"53" -> 0.0157480315,"54" -> 0.0157480315,"55" -> 0.0078740157,"56" -> 0.1023622047,"57" -> 0.1023622047,"58" -> 0.2598425197,"59" -> 0.4566929134,"60" -> 0.4365079365,"61" -> 0.3095238095,"62" -> 0.1746031746,"63" -> 0.2380952381,"64" -> 0.4365079365,"65" -> 0.5354330709,"66" -> 0.4409448819,"67" -> 0.125984252,"68" -> 0.031496063,"69" -> 0.0078740157,"70" -> 0.0078740157,"71" -> 0.0,"72" -> 0.0634920635,"73" -> 0.0,"74" -> 0.0157480315,"75" -> 0.0236220472,"76" -> 0.0,"77" -> 0.0,"78" -> 0.2125984252,"79" -> 0.6929133858,"80" -> 0.1023622047,"81" -> 0.2125984252,"82" -> 0.4724409449,"83" -> 0.3779527559,"84" -> 0.4444444444,"85" -> 0.3174603175,"86" -> 0.9603174603,"87" -> 0.619047619,"88" -> 0.8968253968,"89" -> 0.6062992126,"90" -> 0.2440944882,"91" -> 0.4803149606,"92" -> 0.2204724409,"93" -> 0.0393700787,"94" -> 0.031496063,"95" -> 0.008,"96" -> 0.119047619,"97" -> 0.0078740157,"98" -> 0.0472440945,"99" -> 0.0078740157,"100" -> 0.0078740157,"101" -> 0.0078740157,"102" -> 0.0078740157,"103" -> 0.1023622047,"104" -> 0.1417322835,"105" -> 0.4409448819,"106" -> 0.3149606299,"107" -> 0.3779527559,"108" -> 0.4841269841,"109" -> 0.2301587302,"110" -> 0.2777777778,"111" -> 0.1666666667,"112" -> 0.5079365079,"113" -> 0.4173228346,"114" -> 0.1811023622,"115" -> 0.1496062992,"116" -> 0.1732283465,"117" -> 0.0393700787,"118" -> 0.1102362205,"119" -> 0.024,"120" -> 0.1031746032,"121" -> 0.0,"122" -> 0.0078740157,"123" -> 0.0,"124" -> 0.0,"125" -> 0.0,"126" -> 0.0,"127" -> 0.0,"128" -> 0.031496063,"129" -> 0.1968503937,"130" -> 0.3622047244,"131" -> 0.2913385827,"132" -> 0.3333333333,"133" -> 0.5555555556,"134" -> 0.4285714286,"135" -> 0.4285714286,"136" -> 0.4444444444,"137" -> 0.3307086614,"138" -> 0.1338582677,"139" -> 0.188976378,"140" -> 0.1102362205,"141" -> 0.031496063,"142" -> 0.0,"143" -> 0.0,"144" -> 0.1031746032,"145" -> 0.0078740157,"146" -> 0.0078740157,"147" -> 0.0157480315,"148" -> 0.0078740157,"149" -> 0.0,"150" -> 0.0157480315,"151" -> 0.0472440945,"152" -> 0.3149606299,"153" -> 0.1338582677,"154" -> 0.3149606299,"155" -> 0.2913385827,"156" -> 0.3015873016,"157" -> 0.1666666667,"158" -> 0.3174603175,"159" -> 0.2777777778,"160" -> 0.5079365079,"161" -> 0.125984252,"162" -> 0.0787401575,"163" -> 0.0708661417,"164" -> 0.0551181102,"165" -> 0.0157480315,"166" -> 0.0236220472,"167" -> 0.032,"168" -> 0.0873015873,"169" -> 0.0,"170" -> 0.0078740157,"171" -> 0.0,"172" -> 0.0,"173" -> 0.0,"174" -> 0.0,"175" -> 0.0,"176" -> 0.0472440945,"177" -> 0.0787401575,"178" -> 0.2834645669,"179" -> 0.3307086614,"180" -> 0.3412698413,"181" -> 0.3492063492,"182" -> 0.2222222222,"183" -> 0.5158730159,"184" -> 0.5714285714,"185" -> 0.5826771654,"186" -> 0.3779527559,"187" -> 0.1496062992,"188" -> 0.0708661417,"189" -> 0.0472440945,"190" -> 0.0787401575,"191" -> 0.04,"192" -> 0.0555555556,"193" -> 0.031496063,"194" -> 0.0708661417,"195" -> 0.0078740157,"196" -> 0.0157480315,"197" -> 0.0078740157,"198" -> 0.0,"199" -> 0.0157480315,"200" -> 0.2125984252,"201" -> 0.1417322835,"202" -> 0.2362204724,"203" -> 0.3622047244,"204" -> 0.380952381,"205" -> 0.2857142857,"206" -> 0.3015873016,"207" -> 0.2936507937,"208" -> 0.3492063492,"209" -> 0.2598425197,"210" -> 0.2204724409,"211" -> 0.188976378,"212" -> 0.0708661417,"213" -> 0.1181102362,"214" -> 0.0236220472,"215" -> 0.016,"216" -> 0.119047619,"217" -> 0.0,"218" -> 0.0236220472,"219" -> 0.0,"220" -> 0.0,"221" -> 0.0,"222" -> 0.0,"223" -> 0.2519685039,"224" -> 0.2834645669,"225" -> 0.4566929134,"226" -> 0.3307086614,"227" -> 0.5039370079,"228" -> 0.4761904762,"229" -> 0.373015873,"230" -> 0.3095238095,"231" -> 0.5555555556,"232" -> 0.5634920635,"233" -> 0.4803149606,"234" -> 0.1732283465,"235" -> 0.1102362205,"236" -> 0.0787401575,"237" -> 0.031496063,"238" -> 0.0236220472,"239" -> 0.008,"240" -> 0.0952380952,"241" -> 0.0078740157,"242" -> 0.0393700787,"243" -> 0.0,"244" -> 0.0,"245" -> 0.0078740157,"246" -> 0.0157480315,"247" -> 0.0866141732,"248" -> 0.1338582677,"249" -> 0.1496062992,"250" -> 0.5433070866,"251" -> 0.5433070866,"252" -> 0.5,"253" -> 0.5634920635,"254" -> 0.6349206349,"255" -> 0.6349206349,"256" -> 0.8015873016,"257" -> 0.7007874016,"258" -> 0.3858267717,"259" -> 0.3858267717,"260" -> 0.3070866142,"261" -> 0.1653543307,"262" -> 0.0708661417,"263" -> 0.104,"264" -> 0.0873015873,"265" -> 0.0,"266" -> 0.0078740157,"267" -> 0.0,"268" -> 0.0,"269" -> 0.0,"270" -> 0.0,"271" -> 0.0157480315,"272" -> 0.2125984252,"273" -> 0.0787401575,"274" -> 0.1417322835,"275" -> 0.1181102362,"276" -> 0.1746031746,"277" -> 0.0952380952,"278" -> 0.1666666667,"279" -> 0.3253968254,"280" -> 0.2936507937,"281" -> 0.188976378,"282" -> 0.1732283465,"283" -> 0.0866141732,"284" -> 0.0393700787,"285" -> 0.0,"286" -> 0.0078740157,"287" -> 0.024,"288" -> 0.0476190476,"289" -> 0.0,"290" -> 0.0078740157,"291" -> 0.0,"292" -> 0.0,"293" -> 0.0,"294" -> 0.0,"295" -> 0.4488188976,"296" -> 0.125984252,"297" -> 0.1338582677,"298" -> 0.2913385827,"299" -> 0.094488189,"300" -> 0.119047619,"301" -> 0.3015873016,"302" -> 0.4523809524,"303" -> 0.3412698413,"304" -> 0.3174603175,"305" -> 0.2283464567,"306" -> 0.2125984252,"307" -> 0.1023622047,"308" -> 0.0708661417,"309" -> 0.0078740157,"310" -> 0.0236220472,"311" -> 0.016,"312" -> 0.0793650794,"313" -> 0.0078740157,"314" -> 0.0,"315" -> 0.0078740157,"316" -> 0.0,"317" -> 0.0078740157,"318" -> 0.062992126,"319" -> 0.2755905512,"320" -> 0.062992126,"321" -> 0.4015748031,"322" -> 0.4409448819,"323" -> 0.2913385827,"324" -> 0.5555555556,"325" -> 0.380952381,"326" -> 0.6428571429,"327" -> 0.4126984127,"328" -> 0.4444444444,"329" -> 0.4409448819,"330" -> 0.2755905512,"331" -> 0.4251968504,"332" -> 0.2598425197,"333" -> 0.0708661417,"334" -> 0.0236220472,"335" -> 0.032,"336" -> 0.0793650794,"337" -> 0.0,"338" -> 0.031496063,"339" -> 0.0078740157,"340" -> 0.0,"341" -> 0.0,"342" -> 0.0,"343" -> 0.0,"344" -> 0.0078740157,"345" -> 0.0787401575,"346" -> 0.062992126,"347" -> 0.2283464567,"348" -> 0.246031746,"349" -> 0.1428571429,"350" -> 0.0793650794,"351" -> 0.1031746032,"352" -> 0.0873015873,"353" -> 0.0708661417,"354" -> 0.0393700787,"355" -> 0.0078740157,"356" -> 0.0157480315,"357" -> 0.0236220472,"358" -> 0.0157480315,"359" -> 0.0,"360" -> 0.0079365079,"361" -> 0.0,"362" -> 0.0,"363" -> 0.0,"364" -> 0.0,"365" -> 0.0,"366" -> 0.0,"367" -> 0.0,"368" -> 0.0551181102,"369" -> 0.0551181102,"370" -> 0.0708661417,"371" -> 0.0393700787,"372" -> 0.0952380952,"373" -> 0.0317460317,"374" -> 0.0476190476,"375" -> 0.0634920635,"376" -> 0.119047619,"377" -> 0.188976378,"378" -> 0.0393700787,"379" -> 0.0236220472,"380" -> 0.0,"381" -> 0.0,"382" -> 0.0,"383" -> 0.0,"384" -> 0.0,"385" -> 0.0,"386" -> 0.0,"387" -> 0.0,"388" -> 0.0,"389" -> 0.0,"390" -> 0.0,"391" -> 0.0,"392" -> 0.0,"393" -> 0.0078740157,"394" -> 0.0078740157,"395" -> 0.0866141732,"396" -> 0.1666666667,"397" -> 0.0396825397,"398" -> 0.0158730159,"399" -> 0.0238095238,"400" -> 0.0476190476,"401" -> 0.0236220472,"402" -> 0.0472440945,"403" -> 0.0,"404" -> 0.0,"405" -> 0.0,"406" -> 0.0,"407" -> 0.0)
  final val URL_CB = "http://orion:1026/ngsi-ld/v1/entities/urn:ngsi-ld:ResSantanderBikePrediction1/attrs"
  final val CONTENT_TYPE = ContentType.JSON
  final val METHOD = HTTPMethod.PATCH
  final val BASE_PATH = "./prediction-job"
  final val MONGO_USERNAME = System.getenv("MONGO_USERNAME")
  final val MONGO_PASSWORD = System.getenv("MONGO_PASSWORD")

    def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder
      .appName("PredictingBikeSantander")
      .master("local[*]")
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")
    
    println("STARTING BIKE SANTANDER....")
    
    val ssc = new StreamingContext(spark.sparkContext, Seconds(1))

    
    val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    dateTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"))

    // Load model
    val model = PipelineModel.load(BASE_PATH+"/model")

    val eventStream = ssc.receiverStream(new NGSILDReceiver(9002))
    
    // Process event stream to get updated entities
    val processedDataStream = eventStream
      .flatMap(event => event.entities)
      .map(ent => {
        println(s"ENTITY RECEIVED: $ent")
        val month = ent.attrs("month")("value").toString.toInt
        val idStation = ent.attrs("idStation")("value").toString.toInt
        val hour = ent.attrs("hour")("value").toString.toInt
        val weekday = ent.attrs("weekday")("value").toString.toInt
        val socketId = ent.attrs("socketId")("value").toString
        val predictionId = ent.attrs("predictionId")("value").toString
        val num = (idStation.toInt - 1 )
        val idVariationStation = num * 24 + hour
        val variationStation = variationStations(idVariationStation.toString).toString.toDouble
        val dateTenHoursBefore = dateTimeFormatter.format(new Date(System.currentTimeMillis() - 3600 * 1000 *10))

        val mongoUri = s"mongodb://${MONGO_USERNAME}:${MONGO_PASSWORD}@mongo:27017/bikes_santander.historical?authSource=admin"
        val mongoClient = MongoClients.create(mongoUri);
        val collection = mongoClient.getDatabase("bikes_santander").getCollection("historical")
        val filter = and(gt("update_date", dateTenHoursBefore), equal("dc:identifier", idStation.toString))
        val docs = collection.find(filter)
        val lastMeasure = docs.sort(Sorts.descending("update_date")).first().getString("ayto:bicicletas_libres").toInt
        val tenHoursAgoMeasure = docs.sort(Sorts.ascending("update_date")).first().getString("ayto:bicicletas_libres").toInt
        PredictionRequest(idStation, lastMeasure, tenHoursAgoMeasure, variationStation, weekday, hour, month, socketId, predictionId)
      })

    // Feed each entity into the prediction model
    val predictionDataStream = processedDataStream
      .transform(rdd => {
        val df = spark.createDataFrame(rdd)
        val predictions = model
          .transform(df)
          .select("socketId","predictionId", "prediction", "id_estacion", "dia", "hora", "num_mes")

        predictions.toJavaRDD
    })
      .map(pred=> PredictionResponse(
        pred.get(0).toString,
        pred.get(1).toString,
        pred.get(2).toString.toFloat.round,
        pred.get(3).toString.toInt,
        pred.get(4).toString.toInt,
        pred.get(5).toString.toInt,
        pred.get(6).toString.toInt
      )
    )

    // Convert the output to an OrionSinkObject and send to Context Broker
    val sinkDataStream = predictionDataStream
      .map(res => OrionSinkObject(res.toString, URL_CB, CONTENT_TYPE, METHOD))

    // Add Orion Sink
    OrionSink.addSink(sinkDataStream)
    //sinkDataStream.print()
    //predictionDataStream.print()
    ssc.start()
    ssc.awaitTermination()
  }
}

 