(ns lijstje.core
  (:require [config.core :refer [env]]
            [integrant.core :as ig]
            [lijstje.db :as db]
            [lijstje.domain :as domain]
            [lijstje.logging :as logging]
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
   ::handler {:cookie-store (ig/ref ::cookie-store)
              :datasource (ig/ref ::datasource)
              :host (:host env)
              :logger (ig/ref ::logger)}
   ::logger {:sentry-client (ig/ref ::sentry-client)}
   ::sentry-client (:sentry env)
   ::server {:handler (ig/ref ::handler)
             :logger (ig/ref ::logger)
             :port (:port env)}})

(defmethod ig/init-key ::db-fns [_ _]
  (db/def-db-fns))

(defmethod ig/init-key ::cookie-store [_ {:keys [cookie-key]}]
  (cookie-store {:key (domain/hex-string->bytes cookie-key)}))

(defmethod ig/init-key ::datasource [_ {:keys [jdbc-url]}]
  jdbc-url)

(defmethod ig/init-key ::logger [_ {:keys [sentry-client]}]
  (logging/init! sentry-client))

(defmethod ig/init-key ::sentry-client [_ {:keys [dsn environment]}]
  (sentry/init! dsn environment))

(defmethod ig/init-key ::handler [_ state]
  (app state))

(defn error-logger [logger]
  (fn [message exception]
    (logging/log-error! logger message exception)))

(defn warn-logger [logger]
  (fn [message exception]
    (logging/log-warning! logger message exception)))

(defmethod ig/init-key ::server [_ {:keys [handler logger port]}]
  (let [threads (* 2 (.availableProcessors (Runtime/getRuntime)))
        options {:error-logger (error-logger logger)
                 :warn-logger (warn-logger logger)
                 :port port
                 :thread threads}]
    (logging/log-info! logger (str "Number of threads used: " threads))
    (http-kit/run-server handler options)))

(defmethod ig/halt-key! ::server [_ server]
  (server))

(defn -main [& args]
  (if (some #{"migrate"} args)
    (migrations/migrate!)
    (ig/init system-config)))
