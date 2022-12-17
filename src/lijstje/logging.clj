(ns lijstje.logging
  (:require [clojure.tools.logging :as log]
            [lijstje.sentry :as sentry]))

(defn log-error! [sentry-client message throwable]
  (log/error throwable message)
  (sentry/capture-exception! sentry-client throwable))

(defn log-warning! [sentry-client message throwable]
  (log/warn throwable message)
  (sentry/capture-exception! sentry-client throwable))

(defn uncaught-exception-handler [sentry-client]
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ thread exception]
      (let [message (str "Uncaught exception on thread " (.getName thread))]
        (log-error! sentry-client message exception)))))

(defn init! [sentry-client]
  (Thread/setDefaultUncaughtExceptionHandler
   (uncaught-exception-handler sentry-client)))
