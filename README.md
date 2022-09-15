# YODA - ML bike Santander

http://datos.santander.es/resource/?ds=estado-estaciones-bicicletas&id=c798a2d2-bb27-452e-9a97-25d97b5c21ac&ft=JSON
http://datos.santander.es/api/rest/datasets/tusbic_puestos_libres.json?items=17

## Initial tasks

* Copy the `template.env` file into `.env` and configure it.

## Training

* Clone this project


* Load and save the `prediction-job/santander_bike.csv` [from external source](https://drive.upm.es/s/wsSOZIrjj8xOR3H)
```shell
curl -o prediction-job/santander_bike.csv https://drive.upm.es/s/wsSOZIrjj8xOR3H/download
```

* If the model already exists (`prediction-job/model`) just build de project packages
```shell
docker compose -f docker-compose.build.packages.yml up -d --build
```

* If the model does not exist: Build the packages and train the model
```shell
docker compose -f docker-compose.train.yml up -d --build
```
* View the spark deployment in http://localhost:4040/jobs/

* Test that the `prediction-job/model` folder is generated with the ML model

## Testing (Development)

* Run the whole scenario in dev
```shell
docker compose -f docker-compose.dev.yml up -d
```

* Create sample data (**with the datetime information updated**):

  - Enter in the mongo container
  - Enter the database
   ```
   mongo -u [USERNAME]
   ```

   - Create the database
   ```
   use bikes_santander
   ```
   - Create the collections **with the datetime information updated**
   ```
   db.historical.insertMany([
         {"ayto:bicicletas_libres":"13",
         "ayto:puestos_libres":"12",
         "dc:modified":"2022-09-14T23:44:46Z",
         "dc:identifier":"11",
         "uri":"http://datos.santander.es/api/datos/tusbic_puestos_libres/11.json"},
         {"ayto:bicicletas_libres":"12",
         "ayto:puestos_libres":"13",
         "dc:modified":"2022-09-14T22:44:46Z",
         "dc:identifier":"11",
         "uri":"http://datos.santander.es/api/datos/tusbic_puestos_libres/11.json"},
         {"ayto:bicicletas_libres":"11",
         "ayto:puestos_libres":"14",
         "dc:modified":"2022-09-14T21:44:46Z",
         "dc:identifier":"11",
         "uri":"http://datos.santander.es/api/datos/tusbic_puestos_libres/11.json"},
         {"ayto:bicicletas_libres":"13",
         "ayto:puestos_libres":"12",
         "dc:modified":"2022-09-14T20:44:46Z",
         "dc:identifier":"11",
         "uri":"http://datos.santander.es/api/datos/tusbic_puestos_libres/11.json"},   
         {"ayto:bicicletas_libres":"13",
         "ayto:puestos_libres":"12",
         "dc:modified":"2022-09-14T19:44:46Z",
         "dc:identifier":"11",
         "uri":"http://datos.santander.es/api/datos/tusbic_puestos_libres/11.json"},
         {"ayto:bicicletas_libres":"10",
         "ayto:puestos_libres":"15",
         "dc:modified":"2022-09-14T18:44:46Z",
         "dc:identifier":"11",
         "uri":"http://datos.santander.es/api/datos/tusbic_puestos_libres/11.json"},   
         {"ayto:bicicletas_libres":"10",
         "ayto:puestos_libres":"15",
         "dc:modified":"2022-09-14T17:44:46Z",
         "dc:identifier":"11",
         "uri":"http://datos.santander.es/api/datos/tusbic_puestos_libres/11.json"},
         {"ayto:bicicletas_libres":"9",
         "ayto:puestos_libres":"16",
         "dc:modified":"2022-09-14T16:44:46Z",
         "dc:identifier":"11",
         "uri":"http://datos.santander.es/api/datos/tusbic_puestos_libres/11.json"}
      ])
   ```
* Create the predictionEntities and the subscriptions like in the `entities` folder

### Test the solution

1) Validate that the subscription and entities exist:
```
curl --location --request GET 'http://localhost:1026/ngsi-ld/v1/subscriptions/'
```

```
curl --location --request GET 'http://localhost:1026/ngsi-ld/v1/entities/urn:ngsi-ld:ReqSantanderBikePrediction1'
```

```
curl --location --request GET 'http://localhost:1026/ngsi-ld/v1/entities/urn:ngsi-ld:ResSantanderBikePrediction1'
```

2) Update the `ReqSantanderBikePrediction1`
```
curl --location --request PATCH 'http://localhost:1026/ngsi-ld/v1/entities/urn:ngsi-ld:ReqSantanderBikePrediction1/attrs' \
--header 'Content-Type: application/json' \
--data-raw '{
   "month":{
      "type":"Property",
      "value":9
   },
   "idStation": {
      "type":"Property",
      "value":11
   },
   "weekday":{
      "type":"Property",
      "value":2
   },
   "hour":{
      "type":"Property",
      "value":23
   },
   "predictionId":{
      "type":"Property",
      "value":"p-1662768034900"
   },
   "socketId":{
      "type":"Property",
      "value":"Fn0kKHEF-dOcr311AAAF"
   }
}'
```

3) See if the `ResSantanderBikePrediction1` changes

```
curl --location --request GET 'http://localhost:1026/ngsi-ld/v1/entities/urn:ngsi-ld:ResSantanderBikePrediction1'
```

Response:
```
{
   "@context":"https://uri.etsi.org/ngsi-ld/v1/ngsi-ld-core-context.jsonld",
   "id":"urn:ngsi-ld:ResSantanderBikePrediction1",
   "type":"ResSantanderBikePrediction",
   "socketId":{
      "type":"Property",
      "value":"Fn0kKHEF-dOcr311AAAF"
   },
   "predictionId":{
      "type":"Property",
      "value":"p-1662768034900"
   },
   "predictionValue":{
      "type":"Property",
      "value":15
   },
   "idStation":{
      "type":"Property",
      "value":"11"
   },
   "weekday":{
      "type":"Property",
      "value":2
   },
   "hour":{
      "type":"Property",
      "value":22
   },
   "month":{
      "type":"Property",
      "value":9
   }
```


## Production (YODA)

* Run the whole scenario in prod within YODA (only spark). You need to create the predictionEntities and the subscriptions like in the `entities` folder. They are required the creation of entities and the subscription of spark. In the consuming application there are two possibilities:
   - The application receives a notification when the prediction is made and receives the `urn:ngsi-ld:ResSantanderBikePrediction1`
   - The application asks periodically to orion (`urn:ngsi-ld:ResSantanderBikePrediction1`) and see if the prediction was made

```shell
docker compose up -d
```

* Example of petition made to ask for a prediction:

```
curl --location --request PATCH 'http://broker-yoda.dit.upm.es/ngsi-ld/v1/entities/urn:ngsi-ld:ReqSantanderBikePrediction1/attrs' \
--header 'Content-Type: application/json' \
--data-raw '{
   "month":{
      "type":"Property",
      "value":9
   },
   "idStation": {
      "type":"Property",
      "value":11
   },
   "weekday":{
      "type":"Property",
      "value":2
   },
   "hour":{
      "type":"Property",
      "value":23
   },
   "predictionId":{
      "type":"Property",
      "value":"p-1662768034900"
   },
   "socketId":{
      "type":"Property",
      "value":"Fn0kKHEF-dOcr311AAAF"
   }
}'
```

Being:
- idStation: station id
- month: [1, 2, 3, ..., 12]
- weekday: [1, ..., 7] 1 ->Sunday  7->Saturday
- time: : [0, ... , 23]
- predictionId: String to identify the prediction in the consuming application
- socketId: String to identify the socket with the client in the consuming application

### Testing everything worked

1) Validate that the subscription and entities exist:
```
curl --location --request GET 'http://broker-yoda.dit.upm.es/ngsi-ld/v1/subscriptions/'
```

```
curl --location --request GET 'http://broker-yoda.dit.upm.es/ngsi-ld/v1/entities/urn:ngsi-ld:ReqSantanderBikePrediction1'
```

```
curl --location --request GET 'http://broker-yoda.dit.upm.es/ngsi-ld/v1/entities/urn:ngsi-ld:ResSantanderBikePrediction1'
```

2) Update the `ReqSantanderBikePrediction1`
```
curl --location --request PATCH 'http://broker-yoda.dit.upm.es/ngsi-ld/v1/entities/urn:ngsi-ld:ReqSantanderBikePrediction1/attrs' \
--header 'Content-Type: application/json' \
--data-raw '{
   "month":{
      "type":"Property",
      "value":9
   },
   "idStation": {
      "type":"Property",
      "value":11
   },
   "weekday":{
      "type":"Property",
      "value":2
   },
   "hour":{
      "type":"Property",
      "value":23
   },
   "predictionId":{
      "type":"Property",
      "value":"p-1662768034900"
   },
   "socketId":{
      "type":"Property",
      "value":"Fn0kKHEF-dOcr311AAAF"
   }
}'
```

3) See if the `ResSantanderBikePrediction1` changes

```
curl --location --request GET 'http://broker-yoda.dit.upm.es/ngsi-ld/v1/entities/urn:ngsi-ld:ResSantanderBikePrediction1'
```

Response:
```
{
   "@context":"https://uri.etsi.org/ngsi-ld/v1/ngsi-ld-core-context.jsonld",
   "id":"urn:ngsi-ld:ResSantanderBikePrediction1",
   "type":"ResSantanderBikePrediction",
   "socketId":{
      "type":"Property",
      "value":"Fn0kKHEF-dOcr311AAAF"
   },
   "predictionId":{
      "type":"Property",
      "value":"p-1662768034900"
   },
   "predictionValue":{
      "type":"Property",
      "value":15
   },
   "idStation":{
      "type":"Property",
      "value":"11"
   },
   "weekday":{
      "type":"Property",
      "value":2
   },
   "hour":{
      "type":"Property",
      "value":22
   },
   "month":{
      "type":"Property",
      "value":9
   }
```
Result of prediction: 15 bikes available
- predictionValue: number of bikes available
