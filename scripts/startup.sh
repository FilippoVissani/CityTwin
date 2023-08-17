#!/bin/sh

sbt clean
sbt universal:packageBin
unzip ./core/target/universal/core-0.1.0.zip -d ./core/target/
unzip ./control-panel/target/universal/control-panel-0.1.0.zip -d ./control-panel/target/
unzip ./river-monitor/target/universal/river-monitor-0.1.0.zip -d ./river-monitor/target/

$TERM -e ./core/target/core-0.1.0/bin/core 2551 &
$TERM -e ./core/target/core-0.1.0/bin/core 2552 &
$TERM -e ./core/target/core-0.1.0/bin/core 2553 &
$TERM -e ./control-panel/target/control-panel-0.1.0/bin/control-panel 2554 &
$TERM -e ./river-monitor/target/river-monitor-0.1.0/bin/river-monitor-main 2555 &
$TERM -e ./river-monitor/target/river-monitor-0.1.0/bin/flood-sensor-main 2556 &
$TERM -e ./river-monitor/target/river-monitor-0.1.0/bin/view-main 2557 &