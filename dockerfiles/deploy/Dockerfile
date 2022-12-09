FROM clojure:tools-deps-jammy AS build

RUN $JAVA_HOME/bin/jlink \
         --add-modules java.base,java.desktop,java.naming,java.sql,jdk.unsupported \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output /javaruntime

COPY deps.edn .
RUN clojure -M -e "(prn \"Downloading deps\")"

COPY . .
RUN clojure -T:build clean
RUN clojure -T:build uber
RUN cp target/lijstje-*-standalone.jar /lijstje-standalone.jar

FROM debian:buster-slim

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH "${JAVA_HOME}/bin:${PATH}"
COPY --from=build /javaruntime $JAVA_HOME

RUN mkdir /app
COPY --from=build /lijstje-standalone.jar /app

RUN groupadd -g 999 appuser \
  && useradd -r -u 999 -g appuser appuser -s /bin/false
WORKDIR /app
RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 3000

CMD ["java", "-jar", "lijstje-standalone.jar"]
