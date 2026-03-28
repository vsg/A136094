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

CLASSPATH="lib/*"
MAIN_CLASS="oeis.a136094.Main"
THREADS=$(nproc)

ARGS=(-N 7 -min-piece-check-size 5 -t $THREADS -precalc-alg dfs -solve-alg dfs)
#ARGS=(-N 7 -min-piece-check-size 5 -t $THREADS -precalc-alg dfs -solve-alg dfs -solve "1/123456 24/12347")
#ARGS=(-N 7 -min-piece-check-size 5 -t $THREADS -precalc-alg dfs -solve-alg bfs -solve "1/123456 24/12347")

#ARGS=(-N 8 -min-piece-check-size 6 -t $THREADS -precalc-alg dfs-swarm -solve-alg dfs-swarm -dfs-swarm-max-groups 10 -dfs-batch-size 10000 -dfs-batch-max-cache 4000000000 -dist-123 40 -dist-45 20)
#ARGS=(-N 8 -min-piece-check-size 6 -t $THREADS -precalc-alg dfs -solve-alg dfs-batch -dfs-batch-size 10000 -dfs-batch-max-cache 4000000000 -dist-123 40 -dist-45 20)
#ARGS=(-N 8 -min-piece-check-size 6 -t $THREADS -precalc-alg dfs -solve-alg dfs-disk -dfs-disk-block-size 20000000 -dfs-disk-batch-size 5000000 -dfs-disk-seen-size 20000000 -dist-123 40 -dist-45 20)

#ARGS=(-N 9 -min-piece-check-size 7 -t $THREADS -checkpoint-shapes "1/4;1/5;5/5;5/5,5/5;1/6;1/7" -max-precalc-shape "1/7" -precalc-alg dfs -solve-alg none)
#ARGS=(-N 9 -min-piece-check-size 7 -t $THREADS -checkpoint-shapes "1/6;1/7;1/9" -precalc-alg dfs-batch -dfs-batch-size 10000 -dfs-batch-max-cache 8000000000 -solve-alg none)
#ARGS=(-N 9 -min-piece-check-size 7 -t %THREADS% -checkpoint-shapes "1/9" -precalc-alg none -solve-alg dfs-disk -dfs-disk-block-size 100000000 -dfs-disk-batch-size 10000000 -dfs-disk-seen-size $SEEN_SIZE -dist-123 80 -dist-45 60)

RUN_LOG="run_$(date '+%Y%m%d_%H%M%S').log"
ERROR_LOG="error.log"

java "$JAVA_RAM" -cp "$CLASSPATH" "$MAIN_CLASS" "${ARGS[@]}" 1> >(tee "$RUN_LOG" >&1) 2> >(tee "$ERROR_LOG" >&2)
