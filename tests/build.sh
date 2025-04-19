#!/bin/bash
set -euo pipefail

chmod +x ./gradlew
./gradlew clean
./gradlew build

docker-compose build