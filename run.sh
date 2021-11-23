#!/bin/bash


q=0
port=$((8080+${2:-0}))
while [[ q -lt $1 ]]; do
  if java \
       -agentlib:jdwp=transport=dt_socket,address=*:$port,server=y,suspend=n \
       -jar target/caffeine-bug-1.0-SNAPSHOT-jar-with-dependencies.jar; then
    ((q++))
    echo $q
  else
    echo "Reproduced!"
    pwd
    read k
  fi
done

echo "I'm done!"
