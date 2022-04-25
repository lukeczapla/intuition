# Literature Automation Platform - MongoDB / Solr

This program populates the Mongo database and Solr search with the Articles, 
mapping between gene names, cancer names, drug names, and mutation names and 
pmID of relevant articles.  The purpose of consumption is to fulfill queries 
from curators and report relevant articles / verify content.

## Usage / Debug (port 7050)

MongoConfig.java contains MongoDB config/port information, SolrClientTool.java 
contains Solr config/port information


```bash

mvn spring-boot:run

```

That will leave up the service to work with the system backend and frontend.


 - Match articles to the oncokb_braf_tp53_ros1_pmids.xlsx file (place in
knowledge folder) - or use bash script processSpreadsheet:

```bash

mvn -Dtest=SpringTest#processSpreadsheet -Dfilename=oncokb_braf_tp53_ros1_pmids.xlsx test

```

 - Read in latest Pubmed title/author/abstract/keywords/MeSH terms and follow 
1st-level citations - or use bash script weeklyUpdate (automatically run every
two days now):

```bash

mvn -Dtest=SpringTest#pullDaily test

```

 - Check that all citations have title/author/abstract/keywords/MeSH terms 
downloaded - or use bash script updateCitations:

```bash

mvn -Dtest=SpringTest#updateCitations test

```
 

## Docker deployment

The Dockerfile is included in the package and a file for docker-compose.yml includes the following:

```
  knowledge:
    build:
      context: ./knowledge
    restart: always
    ports:
      - 7050:7050

```

---------------------

The port 7050 may be closed (two lines starting with "ports:") after establishing
a Docker reverse proxy (e.g. NGINX) or similar technology in production


Relevant URLs:

http://localhost:7050/knowledge/swagger-ui/

The Swagger documentation for the backend.


## Deployment on AIML server (internal use)

https://aimlcoe.mskcc.org/knowledge/swagger-ui/


