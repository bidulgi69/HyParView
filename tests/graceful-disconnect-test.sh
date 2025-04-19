#!/bin/bash
set -e

function verify() {
  local url=$1
  local exists_in_active_view
  local exists_in_passive_view

  exists_in_active_view=$(curl -s "http://$url/view?region=ACTIVE" | jq 'map(.id)|contains(["hyparview-node2"])')
  exists_in_passive_view=$(curl -s "http://$url/view?region=PASSIVE" | jq 'map(.id)|contains(["hyparview-node2"])')

  if [ "$exists_in_active_view" = "false" ] && [ "$exists_in_passive_view" = "false" ]; then
    echo "Node-2 disconnected from $url"
  else
    echo "Node-2 is still connected to $url"
    exit 1
  fi
}

bash ./tests/build.sh

echo 'Running up the cluster...'
docker-compose up -d hyparview-node1
sleep 2
docker-compose up -d hyparview-node2
sleep 2
docker-compose up -d hyparview-node3
sleep 2
docker-compose up -d hyparview-node4
sleep 2
docker-compose up -d hyparview-node5

sleep 30

# terminate node-2
echo 'Terminate node-2'
response=$(curl -s -w "%{http_code}" localhost:8081/terminate)
status_code=$(echo "$response" | tail -n 1)

if [ "$status_code" -eq 200 ]; then
  echo "Node-2 has been terminated successfully."
else
  echo "Failed to terminate node-2. (status: $status_code)"
  exit 1
fi

echo 'Waiting for disconnection to propagate throughout the cluster'
sleep 20

verify localhost:8080
verify localhost:8082
verify localhost:8083
verify localhost:8084

bash ./tests/clean.sh