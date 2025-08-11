#!/bin/bash

# Create topic 'orders' with 5 partitions
kafka-topics.sh --create --topic orders --partitions 5 --replication-factor 1 --bootstrap-server localhost:9092
echo "Topic 'orders' created with 5 partitions."
