# Lijstje

A web app for wish lists.

## Requirements

* [Java 17+](https://adoptium.net/)
* [Clojure](https://clojure.org/)

## Development

If you're a beginner to Clojure and don't have a favorite setup yet, give [Visual Studio Code](https://code.visualstudio.com/) in combination with the [Calva extension](https://calva.io/) a try.

Once you've installed Visual Studio Code and Calva, [connect Calva to the project](https://calva.io/connect/) using the project type `deps.edn` and the alias `:dev`, and start development.

A convenient way to get started is opening `dev/user.clj` and evaluating expressions using `alt+enter`.

## Running the app

Run `clojure -X:run` to start the app.
Obviously, this requires Clojure.

Alternatively, run `clojure -X:uberjar` to create an uberjar, followed by `java -jar target/lijstje-<version>-standalone.jar` to start the application.
Starting the application like this doesn't require Clojure, only Java.

## Running migrations

Use `clojure -X:migrate` to run migrations via Clojure.
Use `java -jar target/lijstje-<version>-standalone.jar migrate` to run migrations via Java.

## Creating a native image

Use the GraalVM tracing agent to find out which code is reachable and capture this information in the form of configuration
files by first executing the following command:

```
java -agentlib:native-image-agent=config-output-dir=resources/META-INF/native-image/generated -jar target/lijstje-<version>-standalone.jar
```

While the app is running, perform some requests to allow the tracing agent to do its work.

Afterwards, execute the following command to create a native image:

```
native-image -jar target/lijstje-<version>-standalone.jar --no-fallback \
--initialize-at-build-time=ch.qos.logback \
--initialize-at-build-time=org.slf4j \
target/lijstje
```
