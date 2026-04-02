#!/bin/bash

MEMORY_GB="$(free | grep "Mem:" | awk '{print int($2/1024/1024)}')"

if [ $MEMORY_GB -ge 350 ]; then
  SEEN_SIZE=250000000
  JAVA_RAM=-Xmx300g
elif [ $MEMORY_GB -ge 250 ]; then
  SEEN_SIZE=150000000
  JAVA_RAM=-Xmx200g
elif [ $MEMORY_GB -ge 180 ]; then
  SEEN_SIZE=100000000
  JAVA_RAM=-Xmx150g
elif [ $MEMORY_GB -ge 100 ]; then
  SEEN_SIZE=50000000
  JAVA_RAM=-Xmx100g
elif [ $MEMORY_GB -ge 30 ]; then
  SEEN_SIZE=20000000
  JAVA_RAM=-Xmx28000m
elif [ $MEMORY_GB -ge 15 ]; then
  SEEN_SIZE=10000000
  JAVA_RAM=-Xmx14000m
elif [ $MEMORY_GB -ge 7 ]; then
  SEEN_SIZE=5000000
  JAVA_RAM=-Xmx6000m
else
  SEEN_SIZE=1000000
  JAVA_RAM=-Xmx2000m
fi

CLASSPATH="lib/*:target/*"
MAIN_CLASS="oeis.a136094.Main"
ARGS=("$@")

ARGS+=(--dfs-disk-seen-size=$SEEN_SIZE)

SOLVE_LOG="solve_$(date '+%Y%m%d_%H%M%S').log"

java "$JAVA_RAM" -cp "$CLASSPATH" "$MAIN_CLASS" "${ARGS[@]}" 2>&1 | tee "$SOLVE_LOG"
