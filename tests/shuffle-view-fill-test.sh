#!/bin/bash
set -e

function connect() {
  local host_address="$1"
  local join_host="$2"
  local join_port="$3"

  echo "Connect $join_host to $host_address..."
  curl -XPOST "$host_address/join" \
      -H "Content-Type: application/json" \
      -d "{\"nodeId\": \"$join_host\", \"host\": \"$join_host\", \"port\": $join_port}"
}

function run_scheduler() {
  local host_address="$1"

  echo "Run schedulers in $1"
  curl -XGET "$host_address/schedule"
}

function verify_active_fills() {
  local url=$1

  actual=$(curl -s "http://$url/view?region=ACTIVE" | jq 'map(.id)|length')

  if [ "$actual" -eq 2 ]; then
    echo "$url has 2 active peers."
  else
    echo "Mismatch: ($url has $actual active peer.)"
    exit 1
  fi
}

./gradlew clean
./gradlew build
docker-compose -f docker-compose.manual.yml build

# setup
docker-compose -f docker-compose.manual.yml up -d hyparview-node1 hyparview-node2 hyparview-node3
sleep 5

# node-2 has 2 active peers
# other nodes have 1 active peer
connect localhost:8081 hyparview-node1 8080
connect localhost:8082 hyparview-node2 8080
sleep 3

# run schedulers(shuffleScheduler, heartbeatScheduler)
run_scheduler localhost:8080
run_scheduler localhost:8081
run_scheduler localhost:8082

# wait for shuffle messages to be handled
# 33s= shuffle message will be issued twice for each node
sleep 33

verify_active_fills localhost:8080
verify_active_fills localhost:8082

bash ./tests/clean.sh