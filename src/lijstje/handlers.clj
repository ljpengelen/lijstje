(ns lijstje.handlers 
  (:require [clojure.string :as string]
            [config.core :refer [env]]
            [hiccup.core :refer [h]]
            [hiccup.form :as form]
            [hiccup.page :as hp]
            [lijstje.domain :as domain]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [ring.util.response :as response])
   (:import [org.nibor.autolink LinkExtractor LinkSpan LinkType]))

(def link-extractor
  (-> (LinkExtractor/builder)
      (.linkTypes #{LinkType/URL LinkType/WWW})
      (.build)))

(defn with-protocol [url]
  (if (string/starts-with? url "http")
    url
    (str "http://" url)))

(defn escape-html-and-autolink [input]
  (let [spans (.extractSpans link-extractor input)]
    (for [span spans
          :let [begin (.getBeginIndex span)
                end (.getEndIndex span)
                text (subs input begin end)]]
      (if (instance? LinkSpan span)
        (str "<a target=\"_blank\" href=\"" (with-protocol text) "\">" text "</a>")
        (h text)))))

(defonce compiled-at (System/currentTimeMillis))

(defn page [& content]
  {:status 200
   :headers {"Content-type" "text/html"}
   :body
   (hp/html5
    {:lang "nl"}
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport"
             :content "width=device-width,initial-scale=1"}]
     [:link {:rel "apple-touch-icon"
             :sizes "180x180"
             :href "/apple-touch-icon.png"}]
     [:link {:rel "icon"
             :type "image/png"
             :sizes "32x32"
             :href "/favicon-32x32.png"}]
     [:link {:rel "icon"
             :type "image/png"
             :sizes "16x16"
             :href "/favicon-16x16.png"}]
     [:title "Verlanglijstje"]
     (hp/include-css "/css/reset.css")
     (hp/include-css (str "/css/screen.css?version=" compiled-at))]
    [:body content])})

(defn render-create-list-page [_]
  (page
   [:h1 "Maak een nieuw verlanglijstje"]
   (form/form-to
    [:post "/"]
    (anti-forgery-field)
    [:label "Naam"
     (form/text-field {:required true} :name)]
    (form/submit-button "Maak nieuwe lijst"))))

(defn create-list [request]
  (let [name (-> request :params :name)
        {:keys [private-external-id]} (domain/create-list! name)]
    (response/redirect (str "/list/" private-external-id "/edit") :see-other)))

(defn render-edit-list-page [request]
  (let [{:keys [external-list-id]} (:path-params request)
        {:keys [name gifts public-external-id]} (domain/get-list-by-private-id external-list-id)]
    (page
     [:h1 "Bewerk verlanglijstje " (h name)]
     (for [{:keys [external-id name description price]} gifts]
       (list
        [:p
         (h name) " " (escape-html-and-autolink description) " " (h price)
         [:a {:href (str "/list/" external-list-id "/edit/gift/" external-id "/edit")} "Bewerk cadeau"]
         [:a {:href (str "/list/" external-list-id "/edit/gift/" external-id "/delete")} "Verwijder cadeau"]]))
     [:a {:href (str "/list/" external-list-id "/edit/gift")} "Voeg cadeau toe"]
     [:p "Deel met anderen: " (str (:host env)"/list/" public-external-id "/view")]
     [:a {:href (str "/list/" external-list-id "/delete")} "Verwijder lijstje"])))

(defn render-delete-list-page [request]
  (let [{:keys [external-list-id]} (:path-params request)
        {:keys [name]} (domain/get-list-by-private-id external-list-id)]
    (page
     [:h1 "Verwijder " (h name)]
     [:p "Weet je zeker dat je het verlanglijstje \" " (h name) "\" wilt verwijderen?"]
     (form/form-to
      [:post (str "/list/" external-list-id "/delete")]
      (anti-forgery-field)
      #_{:clj-kondo/ignore [:invalid-arity]}
      (form/submit-button {:name :cancel :formnovalidate true} "Nee, ga terug")
      #_{:clj-kondo/ignore [:invalid-arity]}
      (form/submit-button {:name :ok} "Ja, verwijder lijst")))))

(defn delete-list [request]
  (let [{:keys [external-list-id]} (:path-params request)
        {:keys [ok]} (:params request)]
    (if ok
      (do
        (domain/delete-list! external-list-id)
        (response/redirect "/" :see-other))
      (response/redirect (str "/list/" external-list-id "/edit") :see-other))))

(defn render-view-list-page [request]
  (let [{:keys [external-list-id]} (:path-params request)
        {:keys [name gifts]} (domain/get-list-by-public-id external-list-id)]
    (page
     [:h1 "Verlanglijstje " (h name)]
     (for [{:keys [name description price external-id reserved-by]} gifts]
       (list
        [:p
         {:class (when reserved-by "reserved")}
         (h name) " " (escape-html-and-autolink description) " " (h price)]
        (if reserved-by
          [:a {:href (str "/list/" external-list-id "/view/gift/" external-id "/cancel-reservation")} "Maak reservering ongedaan"]
          [:a {:href (str "/list/" external-list-id "/view/gift/" external-id "/reserve")} "Reserveer cadeau"]))))))

(defn render-create-gift-page [request]
  (let [{:keys [external-list-id]} (:path-params request)]
    (page
     [:h1 "Voeg een cadeau toe"]
     (form/form-to
      [:post (str "/list/" external-list-id "/edit/gift")]
      (anti-forgery-field)
      [:label "Naam"
       (form/text-field {:required true} :name)]
      [:label "Richtprijs"
       (form/text-field {:required true} :price)]
      [:label "Omschrijving"
       (form/text-area {:required true} :description)]
      [:a {:href (str "/list/" external-list-id "/edit")} "Ga terug"]
      #_{:clj-kondo/ignore [:invalid-arity]}
      (form/submit-button {:name :ok} "Voeg cadeau toe")))))

(defn create-gift [request]
  (let [{:keys [external-list-id]} (:path-params request)
        {:keys [name ok price description]} (:params request)]
    (when ok
      (domain/create-gift! external-list-id name price description))
    (response/redirect (str "/list/" external-list-id "/edit") :see-other)))

(defn render-edit-gift-page [request]
  (let [{:keys [external-list-id external-gift-id]} (:path-params request)
        {:keys [name price description]} (domain/get-gift external-gift-id)]
    (page
     [:h1 "Bewerk " (h name)]
     (form/form-to
      [:post (str "/list/" external-list-id "/edit/gift/" external-gift-id "/edit")]
      (anti-forgery-field)
      [:label "Naam"
       (form/text-field {:required true :value name} :name)]
      [:label "Richtprijs"
       (form/text-field {:required true :value price} :price)]
      [:label "Omschrijving"
       #_{:clj-kondo/ignore [:invalid-arity]}
       (form/text-area {:required true} :description description)]
      [:a {:href (str "/list/" external-list-id "/edit")} "Ga terug"]
      #_{:clj-kondo/ignore [:invalid-arity]}
      (form/submit-button {:name :ok} "Sla wijzigingen op")))))

(defn update-gift [request]
  (let [{:keys [external-list-id external-gift-id]} (:path-params request)
        {:keys [name ok price description]} (:params request)]
    (when ok
      (domain/update-gift! external-gift-id name price description))
    (response/redirect (str "/list/" external-list-id "/edit") :see-other)))

(defn render-delete-gift-page [request]
  (let [{:keys [external-list-id external-gift-id]} (:path-params request)
        {:keys [name]} (domain/get-gift external-gift-id)]
    (page
     [:h1 "Verwijder " (h name)]
     [:p "Weet je zeker dat je het cadeau \"" (h name) "\" wilt verwijderen?"]
     (form/form-to
      [:post (str "/list/" external-list-id "/edit/gift/" external-gift-id "/delete")]
      (anti-forgery-field)
      [:a {:href (str "/list/" external-list-id "/edit")} "Nee, ga terug"]
      #_{:clj-kondo/ignore [:invalid-arity]}
      (form/submit-button {:name :ok} "Ja, verwijder cadeau")))))

(defn delete-gift [request]
  (let [{:keys [external-list-id external-gift-id]} (:path-params request)
        {:keys [ok]} (:params request)]
    (when ok
      (domain/delete-gift! external-gift-id))
    (response/redirect (str "/list/" external-list-id "/edit") :see-other)))

(defn render-reserve-gift-page [request]
  (let [{:keys [external-list-id external-gift-id]} (:path-params request)
        {:keys [name]} (domain/get-gift external-gift-id)]
    (page
     [:h1 "Reserveer " (h name)]
     (form/form-to
      [:post (str "/list/" external-list-id "/view/gift/" external-gift-id "/reserve")]
      (anti-forgery-field)
      [:label "Jouw naam"
       (form/text-field {:required true} :reserved-by)]
      [:a {:href (str "/list/" external-list-id "/view")} "Ga terug"]
      #_{:clj-kondo/ignore [:invalid-arity]}
      (form/submit-button {:name :ok} "Reserveer cadeau")))))

(defn reserve-gift [request]
  (let [{:keys [external-list-id external-gift-id]} (:path-params request)
        {:keys [ok reserved-by]} (:params request)]
    (when ok
      (domain/reserve-gift! external-gift-id reserved-by))
    (response/redirect (str "/list/" external-list-id "/view") :see-other)))

(defn render-cancel-gift-reservation-page [request]
  (let [{:keys [external-list-id external-gift-id]} (:path-params request)
        {:keys [name reserved-by]} (domain/get-gift external-gift-id)]
    (page
     [:h1 "Annuleer reservering van " (h name)]
     [:p
      "Dit cadeau is gereserveerd door " (h reserved-by) "."
      "Weet je zeker dat je deze reservering ongedaan wilt maken?"]
     (form/form-to
      [:post (str "/list/" external-list-id "/view/gift/" external-gift-id "/cancel-reservation")]
      (anti-forgery-field)
      [:a {:href (str "/list/" external-list-id "/view")} "Nee, ga terug"]
      #_{:clj-kondo/ignore [:invalid-arity]}
      (form/submit-button {:name :ok} "Ja, maak reservering ongedaan")))))

(defn cancel-gift-reservation [request]
  (let [{:keys [external-list-id external-gift-id]} (:path-params request)
        {:keys [ok]} (:params request)]
    (when ok
      (domain/cancel-gift-reservation! external-gift-id))
    (response/redirect (str "/list/" external-list-id "/view") :see-other)))

(defn render-domain-exception-page [exception]
  (let [{:keys [ui-message]} (ex-data exception)]
    (page
     [:h1 "Er ging iets mis!"]
     [:p ui-message])))

(def internal-server-error-page
  (page
   [:h1 "Onverwachte fout"]))

(def invalid-request-page
  (page
   [:h1 "Ongeldig verzoek"]))
