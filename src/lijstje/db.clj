(ns lijstje.db
  (:require [config.core :refer [env]]
            [hugsql.adapter.next-jdbc :as next-adapter]
            [hugsql.core :as hugsql]))

(hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc))

(hugsql/def-db-fns "lijstje/db.sql")
(declare create-list! get-list-by-public-id get-list-by-private-id
         get-all-lists create-gift! get-gifts-by-list-id
         reserve-gift!)

(def ds (:jdbc-url env))
