(ns lijstje.sentry
  (:require [clojure.tools.logging :as logger]) 
  (:import [io.sentry Sentry Sentry$OptionsConfiguration]))

(defn configurer [dsn environment]
  (reify Sentry$OptionsConfiguration
    (configure [_ options]
      (doto options
        (.setDsn dsn)
        (.setEnvironment environment)
        (.setTracesSampleRate 0)
        (.setDebug true)))))

(defn capture! [exception]
  (Sentry/captureException exception))

(def uncaught-exception-handler
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ thread exception]
      (let [message (str "Uncaught exception on thread " (.getName thread))]
        (clojure.tools.logging/error exception message)
        (capture! exception)))))

(defn init! [dsn environment]
  (Sentry/init (configurer dsn environment))
  (Thread/setDefaultUncaughtExceptionHandler uncaught-exception-handler))
