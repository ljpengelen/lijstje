(ns lijstje.domain 
  (:require [lijstje.db :as db]))

(def secure-random (java.security.SecureRandom.))

(defn random-bytes [number-of-bytes]
  (let [bytes (byte-array number-of-bytes)]
    (.nextBytes secure-random bytes)
    bytes))

(def hex-format (java.util.HexFormat/of))

(defn bytes->hex-string [bytes]
  (.formatHex hex-format bytes))

(defn hex-string->bytes [hex-string]
  (.parseHex hex-format hex-string))

(defn random-hex-string [number-of-bytes]
  (-> number-of-bytes
      random-bytes
      bytes->hex-string))

(comment
  (random-bytes 100)
  (bytes->hex-string (random-bytes 8))
  (random-hex-string 16)
  (hex-string->bytes "3d3d93b4c9bc8aa07d2d0c2acd87e7c3"))

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

(defn create-gift! [private-list-id name price description]
  (if-let [{:keys [list-id]} (db/get-list-by-private-id db/ds {:id private-list-id})]
    (let [gift (new-gift list-id name price description)]
      (db/create-gift! db/ds gift))
    (throw (ex-info "List not found" {:ui-message "Deze lijst bestaat niet."}))))

(comment
  (create-gift! 1 "Koffiebonen" "10,-" "Bruin"))

(defn extend-list-with-gifts [{:keys [list-id] :as list}]
  (let [gifts (db/get-gifts-by-list-id db/ds {:id list-id})]
    (assoc list :gifts gifts)))

(defn get-list-by-public-id [id]
  (if-let [list (db/get-list-by-public-id db/ds {:id id})]
    (extend-list-with-gifts list)
    (throw (ex-info "List not found" {:ui-message "Deze lijst bestaat niet."}))))

(defn get-list-by-private-id [id]
  (if-let [list (db/get-list-by-private-id db/ds {:id id})]
    (extend-list-with-gifts list)
    (throw (ex-info "List not found" {:ui-message "Deze lijst bestaat niet."}))))

(comment
  (get-list-by-public-id "812b098ce4bd6725")
  (get-list-by-private-id "38719a1be66a15e6"))

(defn get-all-lists []
  (for [list (db/get-all-lists db/ds)]
    (extend-list-with-gifts list)))

(comment
  (get-all-lists))

(defn get-gift [external-id]
  (if-let [gift (db/get-gift-by-external-id db/ds {:id external-id})]
    gift
    (throw (ex-info "Gift not found" {:ui-message "Dit cadeau bestaat niet."}))))

(defn reserve-gift! [external-id reserved-by]
  (db/reserve-gift! db/ds {:external-id external-id
                           :reserved-by reserved-by
                           :reserved-at (str (java.time.Instant/now))}))

(comment
  (reserve-gift! "c544781ff5411936" "Kerstman"))

(defn cancel-gift-reservation! [external-id]
  (db/cancel-gift-reservation! db/ds {:external-id external-id}))

(defn delete-list! [private-list-id]
  (db/delete-gifts-by-private-list-id! db/ds {:id private-list-id})
  (db/delete-list-by-private-id! db/ds {:id private-list-id}))

(defn update-gift! [external-id name price description]
  (db/update-gift! db/ds {:id external-id
                          :name name
                          :price price
                          :description description}))

(defn delete-gift! [external-id]
  (db/delete-gift! db/ds {:id external-id}))
