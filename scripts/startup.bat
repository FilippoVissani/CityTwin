call sbt clean

call sbt universal:packageBin

tar -x -f ./core/target/universal/core-0.1.0.zip -C ./core/target/
tar -x -f ./control-panel/target/universal/control-panel-0.1.0.zip -C ./control-panel/target/
tar -x -f ./river-monitor/target/universal/river-monitor-0.1.0.zip -C ./river-monitor/target/
tar -x -f ./air-quality-monitor/target/universal/air-quality-monitor-0.1.0.zip -C ./air-quality-monitor/target/
tar -x -f ./acid-rain-monitor/target/universal/acid-rain-monitor-0.1.0.zip -C ./acid-rain-monitor/target/

start cmd /k call ./core/target/core-0.1.0/bin/core.bat 2551 127.0.0.1 8080
start cmd /k call ./core/target/core-0.1.0/bin/core.bat 2552 127.0.0.1 8080
start cmd /k call ./core/target/core-0.1.0/bin/core.bat 2553 127.0.0.1 8080
start cmd /k call ./control-panel/target/control-panel-0.1.0/bin/control-panel.bat 2554 16000 9000
start cmd /k call ./river-monitor/target/river-monitor-0.1.0/bin/river-monitor-main.bat 2555 riverMonitor1 15 100 100 floodSensor1 view1 floodSensor2
start cmd /k call ./river-monitor/target/river-monitor-0.1.0/bin/flood-sensor-main.bat 2556 floodSensor1 4800 4500
start cmd /k call ./river-monitor/target/river-monitor-0.1.0/bin/flood-sensor-main.bat 2558 floodSensor2 4500 6500
start cmd /k call ./river-monitor/target/river-monitor-0.1.0/bin/view-main.bat 2557 view1 600 200 riverMonitor1
start cmd /k call ./air-quality-monitor/target/air-quality-monitor-0.1.0/bin/air-quality-monitor.bat 2559 AirSensor_ParcoDelleQuerce 6800 6600 "https://air-quality-sensor-cesena.azurewebsites.net/api/getAirValues?code=o_cTLAmHm8d4UvdWC89wVKRqfUMGMF55x0sA9FDGTsczAzFuTYg6bg=="
start cmd /k call ./acid-rain-monitor/target/acid-rain-monitor-0.1.0/bin/acid-rain-monitor.bat 2560 AcidRainSensor_Ippodromo 3000 500
