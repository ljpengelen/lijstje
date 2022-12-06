(ns lijstje.core
  (:require [lijstje.migrations :as migrations]
            [lijstje.server :as server])
  (:gen-class))

(defn -main [& _]
  (migrations/migrate!)
  (server/start!))
