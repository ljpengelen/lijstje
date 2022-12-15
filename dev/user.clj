(ns user
  (:require [clojure.java.browse :refer [browse-url]]
            [config.core :refer [reload-env]]
            [lijstje.migrations :as migrations]
            [lijstje.server :as server]
            [migratus.core :as migratus]))

(comment
  (server/start!)
  (browse-url "http://localhost:3000/")
  (server/stop!)
  (migrations/migrate!)
  (migrations/rollback!)
  (migratus/create nil "gift-external-id")
  (reload-env))
