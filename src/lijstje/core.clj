(ns lijstje.core
  (:require [lijstje.migrations :as migrations]
            [lijstje.server :as server])
  (:gen-class))

(defn -main [& args]
  (if (some #{"migrate"} args)
    (migrations/migrate!)
    (server/start!)))
