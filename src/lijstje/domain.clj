(ns lijstje.domain 
  (:require [lijstje.db :as db]))

(defn random-bytes [number-of-bytes]
  (let [secure-random (java.security.SecureRandom.)
        bytes (byte-array number-of-bytes)]
    (.nextBytes secure-random bytes)
    bytes))

(defn bytes->hex-string [bytes]
  (let [hex-format (java.util.HexFormat/of)]
    (.formatHex hex-format bytes)))

(defn random-hex-string [number-of-bytes]
  (-> number-of-bytes
      random-bytes
      bytes->hex-string))

(comment
  (random-bytes 100)
  (bytes->hex-string (random-bytes 8))
  (random-hex-string 16))

(defn new-list [name]
  {:name name
   :public-external-id (random-hex-string 8)
   :private-external-id (random-hex-string 8)})

(defn new-gift [list-id name price description]
  {:list-id list-id
   :name name
   :price price
   :description description
   :external-id (random-hex-string 8)})

(comment
  (new-list "Lijstje van Luc")
  (new-gift 1 "Koffiebonen" "10,-" "Bruin"))

(defn create-list! [name]
  (let [list (new-list name)]
    (db/create-list! db/ds list)))

(comment
  (create-list! "Kerst Luc"))

(defn create-gift! [list-id name price description]
  (let [gift (new-gift list-id name price description)]
    (db/create-gift! db/ds gift)))

(comment
  (create-gift! 1 "Koffiebonen" "10,-" "Bruin"))

(defn extend-list-with-gifts [{:keys [list-id] :as list}]
  (when list
    (let [gifts (db/get-gifts-by-list-id db/ds {:id list-id})]
      (assoc list :gifts gifts))))

(defn get-list-by-public-id [id]
  (extend-list-with-gifts (db/get-list-by-public-id db/ds {:id id})))

(defn get-list-by-private-id [id]
  (extend-list-with-gifts (db/get-list-by-private-id db/ds {:id id})))

(comment
  (get-list-by-public-id "812b098ce4bd6725")
  (get-list-by-private-id "28d9bad1e3099bf9"))

(defn get-all-lists []
  (for [list (db/get-all-lists db/ds)]
    (extend-list-with-gifts list)))

(comment
  (get-all-lists))

(defn reserve-gift! [external-id reserved-by]
  (db/reserve-gift! db/ds {:external-id external-id
                           :reserved-by reserved-by
                           :reserved-at (str (java.time.Instant/now))}))

(comment
  (reserve-gift! "c544781ff5411936" "Kerstman"))
