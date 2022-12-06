(ns lijstje.handlers 
  (:require [hiccup.util :refer [escape-html]]
            [ring.util.response :as rr])
   (:import [org.nibor.autolink LinkExtractor LinkSpan LinkType]))

(def link-extractor (-> (LinkExtractor/builder)
                        (.linkTypes #{LinkType/URL LinkType/WWW})
                        (.build)))

(defn escape-html-and-autolink [input]
  (let [spans (.extractSpans link-extractor input)]
    (for [span spans
          :let [begin (.getBeginIndex span)
                end (.getEndIndex span)
                text (subs input begin end)]]
      (if (instance? LinkSpan span)
        (str "<a target=\"_blank\" href=\"" text "\">" text "</a>")
        (escape-html text)))))

(defn render-create-list-page [_request]
  (rr/response "Yo"))
