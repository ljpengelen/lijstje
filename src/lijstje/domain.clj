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

(defn create-list! [datasource name]
  (let [list (new-list name)]
    (db/create-list! datasource list)))

(defn create-gift! [datasource private-list-id name price description]
  (if-let [{:keys [list-id]} (db/get-list-by-private-id datasource {:id private-list-id})]
    (let [gift (new-gift list-id name price description)]
      (db/create-gift! datasource gift))
    (throw (ex-info "List not found" {:ui-message "Deze lijst bestaat niet."}))))

(defn extend-list-with-gifts [datasource {:keys [list-id] :as list}]
  (let [gifts (db/get-gifts-by-list-id datasource {:id list-id})]
    (assoc list :gifts gifts)))

(defn get-list-by-public-id [datasource id]
  (if-let [list (db/get-list-by-public-id datasource {:id id})]
    (extend-list-with-gifts datasource list)
    (throw (ex-info "List not found" {:ui-message "Deze lijst bestaat niet."}))))

(defn get-list-by-private-id [datasource id]
  (if-let [list (db/get-list-by-private-id datasource {:id id})]
    (extend-list-with-gifts datasource list)
    (throw (ex-info "List not found" {:ui-message "Deze lijst bestaat niet."}))))

(defn get-all-lists [datasource]
  (for [list (db/get-all-lists datasource)]
    (extend-list-with-gifts datasource list)))

(defn get-gift [datasource external-id]
  (if-let [gift (db/get-gift-by-external-id datasource {:id external-id})]
    gift
    (throw (ex-info "Gift not found" {:ui-message "Dit cadeau bestaat niet."}))))

(defn reserve-gift! [datasource external-id reserved-by]
  (db/reserve-gift! datasource {:external-id external-id
                                :reserved-by reserved-by
                                :reserved-at (str (java.time.Instant/now))}))

(defn cancel-gift-reservation! [datasource external-id]
  (db/cancel-gift-reservation! datasource {:external-id external-id}))

(defn delete-list! [datasource private-list-id]
  (db/delete-gifts-by-private-list-id! datasource {:id private-list-id})
  (db/delete-list-by-private-id! datasource {:id private-list-id}))

(defn update-gift! [datasource external-id name price description]
  (db/update-gift! datasource {:id external-id
                               :name name
                               :price price
                               :description description}))

(defn delete-gift! [datasource external-id]
  (db/delete-gift! datasource {:id external-id}))
