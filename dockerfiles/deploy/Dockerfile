FROM clojure:tools-deps-jammy

RUN groupadd -g 999 appuser \
  && useradd -r -u 999 -g appuser appuser -s /bin/false

EXPOSE 3000

COPY . .

RUN clojure -T:build clean
RUN clojure -T:build uber

RUN cp target/lijstje-*-standalone.jar target/lijstje-standalone.jar

USER appuser

CMD ["java", "-jar", "target/lijstje-standalone.jar"]
