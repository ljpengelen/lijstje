(ns lijstje.db
  (:require [hugsql.adapter.next-jdbc :as next-adapter]
            [hugsql.core :as hugsql]))

(hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc))

(hugsql/def-db-fns "lijstje/db.sql")
(declare create-list! get-list-by-public-id get-list-by-private-id
         get-all-lists create-gift! get-gifts-by-list-id
         get-gift-by-external-id reserve-gift! cancel-gift-reservation!
         delete-list-by-private-id! delete-gifts-by-private-list-id!
         update-gift! delete-gift!)
