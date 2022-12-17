(ns lijstje.logging
  (:require [clojure.tools.logging :as log]))

(defn log-error! [capture-exception! message throwable]
  (log/error throwable message)
  (capture-exception! throwable))

(defn log-warning! [capture-exception! message throwable]
  (log/warn throwable message)
  (capture-exception! throwable))

(defn uncaught-exception-handler [capture-exception!]
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ thread exception]
      (let [message (str "Uncaught exception on thread " (.getName thread))]
        (log-error! capture-exception! message exception)))))

(defn init! [capture-exception!]
  (Thread/setDefaultUncaughtExceptionHandler
   (uncaught-exception-handler capture-exception!)))
