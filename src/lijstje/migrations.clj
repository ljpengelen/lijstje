(ns lijstje.migrations
  (:require [config.core :refer [env]]
            [migratus.core :as migratus]))

(def config {:store :database
             :db {:connection-uri (:jdbc-url env)}})

(defn migrate! []
  (migratus/migrate config))

(defn rollback! []
  (migratus/rollback config))
