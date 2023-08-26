# CityTwin

The objective of CityTwin is to realize the simulation of a digital twin system in the context of the smart city. In particular, we want to realize a system that is able to capture and represent in digital format the behavior of the various entities present within the city.

- Within the simulation, there will be two types of nodes: Mainstay and Resource.
- Mainstay nodes are the backbone of the entire system.
- Resource nodes represent abstractions of sensors, actuators, or more complex entities.
- The user can view the current state of the system, the data history, and any statistics.
- The user has the ability to interact with the system via GUI (E.g.: Intervention after the detection of a fire).

## Technologies used

- Scala + SBT
- Akka
- MongoDB
- Docker

## How to run the project

### Common requirements

- Scala 3.3.0
- Java openjdk 17.0.7
- SBT 1.x
- Docker
- Docker Compose

### Platform specific requirements

Linux:

- unzip

Windows:

- tar

### Build and start the system

First of all, start the persistence service:

```
docker compose build
docker compose up
```

Then start the rest of the system in another terminal:
On Linux:

```bash
./scripts/startup.sh
```

On Windows:
```
./scripts/startup.bat
```
