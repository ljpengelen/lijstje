(ns lijstje.routes
  (:require [config.core :refer [env]]
            [lijstje.domain :as domain]
            [lijstje.handlers :as h]
            [reitit.ring :as ring]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]))

(defn wrap-pretty-exceptions [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (if (and (instance? clojure.lang.ExceptionInfo e) (:ui-message (ex-data e)))
          (h/render-domain-exception-page e)
          (do
            (println e)
            h/internal-server-error-page))))))

(defn no-caching-response [response]
  (assoc-in response [:headers "Cache-Control"] "no-cache, no-store"))

(defn wrap-no-caching [handler]
  (fn [request]
    (no-caching-response (handler request))))

(def store (cookie-store {:key (domain/hex-string->bytes (:cookie-key env))}))

(def app
  (ring/ring-handler
   (ring/router
    [["/" {:get h/render-create-list-page
           :post h/create-list}]
     ["/list/:external-list-id"
      ["/delete" {:get h/render-delete-list-page
                  :post h/delete-list}]
      ["/view"
       ["" h/render-view-list-page]
       ["/gift/:external-gift-id"
        ["/reserve" {:get h/render-reserve-gift-page
                     :post h/reserve-gift}]
        ["/cancel-reservation" {:get h/render-cancel-gift-reservation-page
                                :post h/cancel-gift-reservation}]]]
      ["/edit" 
       ["" h/render-edit-list-page]
       ["/gift"
        ["" {:get h/render-create-gift-page
             :post h/create-gift}]
        ["/:external-gift-id"
         ["/edit" {:get h/render-edit-gift-page
                   :post h/update-gift}]
         ["/delete" {:get h/render-delete-gift-page
                     :post h/delete-gift}]]]]]]
    {:data {:middleware [wrap-pretty-exceptions
                         wrap-params
                         wrap-keyword-params
                         wrap-no-caching
                         [wrap-session {:store store}]
                         [wrap-anti-forgery {:error-response h/invalid-request-page}]]}})
   (ring/routes
    (ring/create-resource-handler
     {:path "/"})
    (ring/create-default-handler
     {:not-found (constantly {:status 404 :body "Not found"})}))))
