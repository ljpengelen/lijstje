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

(def link-extractor (-> (LinkExtractor/builder)
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
     (hp/include-css "/css/screen.css")]
    [:body content])})

(defn render-create-list-page [_]
  (page
   [:h1 "Maak een nieuw verlanglijstje"]
   (form/form-to
    [:post "/"]
    (anti-forgery-field)
    [:label "Naam"
     (form/text-field :name)]
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
     (for [{:keys [name description price]} gifts]
       [:p (h name) " " (escape-html-and-autolink description) " " (h price)])
     [:a {:href (str "/list/" external-list-id "/edit/gift")} "Voeg cadeau toe"]
     [:p "Deel met anderen: " (str (:host env)"/list/" public-external-id "/view")])))

(defn render-view-list-page [request]
  (let [{:keys [external-list-id]} (:path-params request)
        {:keys [name gifts]} (domain/get-list-by-public-id external-list-id)]
    (page
     [:h1 "Verlanglijstje " (h name)]
     (for [{:keys [name description price external-id]} gifts]
       (list
        [:p (h name) " " (escape-html-and-autolink description) " " (h price)]
        [:a {:href (str "/gift/" external-id "/reserve")} "Reserveer cadeau"])))))

(defn render-create-gift-page [request]
  (let [{:keys [external-list-id]} (:path-params request)]
    (page
     [:h1 "Voeg een cadeau toe"]
     (form/form-to
      [:post (str "/list/" external-list-id "/edit/gift")]
      (anti-forgery-field)
      [:label "Naam"
       (form/text-field :name)]
      [:label "Richtprijs"
       (form/text-field :price)]
      [:label "Omschrijving"
       (form/text-area :description)]
      (form/submit-button "Voeg cadeau toe")))))

(defn create-gift [request]
  (let [{:keys [external-list-id]} (:path-params request)
        {:keys [list-id]} (domain/get-list-by-private-id external-list-id)
        {:keys [name price description]} (:params request)]
    (domain/create-gift! list-id name price description)
    (response/redirect (str "/list/" external-list-id "/edit") :see-other)))

(defn render-reserve-gift-page [request]
  (let [{:keys [external-list-id external-gift-id]} (:path-params request)
        {:keys [name]} (domain/get-gift external-gift-id)]
    (page
     [:h1 "Reserveer " (h name)]
     (form/form-to
      [:post (str "/list/" external-list-id "/view/gift/" external-gift-id "/reserve")]
      (anti-forgery-field)
      [:label "Naam"
       (form/text-field :reserved-by)]
      (form/submit-button "Reserveer cadeau")))))

(defn reserve-gift [request]
  (let [{:keys [external-list-id external-gift-id]} (:path-params request)
        {:keys [reserved-by]} (:params request)]
    (domain/reserve-gift! external-gift-id reserved-by)
    (response/redirect (str "/list/" external-list-id "/view"))))

(def internal-server-error-page
  (page
   [:h1 "Onverwachte fout"]))

(def invalid-request-page
  (page
   [:h1 "Ongeldig verzoek"]))
