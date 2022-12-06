(ns lijstje.server
  (:require [config.core :refer [env]]
            [lijstje.routes :refer [app]]
            [org.httpkit.server :as http-kit]
            [ring.middleware.reload :refer [wrap-reload]]))

(defonce server (atom nil))

(def port (:port env))

(defn start! []
  (when-not @server
    (reset! server (http-kit/run-server (wrap-reload #'app) {:port port :join? false}))))

(defn stop! []
  (when @server
    (@server)
    (reset! server nil)))
