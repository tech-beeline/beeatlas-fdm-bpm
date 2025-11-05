ARG IMAGE_REGISTRY
ARG SLIM=''
FROM ${IMAGE_REGISTRY}/dockerhub/library/openjdk:17${SLIM}-buster
ENV DEBIAN_FRONTEND=noninteractive
RUN apt update && apt install -y --no-install-recommends \
        ca-certificates \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /app
RUN useradd -m -d /app -s /bin/bash -g root -u 1000 app && chown app:root .
USER app
COPY build/*.jar app.jar
COPY *.sh .
COPY runapp-java.sh runapp.sh
COPY *.crt .
USER root
COPY *.crt /usr/local/share/ca-certificates
RUN update-ca-certificates
RUN update-ca-certificates && \
    keytool -importcert -file VC_RootCA.crt -alias VC_RootCA -keystore  $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt -trustcacerts &&  \
    keytool -importcert -file VC_BEERootCA.crt -alias VC_BEERootCA -keystore  $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt -trustcacerts &&  \
    keytool -import -alias apps.yd-m6-kt22.vimpelcom -keystore  $JAVA_HOME/lib/security/cacerts -file apps.yd-m6-kt22.vimpelcom.ru.crt -storepass changeit &&  \
    keytool -import -alias inside-beeline-ru-crt -keystore  $JAVA_HOME/lib/security/cacerts -file inside.beeline.ru.crt -storepass changeit
USER app
RUN keytool -importcert -file VC_RootCA.crt -alias VC_RootCA -storepass changeit -noprompt -trustcacerts &&  \
    keytool -importcert -file VC_BEERootCA.crt -alias VC_BEERootCA -storepass changeit -noprompt -trustcacerts &&  \
    keytool -importcert -file apps.yd-m6-kt22.vimpelcom.ru.crt -alias apps.yd-m6-kt22.vimpelcom -storepass changeit -noprompt -trustcacerts &&  \
    keytool -importcert -file inside.beeline.ru.crt -alias inside.beeline.ru.crt -storepass changeit -noprompt -trustcacerts

EXPOSE 8080/tcp
EXPOSE 8090/tcp
EXPOSE 10260/tcp
ENTRYPOINT [ "/bin/bash", "/app/run.sh" ]