#!/bin/bash

mvn clean package

for i in $(seq 1 ${1:-1}); do
  name="terminal$i"

  if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    echo "Linux"
    guake -n "$(pwd)" -r "$name" -e "./run.sh ${2:-1} $i"
  elif [[ "$OSTYPE" == "darwin"* ]]; then
    echo "Macos"
    script="./run.sh ${2:-1} $i"
    osascript \
      -e 'tell application "Terminal" to activate' \
      -e 'tell application "System Events" to tell process "Terminal" to keystroke "t" using command down' \
      -e "tell application \"Terminal\" to do script \"$script\" in selected tab of the front window"
    sleep 3
  else
    echo "Unsupported OS"
  fi
done
