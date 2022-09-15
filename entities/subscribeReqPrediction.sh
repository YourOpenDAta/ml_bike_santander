curl -v  orion:1026/ngsi-ld/v1/subscriptions/ -s -S -H 'Content-Type: application/ld+json' -d @- <<EOF
{
  "description": "A subscription to get request predictions for bikes in Santander",
  "type": "Subscription",
  "entities": [{
    "id": "urn:ngsi-ld:ReqSantanderBikePrediction1",
    "type": "ReqSantanderBikePrediction"
    }],
  "watchedAttributes": [
      "predictionId",
      "socketId",
      "idStation",
      "month",
      "hour",
      "weekday"
    ],
  "notification": {
    "endpoint": {
      "uri": "http://spark-master-bike-santander:9002",
      "accept": "application/json"
    }
  },
    "@context": [
        "https://uri.etsi.org/ngsi-ld/v1/ngsi-ld-core-context.jsonld"
    ]
}
EOF
