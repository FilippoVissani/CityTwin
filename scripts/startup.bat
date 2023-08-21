call sbt clean

call sbt universal:packageBin

tar -x -f ./core/target/universal/core-0.1.0.zip -C ./core/target/
tar -x -f ./control-panel/target/universal/control-panel-0.1.0.zip -C ./control-panel/target/
tar -x -f ./river-monitor/target/universal/river-monitor-0.1.0.zip -C ./river-monitor/target/

start cmd /k call ./core/target/core-0.1.0/bin/core.bat 2551
start cmd /k call ./core/target/core-0.1.0/bin/core.bat 2552
start cmd /k call ./core/target/core-0.1.0/bin/core.bat 2553
start cmd /k call ./control-panel/target/control-panel-0.1.0/bin/control-panel.bat 2554 16000 9000
start cmd /k call ./river-monitor/target/river-monitor-0.1.0/bin/river-monitor-main.bat 2555 riverMonitor1 100 100 floodSensor1 view1
start cmd /k call ./river-monitor/target/river-monitor-0.1.0/bin/flood-sensor-main.bat 2556 floodSensor1 4800 4500
start cmd /k call ./river-monitor/target/river-monitor-0.1.0/bin/view-main.bat 2557 view1 600 200 riverMonitor1
