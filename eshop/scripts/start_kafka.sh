#!/bin/bash

# Start Kafka using Docker Compose
docker-compose up -d

# Wait a few seconds for Kafka to be ready
echo "Waiting for Kafka to start..."
sleep 10
echo "Kafka should be ready now."
