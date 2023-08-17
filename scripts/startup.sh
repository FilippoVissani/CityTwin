#!/bin/sh

sbt clean
sbt universal:packageBin
unzip ./core/target/universal/core-0.1.0.zip -d ./core/target/
unzip ./control-panel/target/universal/control-panel-0.1.0.zip -d ./control-panel/target/

./core/target/core-0.1.0/bin/core 2551 &

./core/target/core-0.1.0/bin/core 2552 &

./core/target/core-0.1.0/bin/core 2553 &

./control-panel/target/control-panel-0.1.0/bin/control-panel 2554 &
