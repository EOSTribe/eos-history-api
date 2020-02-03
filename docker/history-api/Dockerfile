FROM openjdk:8-jre-alpine
#RUN apt-get install git && \
#    git clone https://github.com/EOSTribe/websocket-endpoint && \
#    cd websocket-endpoint && \
#    git checkout  elastic-publisher && \
#    gradle clean build
COPY build/eosio-history-api-0.1.21.jar /usr/app/app.jar
ENTRYPOINT ["java", "-jar", "/usr/app/app.jar"]
