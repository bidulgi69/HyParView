#!/bin/bash
set -e

function verify() {
  local url=$1
  local exists_in_active_view
  local exists_in_passive_view

  exists_in_active_view=$(curl -s "http://$url/view?region=ACTIVE" | jq 'map(.id)|contains(["hyparview-node3"])')
  exists_in_passive_view=$(curl -s "http://$url/view?region=PASSIVE" | jq 'map(.id)|contains(["hyparview-node3"])')

  if [ "$exists_in_active_view" = "false" ] && [ "$exists_in_passive_view" = "false" ]; then
    echo "Node-3 disconnected from $url"
  else
    echo "Node-3 still exists in $url"
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

# shutdown node-3
echo 'Shutting down node-3'
docker-compose down hyparview-node3

echo 'Waiting for disconnection to propagate throughout the cluster'
sleep 20

verify localhost:8080
verify localhost:8081
verify localhost:8083
verify localhost:8084

bash ./tests/clean.sh