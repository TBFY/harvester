<p align="center"><img width=15% src="https://github.com/TBFY/general/blob/master/figures/tbfy-logo.png"></p>
<p align="center"><img width=40% src="https://github.com/TBFY/harvester/blob/master/logo.png"></p>

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
![Java](https://img.shields.io/badge/java-v1.8+-blue.svg)
![Maven](https://img.shields.io/badge/maven-v3.0+-blue.svg)
[![](https://jitci.com/gh/TBFY/harvester/svg)](https://jitci.com/gh/TBFY/harvester)
[![Build Status](https://travis-ci.org/TBFY/harvester.svg?branch=master)](https://travis-ci.org/TBFY/harvester)
[![Release Status](https://jitci.com/gh/TBFY/harvester/svg)](https://jitci.com/gh/TBFY/harvester)
[![GitHub Issues](https://img.shields.io/github/issues/TBFY/harvester.svg)](https://github.com/TBFY/harvester/issues)
[![License](https://img.shields.io/badge/license-Apache2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)




## Basic Overview

Download articles and legal documents from public procurement sources:
- Tender and contract data of European government bodies from [OpenOpps](https://openopps.com) via [API](http://theybuyforyou.eu/openopps-api/) or Amazon-S3 bucket (*credentials are required*)
- Legislative texts via [JRC-Acquis](https://ec.europa.eu/jrc/en/language-technologies/jrc-acquis) dataset.
- Public procurement notices via [TED](https://ted.europa.eu/) dataset.

And index them into [SOLR](http://lucene.apache.org/solr/) to perform complex queries and visualize results through [Banana](https://github.com/lucidworks/banana).

## Quick Start

1. Install [Docker](https://docs.docker.com/install/) and [Docker-Compose](https://docs.docker.com/compose/install/) 
1. Clone this repo

	```
	git clone https://github.com/TBFY/harvester.git
	```
1. Move into `src/test/docker` directory.
1. Run Solr and Banana by: `docker-compose up -d`
1. You should be able to monitor the progress by: `docker-compose logs -f`
1. A Solr Admin site should be available at: [http://localhost:8983/solr](http://localhost:8983/solr)
1. Rename the configuration file: `src/test/resources/credentials.properties.sample` to `src/test/resources/credentials.properties` (*if you have credentials, update its content*) 
1. Download and extract TED articles from [ftp://guest:guest@ted.europa.eu/daily-packages/](ftp://guest:guest@ted.europa.eu/daily-packages/) and save them at: `input/ted`
1. Move into base directory and run our harvester by: `./test TEDHarvester`
1. A dashboard with results should be available at: [http://localhost:8983/solr/banana](http://localhost:8983/solr/banana)


Take a look at all our harvesters here:  `src/test/java/harvest/`. 

## Last Stable Release [![](https://jitpack.io/v/TBFY/harvester.svg)](https://jitpack.io/#TBFY/harvester)
Step 1. Add the JitPack repository to your build file
```xml
        <repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
```
Step 2. Add the dependency
```xml
        <dependency>
	    <groupId>com.github.TBFY</groupId>
	    <artifactId>harvester</artifactId>
	    <version>last-stable-release-version</version>
	</dependency>
```

## Contributing
Please take a look at our [contributing](https://github.com/TBFY/harvester/blob/master/CONTRIBUTING.md) guidelines if you're interested in helping!
