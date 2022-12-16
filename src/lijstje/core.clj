(ns lijstje.core
  (:require [config.core :refer [env]]
            [integrant.core :as ig]
            [lijstje.migrations :as migrations]
            [lijstje.routes :refer [app]]
            [org.httpkit.server :as http-kit])
  (:gen-class))

(def system-config
  {::datasource {:jdbc-url (:jdbc-url env)}
   ::server {:cookie-key (:cookie-key env)
             :datasource (ig/ref ::datasource)
             :host (:host env)
             :port (:port env)}})

(defmethod ig/init-key ::datasource [_ {:keys [jdbc-url]}]
  jdbc-url)

(defmethod ig/init-key ::server [_ {:keys [port] :as value}]
  (let [state (select-keys value [:cookie-key :datasource :host])]
    (http-kit/run-server (app state) {:port port :join? false})))

(defmethod ig/halt-key! ::server [_ server]
  (server))

(defn -main [& args]
  (if (some #{"migrate"} args)
    (migrations/migrate!)
    (ig/init system-config)))
