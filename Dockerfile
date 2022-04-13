FROM openjdk:18-jdk-slim-bullseye

RUN apt-get update
RUN apt-get install -y npm maven
RUN apt-get install -y python3
RUN apt-get install -y python3-pip
RUN apt-get install -y tesseract-ocr
RUN pip3 install requests

COPY . /app
WORKDIR /app/frontend

RUN cp package-server.json package.json
RUN npm install
RUN cp src/endpoint_deploy.js src/endpoint.js
RUN npm run-script build

RUN cp -r build/* /app/src/main/webapp

WORKDIR /app

RUN mvn clean package -DskipTests
RUN mv target/intuition-0.99.1.war .

CMD ["sh", "start.sh"]

