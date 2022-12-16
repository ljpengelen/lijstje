(ns user
  (:require [clojure.java.browse :refer [browse-url]]
            [config.core :refer [env reload-env]]
            [integrant.repl :refer [go halt reset set-prep!]]
            [lijstje.core :refer [system-config]]
            [lijstje.domain :as domain]
            [lijstje.migrations :as migrations]
            [migratus.core :as migratus]))

(set-prep! (constantly system-config))

(comment
  (go)
  (browse-url "http://localhost:3000/")
  (reset)
  (halt))

(comment
  (reload-env))

(comment
  (migrations/migrate!)
  (migrations/rollback!)
  (migratus/create nil "gift-external-id"))

(comment
  (domain/get-all-lists (:jdbc-url env)))
