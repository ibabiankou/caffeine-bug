#!/bin/bash

mvn clean package

for i in $(seq 1 ${1:-1}); do
  name="terminal$i"
	guake -n "$(pwd)" -r "$name" -e "./run.sh ${2:-1} $i"
done

