(ns lijstje.logging
  (:require [clojure.tools.logging :as log]
            [clojure.tools.logging.impl :as impl]
            [lijstje.sentry :as sentry]))

(defprotocol Logger
  (-log [this ns level message throwable]))

(defmacro log [logger level message throwable]
  `(-log ~logger ~*ns* ~level ~message ~throwable))

(defmacro log-info! [logger message]
  `(log ~logger :info ~message nil))

(defmacro log-warning! [logger message throwable]
  `(log ~logger :warn ~message ~throwable))

(defmacro log-error! [logger message throwable]
  `(log ~logger :error ~message ~throwable))

(defn uncaught-exception-handler [logger]
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ thread exception]
      (let [message (str "Uncaught exception on thread " (.getName thread))]
        (log-error! logger message exception)))))

(defn create-logger [sentry-client]
  (reify Logger
    (-log [_ ns level message throwable]
      (let [logger (impl/get-logger log/*logger-factory* ns)]
        (log/log* logger level throwable message))
      (when throwable
        (sentry/capture-exception! sentry-client throwable)))))
