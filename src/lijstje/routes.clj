(ns lijstje.routes
  (:require [lijstje.handlers :as h]
            [reitit.ring :as ring]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]))

(defn wrap-pretty-exceptions [handler logger]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (h/handle-exception e logger)))))

(defn no-caching-response [response]
  (assoc-in response [:headers "Cache-Control"] "no-cache, no-store"))

(defn wrap-no-caching [handler]
  (fn [request]
    (no-caching-response (handler request))))

(defn wrap-state [handler state]
  (fn [request]
    (handler state request)))

(defn app [{:keys [cookie-store logger] :as state}]
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
                     :post h/delete-gift}]]]]]
     ["/info" {:get h/render-info-object}]]
    {:data {:middleware [[wrap-pretty-exceptions logger]
                         wrap-params
                         wrap-keyword-params
                         wrap-no-caching
                         [wrap-session {:store cookie-store}]
                         [wrap-anti-forgery {:error-response h/invalid-request-page}]
                         [wrap-state state]]}})
   (ring/routes
    (ring/create-resource-handler
     {:path "/"})
    (ring/create-default-handler
     {:not-found (constantly {:status 404 :body "Not found"})}))))
