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

function verify_node3_evict() {
  local url=$1
  local region=$2
  local expect=$3
  local actual

  actual=$(curl -s "http://$url/view?region=$region" | jq 'map(.id)|contains(["hyparview-node3"])')

  if [ "$actual" -ne "$expect" ]; then
    echo "Mismatch: (expected: $expect, actual: $actual)"
    exit 1
  else
    echo "Matches well"
  fi
}

./gradlew clean
./gradlew build
docker-compose -f docker-compose.manual.yml build

# setup
docker-compose -f docker-compose.manual.yml up -d hyparview-node1 hyparview-node2 hyparview-node3
sleep 5

# connect node-2, 3 to node-1
connect localhost:8080 hyparview-node3 8080
connect localhost:8080 hyparview-node2 8080

# run node-4
docker-compose -f docker-compose.manual.yml up -d hyparview-node4
sleep 3

# connect node-4 to node-1
# eldest entry(node3) will evict from active view
connect localhost:8080 hyparview-node4 8080
sleep 3

# verify node-3 was evicted from active view
verify_node3_evict localhost:8080 ACTIVE "false"
# verify node-3 was moved to passive view
verify_node3_evict localhost:8080 PASSIVE "true"

bash ./tests/clean.sh