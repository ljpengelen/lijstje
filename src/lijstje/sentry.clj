(ns lijstje.sentry
  (:import [io.sentry Sentry Sentry$OptionsConfiguration]))

(defn configurer [dsn environment]
  (reify Sentry$OptionsConfiguration
    (configure [_ options]
      (doto options
        (.setDsn dsn)
        (.setEnvironment environment)
        (.setTracesSampleRate 0)
        (.setDebug true)))))

(defn init! [dsn environment]
  (Sentry/init (configurer dsn environment)))

(defn capture-exception! [exception]
  (Sentry/captureException exception))
