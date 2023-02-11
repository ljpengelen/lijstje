(ns lijstje.graal 
  (:require [ring.util.response :as response])
  (:import [java.util Date]))

(defn content-length [^java.net.URLConnection conn]
  (let [len (.getContentLengthLong conn)]
    (when (<= 0 len) len)))

(defn last-modified [^java.net.URLConnection conn]
  (let [last-mod (.getLastModified conn)]
    (when-not (zero? last-mod)
      (Date. last-mod))))

(defmethod response/resource-data :resource
  [^java.net.URL url]
  (let [resource (.openConnection url)
        length (content-length resource)]
    (when (pos? length)
      {:content (.getInputStream resource)
       :content-length length
       :last-modified (last-modified resource)})))
