#!/bin/bash

echo "Streaming Kafka topic 'orders' from beginning..."
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic orders --from-beginning --timeout-ms 30000
