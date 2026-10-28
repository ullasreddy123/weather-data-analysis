#!/usr/bin/env bash
# Builds the MapReduce job and runs it over data/weather.csv, writing data/output/.
# Uses a local Hadoop install if available, otherwise the apache/hadoop Docker image.
set -euo pipefail
cd "$(dirname "$0")/.."

JAR=hadoop-job/target/weather-hadoop-job.jar
CLASS=com.weather.hadoop.WeatherJob

if command -v mvn >/dev/null; then
  mvn -q -B -pl hadoop-job -am -DskipTests package
else
  docker run --rm -v "$PWD":/work -w /work maven:3.9-eclipse-temurin-17 \
    mvn -q -B -pl hadoop-job -am -DskipTests package
fi

rm -rf data/output

if command -v hadoop >/dev/null; then
  hadoop jar "$JAR" "$CLASS" "file://$PWD/data/weather.csv" "file://$PWD/data/output"
else
  docker run --rm -v "$PWD":/work -w /work apache/hadoop:3.3.6 \
    hadoop jar "/work/$JAR" "$CLASS" file:///work/data/weather.csv file:///work/data/output
fi

echo "Done. Output in data/output/. Refresh the dashboard (or click 'Reload data')."
