#!/bin/bash
set -e

verify_value() {
  local expected="$1"
  local actual="$2"

  if [ "$actual" -ne "$expected" ]; then
    echo "Mismatch: (Expected: ${expected}, Actual: ${actual})"
    exit 1
  else
    echo 'Matches well'
  fi
}

bash ./tests/build.sh

# run node-1
docker-compose up -d hyparview-node1
sleep 3

before=$(curl -s 'http://localhost:8080/view?region=ACTIVE' | jq 'length')
echo "Before node-2 joins the cluster, the size of active view in node-1 should be 0"
verify_value 0 "$before"

# run node-2 which has node-1 as seed(bootstrap)
docker-compose up -d hyparview-node2
sleep 5

after=$(curl -s 'http://localhost:8080/view?region=ACTIVE' | jq 'length')
echo "After node-2 joins the cluster, the size of active view in node-1 should be 1"
verify_value 1 "$after"

# run node-3 which has node-2 as bootstrap
docker-compose up -d hyparview-node3
sleep 5

after_2=$(curl -s 'http://localhost:8081/view?region=ACTIVE' | jq 'length')
echo "After node-3 joins the cluster, the size of active view in node-2 should be 2"
verify_value 2 "$after_2"

bash ./tests/clean.sh
