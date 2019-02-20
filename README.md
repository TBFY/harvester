# harvester

Download articles and legal documents on public procurement.

## Features
- Tender and contract data from a range of European government bodies from [OpenOpps](https://openopps.com) via [API](http://theybuyforyou.eu/openopps-api/) or Amazon-S3 bucket (*credentials are required*)
- Legislative texts from the European Union generated between years 1958 and 2006 in 22 languages via [JRC-Acquis](https://ec.europa.eu/jrc/en/language-technologies/jrc-acquis) dataset.
- Public procurement notices from the European Union via [TED](https://ted.europa.eu/) dataset.
- All of them are indexed in [SOLR](http://lucene.apache.org/solr/) to perform complex queries and visualize results through [Banana](https://github.com/lucidworks/banana).

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

