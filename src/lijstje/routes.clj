(ns lijstje.routes
  (:require [lijstje.handlers :as h]
            [reitit.ring :as ring]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]))

(defn no-caching-response [response]
  (assoc-in response [:headers "Cache-Control"] "no-cache, no-store"))

(defn wrap-no-caching [handler]
  (fn [request]
    (no-caching-response (handler request))))

(def app
  (ring/ring-handler
   (ring/router
    [["/" h/render-create-list-page]
     ["/list/:external-id"
      ["/view" h/render-create-list-page]
      ["/edit" h/render-create-list-page]]
     ["/gift/:external-id" h/render-create-list-page]]
    {:data {:middleware [wrap-params
                         wrap-keyword-params
                         wrap-no-caching]}})
   (ring/routes
    (ring/create-default-handler
     {:not-found (constantly {:status 404 :body "Not found"})}))))

(app {:uri "/" :request-method :get})
