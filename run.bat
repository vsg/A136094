set CLASSPATH=lib\*;target\*
set MAIN_CLASS=oeis.a136094.Main

set ARGS=-Xmx500m %MAIN_CLASS% --n=7 --checkpoint-shapes=1/7 --precalc-alg=dfs --solve-alg=dfs
::set ARGS=-Xmx500m %MAIN_CLASS% --n=7 --checkpoint-shapes=1/7 --precalc-alg=dfs-swarm --solve-alg=dfs-swarm --dfs-swarm-max-groups=5
::set ARGS=-Xmx500m %MAIN_CLASS% --n=7 --checkpoint-shapes=1/7 --precalc-alg=dfs --solve-alg=dfs --debug --debug-next-moves --max-precalc-shape=2/3,1/3 --solve "12/123 4/456 4/14"
::set ARGS=-Xmx500m %MAIN_CLASS% --n=7 --checkpoint-shapes=1/7 --precalc-alg=dfs --solve-alg=dfs --solve "1/123456 24/12347"

::set ARGS=-Xmx6000m %MAIN_CLASS% --n=8 --checkpoint-shapes=1/8 --precalc-alg=dfs --solve-alg=dfs-batch --dfs-batch-size=10000 --dfs-batch-max-cache=250000000 --max-loop-123=40 --max-loop-45=20
::set ARGS=-Xmx10000m %MAIN_CLASS% --n=8 --checkpoint-shapes=1/8 --precalc-alg=dfs --solve-alg=dfs-batch --dfs-batch-size=10000 --dfs-batch-max-cache=1000000000 --max-loop-123=40 --max-loop-45=20
::set ARGS=-Xmx32000m %MAIN_CLASS% --n=8 --checkpoint-shapes=1/8 --precalc-alg=dfs --solve-alg=dfs-batch --dfs-batch-size=10000 --dfs-batch-max-cache=4000000000 --max-loop-123=40 --max-loop-45=20
::set ARGS=-Xmx15000m %MAIN_CLASS% --n=8 --checkpoint-shapes=1/8 --precalc-alg=dfs --solve-alg=dfs-disk --dfs-disk-block-size=10000000 --dfs-disk-batch-size=5000000 --dfs-disk-seen-size=50000000 --max-loop-123=40 --max-loop-45=20

::set ARGS=-Xmx12000m %MAIN_CLASS% --n=9 --checkpoint-shapes=2/6 --max-precalc-shape=2/6 --precalc-alg=dfs --solve-alg=none
::set ARGS=-Xmx32000m %MAIN_CLASS% --n=9 --checkpoint-shapes=2/6;1/9 --precalc-alg=dfs-batch --dfs-batch-size=10000 --dfs-batch-max-cache=4000000000 --solve-alg=none
::set ARGS=-Xmx32000m %MAIN_CLASS% --n=9 --checkpoint-shapes=1/9 --precalc-alg=none --solve-alg=dfs-disk --dfs-disk-block-size=50000000 --dfs-disk-batch-size=10000000 --dfs-disk-seen-size=50000000 --max-loop-123=80 --max-loop-45=60

set DBG=-Xverify:none -Djdk.attach.allowAttachSelf
::set DBG=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8000

@for /f %%i in ('powershell -Command "Get-Date -Format yyyyMMdd_HHmmss"') do set TIMESTAMP=%%i
set RUN_LOG=run_%TIMESTAMP%.log
set ERROR_LOG=error.log

java %DBG% -cp %CLASSPATH% %ARGS% 1>>%RUN_LOG% 2>>%ERROR_LOG%
