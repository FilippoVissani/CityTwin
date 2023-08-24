#!/bin/sh

sbt clean
sbt universal:packageBin
unzip ./core/target/universal/core-0.1.0.zip -d ./core/target/
unzip ./control-panel/target/universal/control-panel-0.1.0.zip -d ./control-panel/target/
unzip ./river-monitor/target/universal/river-monitor-0.1.0.zip -d ./river-monitor/target/
unzip ./air-quality-monitor/target/universal/air-quality-monitor-0.1.0.zip -d ./air-quality-monitor/target/
unzip ./acid-rain-monitor/target/universal/acid-rain-monitor-0.1.0.zip -d ./acid-rain-monitor/target/
unzip ./noise-pollution-monitor/target/universal/noise-pollution-monitor-0.1.0.zip -d ./noise-pollution-monitor/target/

$TERM -e ./core/target/core-0.1.0/bin/core 2551 127.0.0.1 8080 &
$TERM -e ./core/target/core-0.1.0/bin/core 2552 127.0.0.1 8080 &
$TERM -e ./core/target/core-0.1.0/bin/core 2553 127.0.0.1 8080 &
$TERM -e ./control-panel/target/control-panel-0.1.0/bin/control-panel 2554 16000 9000 &
$TERM -e ./river-monitor/target/river-monitor-0.1.0/bin/river-monitor-main 2555 RiverMonitor_Savio 15 5200 5600 FloodSensor_PonteVecchio FloodSensor_PonteNuovo View_Savio &
$TERM -e ./river-monitor/target/river-monitor-0.1.0/bin/flood-sensor-main 2556 FloodSensor_PonteVecchio 4500 7100 &
$TERM -e ./river-monitor/target/river-monitor-0.1.0/bin/flood-sensor-main 2557 FloodSensor_PonteNuovo 4900 4200 &
$TERM -e ./river-monitor/target/river-monitor-0.1.0/bin/view-main 2558 View_Savio 600 200 RiverMonitor_Savio &
$TERM -e ./air-quality-monitor/target/air-quality-monitor-0.1.0/bin/air-quality-monitor.bat 2559 AirSensor_ParcoDelleQuerce 6800 6600 "https://air-quality-sensor-cesena.azurewebsites.net/api/getAirValues?code=o_cTLAmHm8d4UvdWC89wVKRqfUMGMF55x0sA9FDGTsczAzFuTYg6bg==" &
$TERM -e ./acid-rain-monitor/target/acid-rain-monitor-0.1.0/bin/acid-rain-monitor.bat 2560 AcidRainSensor_Ippodromo 3000 500 &
$TERM -e ./noise-pollution-monitor/target/noise-pollution-monitor-0.1.0/bin/noise-pollution-monitor.bat 2561 NoiseSensor_Scuola 2000 5000 &
