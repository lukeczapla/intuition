## OncoKB tool for curation prototypes and other services
# Literature Automation Platform + MongoDB + SOLR

RECENT CHANGES: The LDAP/SAML was deactivated, any current name (e.g. MSK ID) in the MongoDB
still works (with any password) and the endpoint is now "/intuition", it was never fully 'knowledge' because curators rely on intuition about repeated observations from reading the text as a career, that is/has been the most profitable way to find the functional sentences in the demo frontend.  
REQUIRED: The one greatest biggest criteria is on the PDF2HTML (and the other paragraph / structured table + figures endpoint delivered as JSON) for the advanced cases, selling this was never a goal but it's almost like it has to be good enough to ace the edge cases (latest graphs in some PMIDs >~ 33000000 encoded with newer approaches - great for PDF highlighting but bad for sectionalizing the photo/image components) to fully upgrade to the newest pipeline.

## (More details to be added)

# Intro - Quick Summary

This program populates the Mongo database and Solr search with the Articles, between gene names, cancer names, drug names, and mutation names and 
pmID of relevant articles (more generally, all entities).  The purpose of consumption is to fulfill queries 
from curators and report relevant articles / verify content sorted into categories with topics highlighted.  
PDFs are parsed and destructured (pdf package), and an older Tika pipeline framework exists, as well.

## Usage / Debug (port 7050)

MongoConfig.java contains MongoDB config/port information, SolrClientTool.java 
contains Solr config/port information


```bash

mvn spring-boot:run

```

That will leave up the service to work with the system backend and frontend.


 - Match articles to the oncokb_braf_tp53_ros1_pmids.xlsx file (place in
intuition folder) - or use bash script processSpreadsheet:

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
  intuition:
    build:
      context: ./intuition
    restart: always
    ports:
      - 7050:7050

```

---------------------

The port 7050 may be closed (two lines starting with "ports:") after establishing
a Docker reverse proxy (e.g. NGINX) or similar technology in production

solr.apache.org provides the Dockerfile for SOLR 8.11.1 and the latest MongoDB Community Edition amd64/mongo is available on Docker Hub (Ubuntu Focal with MongoDB CE 5.0.x)

Relevant URLs:

http://localhost:7050/intuition/swagger-ui/

The Swagger documentation for the backend.


## Old deployment on AIML server (internal use)

https://aimlcoe.mskcc.org/knowledge/swagger-ui/



#### P.P.S. also contains a few AI/ML endpoints for optimizing under limited sampling sizes with randomization tests, just some demos of DL4j with dropout regularization plus reweighting, where the TensorFlow backend can be substituted in or a Keras model written out for Python tensorflow.keras.  It's a survival of the fittest (evolutionary) approach to deep learning networks where the maximum-impact factors are the objective criteria for identification.  Seemed a valid alternative approach to, e.g., boosted trees (xgboost) with limited objective weights strongly dependent on randomization criteria.
