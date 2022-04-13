# Literature Frontend

This program populates the Mongo database with the Articles, and maps between 
gene names, cancer names, drug names, and mutation names and pmID of relevant 
articles.  The purpose of consumption is to fulfill queries from curators and
report relevant articles.

## Usage

Running demo version on port 3000 (localhost:3000/knowledge)

```bash
npm install
npm start
```

Compiling production version for the server (usually tested on localhost:7050/knowledge):

```bash
./make
```

Testing the final version locally:

```bash

cd ..
mvn spring-boot:run

```

This will leave up the webservice and frontend to work with the MongoDB system.


## Docker deployment

The Dockerfile is included in the product and a file for docker-compose.yml includes the following:

```
version: 3

  intuition:
    build:
      context: ./intuition
    restart: always
    ports:
      - 7050:7050


```


### Relevant URLs (on local machine):

 - http://localhost:7050/intuition

The testing production version of the full system

 - http://localhost:3000/intuition

Testing production for the frontend (with endpoint_test.js)

 - http://localhost:7050/intuition/swagger-ui/

The Swagger documentation for the backend.


