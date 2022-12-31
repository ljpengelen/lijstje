(ns lijstje.sentry
  (:import [io.sentry Sentry Sentry$OptionsConfiguration]))

(defprotocol SentryClient
  (capture-exception! [this exception]))

(defn configurer [dsn environment]
  (reify Sentry$OptionsConfiguration
    (configure [_ options]
      (doto options
        (.setDsn dsn)
        (.setEnvironment environment)
        (.setTracesSampleRate 0)
        (.setDebug true)))))

(defn create-sentry-client [dsn environment]
  (Sentry/init (configurer dsn environment))
  (reify SentryClient
    (capture-exception!
      [_ exception]
      (Sentry/captureException exception))))
