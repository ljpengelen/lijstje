FROM clojure:tools-deps-jammy

EXPOSE 3000

COPY . .

RUN clojure -T:build clean
RUN clojure -T:build uber

RUN cp target/lijstje-*-standalone.jar target/lijstje-standalone.jar

CMD ["java", "-jar", "target/lijstje-standalone.jar"]
