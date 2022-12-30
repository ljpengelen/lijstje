(ns lijstje.logging
  (:require [clojure.tools.logging :as log]
            [lijstje.sentry :as sentry]))

(defprotocol Logger
  (log-info! [this message])
  (log-error! [this message throwable])
  (log-warning! [this message throwable]))

(defn uncaught-exception-handler [logger]
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ thread exception]
      (let [message (str "Uncaught exception on thread " (.getName thread))]
        (log-error! logger message exception)))))

(defn init! [sentry-client]
  (let [logger (reify Logger
                 (log-info!
                   [_ message]
                   (log/info message))
                 (log-error!
                   [_ message throwable]
                   (log/error throwable message)
                   (sentry/capture-exception! sentry-client throwable))
                 (log-warning!
                   [_ message throwable]
                   (log/warn throwable message)
                   (sentry/capture-exception! sentry-client throwable)))]
    (Thread/setDefaultUncaughtExceptionHandler
     (uncaught-exception-handler logger))
    logger))
