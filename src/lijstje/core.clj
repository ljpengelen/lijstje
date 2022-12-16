(ns lijstje.core
  (:require [config.core :refer [env]]
            [integrant.core :as ig]
            [lijstje.domain :as domain]
            [lijstje.migrations :as migrations]
            [lijstje.routes :refer [app]]
            [org.httpkit.server :as http-kit]
            [ring.middleware.session.cookie :refer [cookie-store]])
  (:gen-class))

(def system-config
  {::cookie-store {:cookie-key (:cookie-key env)}
   ::datasource {:jdbc-url (:jdbc-url env)}
   ::server {:cookie-store (ig/ref ::cookie-store)
             :datasource (ig/ref ::datasource)
             :host (:host env)
             :port (:port env)}})

(defmethod ig/init-key ::cookie-store [_ {:keys [cookie-key]}]
  (cookie-store {:key (domain/hex-string->bytes cookie-key)}))

(defmethod ig/init-key ::datasource [_ {:keys [jdbc-url]}]
  jdbc-url)

(defmethod ig/init-key ::server [_ {:keys [port] :as state}]
  (http-kit/run-server (app state) {:port port :join? false}))

(defmethod ig/halt-key! ::server [_ server]
  (server))

(defn -main [& args]
  (if (some #{"migrate"} args)
    (migrations/migrate!)
    (ig/init system-config)))
