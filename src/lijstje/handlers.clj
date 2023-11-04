(ns lijstje.handlers 
  (:require [clojure.string :as string]
            [hiccup.core :refer [h]]
            [hiccup.form :as form]
            [hiccup.page :as hp]
            [lijstje.domain :as domain]
            [lijstje.logging :as logging]
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
        (str "<a target=\"_blank\" rel=\"noreferrer\" href=\"" (with-protocol text) "\">" text "</a>")
        (h text)))))

(defmacro compiled-at [] (System/currentTimeMillis))

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
     (hp/include-css (str "/css/screen.css?version=" (compiled-at)))]
    [:body content])})

(defn confirmation-button [text]
  #_{:clj-kondo/ignore [:invalid-arity]}
  (form/submit-button {:class "button" :name :ok} text))

(defn render-create-list-page [_ _]
  (page
   [:h1 "Maak een nieuw verlanglijstje"]
   (form/form-to
    [:post "/"]
    (anti-forgery-field)
    [:label "Naam van je lijstje"
     (form/text-field {:maxlength 100 :required true :autocomplete "off"} :name)]
    (confirmation-button "Maak nieuwe lijst"))))

(defn create-list [{:keys [datasource]} request]
  (let [name (-> request :params :name)
        {:keys [private-external-id]} (domain/create-list! datasource name)]
    (response/redirect (str "/list/" private-external-id "/edit") :see-other)))

(defn primary-button-link [url text]
  [:a {:class "button" :href url} text])

(defn secondary-button-link [url text]
  [:a {:class "secondary-button" :href url} text])

(defn render-edit-list-page [{:keys [datasource host]} request]
  (let [{:keys [external-list-id]} (:path-params request)
        {:keys [name gifts public-external-id]}
        (domain/get-list-by-private-id datasource external-list-id)]
    (page
     [:h1 "Bewerk verlanglijstje " (h name)]
     [:div {:class "edit-list"}
      [:div {:class "edit-list-urls"}
       [:p
        "Bewaar deze pagina goed. "
        "Het is de enige manier om later je lijstje aan te passen of te "
        [:a {:href (str "/list/" external-list-id "/delete")} "verwijderen"] "."]
       [:p "Deel de volgende URL met anderen. Zij kunnen dan je lijstje bekijken en cadeau's reserveren."]
       [:code (str host "/list/" public-external-id "/view")]]
      [:div {:class "edit-list-gifts"}
       (when (empty? gifts)
         [:p {:class "no-gifts"} "Dit verlanglijstje bevat nog geen cadeau's."])
       (for [{:keys [external-id name description price]} gifts]
         [:div {:class "gift"}
          [:div {:class "gift-name-and-price"}
           [:div {:class "gift-name"} (h name)]
           [:div {:class "gift-price"} (h price)]]
          [:div {:class "gift-description"} (escape-html-and-autolink description)]
          [:div {:class "horizontal-buttons"}
           (primary-button-link (str "/list/" external-list-id "/edit/gift/" external-id "/edit") "Bewerk cadeau")
           (primary-button-link (str "/list/" external-list-id "/edit/gift/" external-id "/delete") "Verwijder cadeau")]])
       (primary-button-link (str "/list/" external-list-id "/edit/gift") "Voeg cadeau toe")]])))

(defn cancellation-button [url text]
  (secondary-button-link url text))

(defn render-delete-list-page [{:keys [datasource]} request]
  (let [{:keys [external-list-id]} (:path-params request)
        {:keys [name]} (domain/get-list-by-private-id datasource external-list-id)]
    (page
     [:h1 "Verwijder " (h name)]
     [:p
      "Weet je zeker dat je het verlanglijstje \"" (h name) "\" wilt verwijderen? "
      "Dit kan later niet ongedaan gemaakt worden."]
     (form/form-to
      [:post (str "/list/" external-list-id "/delete")]
      (anti-forgery-field)
      [:div {:class "horizontal-buttons"}
       (cancellation-button (str "/list/" external-list-id "/edit") "Nee, ga terug")
       (confirmation-button "Ja, verwijder lijst")]))))

(defn delete-list [{:keys [datasource]} request]
  (let [{:keys [external-list-id]} (:path-params request)
        {:keys [ok]} (:params request)]
    (when ok
      (domain/delete-list! datasource external-list-id))
    (response/redirect "/" :see-other)))

(defn render-view-list-page [{:keys [datasource]} request]
  (let [{:keys [external-list-id]} (:path-params request)
        {:keys [name gifts]} (domain/get-list-by-public-id datasource external-list-id)]
    (page
     [:div {:class "header"}
      [:h1 "Ver&shy;lang&shy;lijst&shy;je " (h name)]
      [:a {:class "menu-button" :href "/"} "Maak nieuw lijstje"]]
     [:div {:class "gifts"}
      (for [{:keys [name description price external-id reserved-by]} (sort-by :reserved-at gifts)]
        [:div
         {:class (str "gift " (when reserved-by "reserved"))}
         [:div {:class "gift-name-and-price"}
          [:div {:class "gift-name"} (h name)]
          [:div {:class "gift-price"} (h price)]]
         [:div {:class "gift-description"} (escape-html-and-autolink description)]
         (if reserved-by
           [:a {:class "button" :href (str "/list/" external-list-id "/view/gift/" external-id "/cancel-reservation")} "Maak reservering ongedaan"]
           [:a {:class "button" :href (str "/list/" external-list-id "/view/gift/" external-id "/reserve")} "Reserveer cadeau"])])])))

(defn render-create-gift-page [_ request]
  (let [{:keys [external-list-id]} (:path-params request)]
    (page
     [:h1 "Voeg een cadeau toe"]
     [:p
      "In de omschrijving van het cadeau kun je ook URLs gebruiken. "
      "Degenen met wie je je lijstje deelt, krijgen dan klikbare links te zien."]
     (form/form-to
      [:post (str "/list/" external-list-id "/edit/gift")]
      (anti-forgery-field)
      [:label "Naam"
       (form/text-field {:maxlength 100 :required true :autocomplete "off"} :name)]
      [:label "Richtprijs"
       (form/text-field {:maxlength 100 :required true :autocomplete "off"} :price)]
      [:label "Omschrijving"
       (form/text-area {:maxlength 2000 :required true :rows 10 :autocomplete "off"} :description)]
      [:div {:class "horizontal-buttons"}
       (cancellation-button (str "/list/" external-list-id "/edit") "Ga terug")
       (confirmation-button "Voeg cadeau toe")]))))

(defn create-gift [{:keys [datasource]} request]
  (let [{:keys [external-list-id]} (:path-params request)
        {:keys [name ok price description]} (:params request)]
    (when ok
      (domain/create-gift! datasource external-list-id name price description))
    (response/redirect (str "/list/" external-list-id "/edit") :see-other)))

(defn render-edit-gift-page [{:keys [datasource]} request]
  (let [{:keys [external-list-id external-gift-id]} (:path-params request)
        {:keys [name price description]} (domain/get-gift datasource external-gift-id)]
    (page
     [:h1 "Bewerk " (h name)]
     [:p
      "In de omschrijving van het cadeau kun je ook URLs gebruiken. "
      "Degenen met wie je je lijstje deelt, krijgen dan klikbare links te zien."]
     (form/form-to
      [:post (str "/list/" external-list-id "/edit/gift/" external-gift-id "/edit")]
      (anti-forgery-field)
      [:label "Naam"
       (form/text-field {:maxlength 100 :required true :value name :autocomplete "off"} :name)]
      [:label "Richtprijs"
       (form/text-field {:maxlength 100 :required true :value price :autocomplete "off"} :price)]
      [:label "Omschrijving"
       #_{:clj-kondo/ignore [:invalid-arity]}
       (form/text-area {:maxlength 2000 :required true :rows 10 :autocomplete "off"} :description description)]
      [:div {:class "horizontal-buttons"}
       (cancellation-button (str "/list/" external-list-id "/edit") "Ga terug")
       (confirmation-button "Sla wijzigingen op")]))))

(defn update-gift [{:keys [datasource]} request]
  (let [{:keys [external-list-id external-gift-id]} (:path-params request)
        {:keys [name ok price description]} (:params request)]
    (when ok
      (domain/update-gift! datasource external-gift-id name price description))
    (response/redirect (str "/list/" external-list-id "/edit") :see-other)))

(defn render-delete-gift-page [{:keys [datasource]} request]
  (let [{:keys [external-list-id external-gift-id]} (:path-params request)
        {:keys [name]} (domain/get-gift datasource external-gift-id)]
    (page
     [:h1 "Verwijder " (h name)]
     [:p "Weet je zeker dat je het cadeau \"" (h name) "\" wilt verwijderen?"]
     (form/form-to
      [:post (str "/list/" external-list-id "/edit/gift/" external-gift-id "/delete")]
      (anti-forgery-field)
      [:div {:class "horizontal-buttons"}
       (cancellation-button (str "/list/" external-list-id "/edit") "Nee, ga terug")
       (confirmation-button "Ja, verwijder cadeau")]))))

(defn delete-gift [{:keys [datasource]} request]
  (let [{:keys [external-list-id external-gift-id]} (:path-params request)
        {:keys [ok]} (:params request)]
    (when ok
      (domain/delete-gift! datasource external-gift-id))
    (response/redirect (str "/list/" external-list-id "/edit") :see-other)))

(defn render-reserve-gift-page [{:keys [datasource]} request]
  (let [{:keys [external-list-id external-gift-id]} (:path-params request)
        {:keys [name]} (domain/get-gift datasource external-gift-id)]
    (page
     [:h1 "Reserveer " (h name)]
     [:p
      "Je kunt cadeau's reserveren als je van plan bent om ze te kopen. "
      "Als dat uiteindelijk niet lukt, dan kun je de reservering weer ongedaan maken. "]
     [:p
      "Geef een naam op bij het reserveren, dan weet je later zeker dat jij degene bent "
      "die een cadeau gereserveerd had."]
     (form/form-to
      [:post (str "/list/" external-list-id "/view/gift/" external-gift-id "/reserve")]
      (anti-forgery-field)
      [:label "Jouw naam"
       (form/text-field {:maxlength 100 :required true :autocomplete "name"} :reserved-by)]
      [:div {:class "horizontal-buttons"}
       [:a {:class "secondary-button" :href (str "/list/" external-list-id "/view")} "Ga terug"]
       #_ {:clj-kondo/ignore [:invalid-arity]}
       (form/submit-button {:class "button" :name :ok} "Reserveer cadeau")]))))

(defn reserve-gift [{:keys [datasource]} request]
  (let [{:keys [external-list-id external-gift-id]} (:path-params request)
        {:keys [ok reserved-by]} (:params request)]
    (when ok
      (domain/reserve-gift! datasource external-gift-id reserved-by))
    (response/redirect (str "/list/" external-list-id "/view") :see-other)))

(defn render-cancel-gift-reservation-page [{:keys [datasource]} request]
  (let [{:keys [external-list-id external-gift-id]} (:path-params request)
        {:keys [name reserved-by]} (domain/get-gift datasource external-gift-id)]
    (page
     [:h1 "Annuleer reservering van " (h name)]
     [:p
      "Dit cadeau is gereserveerd door " (h reserved-by) ". "
      "Weet je zeker dat je deze reservering ongedaan wilt maken?"]
     (form/form-to
      [:post (str "/list/" external-list-id "/view/gift/" external-gift-id "/cancel-reservation")]
      (anti-forgery-field)
      [:div {:class "horizontal-buttons"}
       [:a {:class "secondary-button" :href (str "/list/" external-list-id "/view")} "Nee, ga terug"]
       #_ {:clj-kondo/ignore [:invalid-arity]}
       (form/submit-button {:class "button" :name :ok} "Ja, maak reservering ongedaan")]))))

(defn cancel-gift-reservation [{:keys [datasource]} request]
  (let [{:keys [external-list-id external-gift-id]} (:path-params request)
        {:keys [ok]} (:params request)]
    (when ok
      (domain/cancel-gift-reservation! datasource external-gift-id))
    (response/redirect (str "/list/" external-list-id "/view") :see-other)))

(defn render-domain-exception-page [exception]
  (let [{:keys [ui-message]} (ex-data exception)]
    (page
     [:h1 "Er ging iets mis!"]
     [:p ui-message])))

(def internal-server-error-page
  (page
   [:h1 "Onverwachte fout"]
   [:p
    "Er is een onverwachte fout opgetreden. "
    "Wij zijn direct op de hoogte gebracht van deze fout "
    "en gaan op zoek naar een oplossing. "
    "Excuses voor het ongemak!"]))

(defn handle-exception [exception logger]
  (if (and
       (instance? clojure.lang.ExceptionInfo exception)
       (:ui-message (ex-data exception)))
    (render-domain-exception-page exception)
    (do
      (logging/log-error! logger "Unexpected exception" exception)
      internal-server-error-page)))

(def invalid-request-page
  (page
   [:h1 "Ongeldig verzoek"]))
