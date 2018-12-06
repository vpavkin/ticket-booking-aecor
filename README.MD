# Eventsourced ticket bookings with Aecor

This is a sample application for the ["Aecor - Purely functional event sourcing in Scala"](https://pavkin.ru/aecor-intro/) post series.

### Prerequisites

Infrastructure for the app includes postgres and kafka, so you'll need either

* docker-compose (recommended)
* or have both postgres and kafka installed locally.

### Running (with docker-compose)

1. Launch infrastructure using compose (in the project folder):

```bash
docker-compose up -d
```

2. Run the app

```
sbt booking/run
```

### Playing around

There are http endpoints you can call

##### Place booking:

```bash
curl --request POST \
  --url http://localhost:9000/clientA/bookings \
  --header 'content-type: application/json' \
  --data '{
	"concertId": "concertA",
	"seats": [
		{
			"row": 1,
			"number": 2
		},
		{
			"row": 1,
			"number": 3
		}
	]
}'
```

##### Get client bookings:

```bash
curl --request GET \
  --url http://localhost:9000/clientA/bookings
```

##### Emulate payment (via kafka message):

Payments are received from another service via PaymentReceived topic, so to emulate a payment we'll need to produce a message:

```bash
docker exec -it ticket-booking_kafka_1 /bin/bash

$KAFKA_HOME/bin/kafka-console-producer.sh --broker-list localhost:9092 --topic PaymentReceived

{"clientId":"clientA", "paymentId":"12345", "bookingId": "<your_booking_id>"}
```

