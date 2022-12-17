(ns lijstje.core
  (:require [clojure.tools.logging :as logger]
            [config.core :refer [env]]
            [integrant.core :as ig]
            [lijstje.db :as db]
            [lijstje.domain :as domain]
            [lijstje.migrations :as migrations]
            [lijstje.routes :refer [app]]
            [lijstje.sentry :as sentry]
            [org.httpkit.server :as http-kit]
            [ring.middleware.session.cookie :refer [cookie-store]])
  (:gen-class))

(def system-config
  {::cookie-store {:cookie-key (:cookie-key env)}
   ::datasource {:jdbc-url (:jdbc-url env)}
   ::db-fns nil
   ::sentry (:sentry env)
   ::server {:capture-exception! sentry/capture!
             :cookie-store (ig/ref ::cookie-store)
             :datasource (ig/ref ::datasource)
             :db-fns (ig/ref ::db-fns)
             :host (:host env)
             :port (:port env)
             :sentry (ig/ref ::sentry)}})

(defmethod ig/init-key ::db-fns [_ _]
  (db/def-db-fns))

(defmethod ig/init-key ::cookie-store [_ {:keys [cookie-key]}]
  (cookie-store {:key (domain/hex-string->bytes cookie-key)}))

(defmethod ig/init-key ::datasource [_ {:keys [jdbc-url]}]
  jdbc-url)

(defmethod ig/init-key ::sentry [_ {:keys [dsn environment]}]
  (sentry/init! dsn environment))

(defn error-logger [message exception]
  (clojure.tools.logging/error exception message)
  (sentry/capture! exception))

(defn warning-logger [message exception]
  (clojure.tools.logging/warn exception message)
  (sentry/capture! exception))

(defmethod ig/init-key ::server [_ {:keys [port] :as state}]
  (let [state (dissoc state :db-fns :port :sentry)
        options {:error-logger error-logger
                 :warn-logger warning-logger
                 :join? false
                 :port port}]
    (http-kit/run-server (app state) options)))

(defmethod ig/halt-key! ::server [_ server]
  (server))

(defn -main [& args]
  (if (some #{"migrate"} args)
    (migrations/migrate!)
    (ig/init system-config)))
