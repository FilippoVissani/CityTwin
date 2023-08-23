#!/bin/sh

sbt clean
sbt universal:packageBin
unzip ./core/target/universal/core-0.1.0.zip -d ./core/target/
unzip ./control-panel/target/universal/control-panel-0.1.0.zip -d ./control-panel/target/
unzip ./river-monitor/target/universal/river-monitor-0.1.0.zip -d ./river-monitor/target/
unzip ./air-quality-monitor/target/universal/air-quality-monitor-0.1.0.zip -d ./air-quality-monitor/target/

$TERM -e ./core/target/core-0.1.0/bin/core 2551 127.0.0.1 8080 &
$TERM -e ./core/target/core-0.1.0/bin/core 2552 127.0.0.1 8080 &
$TERM -e ./core/target/core-0.1.0/bin/core 2553 127.0.0.1 8080 &
$TERM -e ./control-panel/target/control-panel-0.1.0/bin/control-panel 2554 16000 9000 &
$TERM -e ./river-monitor/target/river-monitor-0.1.0/bin/river-monitor-main 2555 riverMonitor1 15 100 100 floodSensor1 view1 floodSensor2 &
$TERM -e ./river-monitor/target/river-monitor-0.1.0/bin/flood-sensor-main 2556 floodSensor1 4800 4500 &
$TERM -e ./river-monitor/target/river-monitor-0.1.0/bin/flood-sensor-main 2558 floodSensor2 4500 6500 &
$TERM -e ./river-monitor/target/river-monitor-0.1.0/bin/view-main 2557 view1 600 200 riverMonitor1 &
$TERM -e ./air-quality-monitor/target/air-quality-monitor-0.1.0/bin/air-quality-monitor.bat 2559 AirSensor_ParcoDelleQuerce 6500 6500 "https://air-quality-sensor-cesena.azurewebsites.net/api/getAirValues?code=o_cTLAmHm8d4UvdWC89wVKRqfUMGMF55x0sA9FDGTsczAzFuTYg6bg=="