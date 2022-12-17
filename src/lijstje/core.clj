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
   ::logging {:capture-exception! sentry/capture-exception!
              :sentry (ig/ref ::sentry)}
   ::sentry (:sentry env)
   ::server {:cookie-store (ig/ref ::cookie-store)
             :datasource (ig/ref ::datasource)
             :db-fns (ig/ref ::db-fns)
             :host (:host env)
             :log-error! (partial logging/log-error! sentry/capture-exception!)
             :log-warning! (partial logging/log-warning! sentry/capture-exception!)
             :logging (ig/ref ::logging)
             :port (:port env)}})

(defmethod ig/init-key ::db-fns [_ _]
  (db/def-db-fns))

(defmethod ig/init-key ::cookie-store [_ {:keys [cookie-key]}]
  (cookie-store {:key (domain/hex-string->bytes cookie-key)}))

(defmethod ig/init-key ::datasource [_ {:keys [jdbc-url]}]
  jdbc-url)

(defmethod ig/init-key ::logging [_ {:keys [capture-exception!]}]
  (logging/init! capture-exception!))

(defmethod ig/init-key ::sentry [_ {:keys [dsn environment]}]
  (sentry/init! dsn environment))

(defmethod ig/init-key ::server [_ {:keys [log-error! log-warning! port] :as state}]
  (let [state (dissoc state :db-fns :logging :port)
        options {:error-logger log-error!
                 :warn-logger log-warning!
                 :join? false
                 :port port}]
    (http-kit/run-server (app state) options)))

(defmethod ig/halt-key! ::server [_ server]
  (server))

(defn -main [& args]
  (if (some #{"migrate"} args)
    (migrations/migrate!)
    (ig/init system-config)))
