# (Distributed) GeoBroker

[![CodeFactor](https://www.codefactor.io/repository/github/moewex/geobroker/badge)](https://www.codefactor.io/repository/github/moewex/geobroker)

In the Internet of Things, the relevance of data often depends on the geographic context of data producers and consumers. Today’s data distribution services, however, mostly focus on data content and not on geo-context, which could help to reduce the dissemination of excess data in many IoT scenarios. We propose to use the geo-context information associated with devices to control data distribution.
For this, we designed GeoBroker, a data distribution service that uses the location of things, as well as geofences for messages and subscriptions, to control data distribution. This way, we enable new IoT application scenarios while also increasing overall system efficiency for scenarios where geo-contexts matter by delivering only relevant messages.

GeoBroker can also be used for message routing in distributed environments. Here, DisGB (distributed GeoBroker) uses geo-context information to improve inter-broker routing. We describe the abstract concept in this [tech report](https://www.mcc.tu-berlin.de/fileadmin/fg344/publications/2020-fgfc.pdf).

If you use this software in a publication, please cite it as:

### Text
Jonathan Hasenburg, David Bermbach. **GeoBroker: Leveraging Geo-Contexts for IoT Data Distribution**. In: Computer Communications. Elsevier 2020.

Jonathan Hasenburg, David Bermbach. **Towards Geo-Context Aware IoT Data Distribution.** In: Proceedings of the 4th Workshop on IoT Systems Provisioning and Management for Context-Aware Smart Cities (ISYCC 2019). Springer 2019.

### BibTeX
```
@article{hasenburg_geobroker:_2020,
	title = {{GeoBroker}: Leveraging geo-contexts for {IoT} data distribution},
	author = {Hasenburg, Jonathan and Bermbach, David},
	journal = {Computer Communications},
	volume = {151},
	doi = {10.1016/j.comcom.2020.01.015},
	year = {2020}
}

@inproceedings{hasenburg_towards_2018,
	title = {Towards Geo-Context Aware {IoT} Data Distribution},
	booktitle = {4th Workshop on {IoT} Systems Provisioning \& Management for Context-Aware Smart Cities ({ISYCC})},
	publisher = {Springer},
	author = {Hasenburg, Jonathan and Bermbach, David},
	year = {2018}
}
```

## Compiling and running the project from source

As this project contains multiple git submodules, one needs to run the following after cloning/downloading the sources:
```
git submodule init
git submodule update
```

### Using IntelliJ

- Import the project, choose Maven as the external model
- IntelliJ will automatically resolve all dependencies
- To start GeoBroker, run the main method of *de.hasenburg.geobroker.server.main.Server.kt* (you can find this class
 in the GeoBroker-Server directory)
    - This will start the server with a default configuration
    - You can provide your own configuration by providing the path to the configuration file as program argument
- A very simple client can be started by running *de.hasenburg.geobroker.client.main.SimpleClient.kt* (you can find
 this class in the GeoBroker-Client directory)
    - This client connects to GeoBroker and executes a set of simple actions

### Using docker

- Docker specific instructions can be found in *GeoBroker-Server/Docker.md* and *GeoBroker-Client/Docker.md*.

## Usage of Code in Publications

The code found in this repository has been used for the experiments of the GeoBroker paper.
- The original code for GEO experiments (geo-context information is used) is accessible in the GEO branch or release 
[v1.0](https://github.com/MoeweX/geobroker/releases/tag/v1.0).
- The original code for NoGEO experiments (geo-context information is not used) is accessible in the NoGEO branch or 
release [v1.0-noContext](https://github.com/MoeweX/geobroker/releases/tag/v1.0-noContext).

