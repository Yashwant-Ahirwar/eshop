#!/bin/bash

echo "Sending 100 orders..."

for i in {1..100}
do
  curl -X POST -H "Content-Type: application/json" \
  -d "{\"orderId\":\"order${i}\", \"product\":\"Product ${i}\", \"quantity\":$((i % 10 + 1))}" \
  http://localhost:8080/orders &
done

# Wait for all requests to finish
wait

echo "Finished sending 100 orders."
