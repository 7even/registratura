(ns registratura.http
  (:require [bidi.ring :refer [make-handler]]
            [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [registratura.db :as db]
            [registratura.html :as html]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [bad-request content-type not-found response]]
            [tick.core :as t]
            [time-literals.read-write]))

(time-literals.read-write/print-time-literals-clj!)

(defn- created [body]
  {:status 201
   :headers {}
   :body body})

(defn- unprocessable-entity [body]
  {:status 422
   :headers {}
   :body body})

(s/def :patient/id
  int?)

(s/def :patient/first-name
  string?)

(s/def :patient/middle-name
  (s/nilable string?))

(s/def :patient/last-name
  string?)

(s/def :patient/gender
  #{:gender/male :gender/female})

(s/def :patient/birthday
  t/date?)

(s/def :patient/address
  string?)

(s/def :patient/insurance-number
  string?)

(s/def :patient/query
  string?)

(s/def :patient/genders
  (s/coll-of :patient/gender))

(s/def :patient/min-age
  (s/and int? (complement neg?)))

(s/def :patient/max-age
  (s/and int? (complement neg?)))

(s/def :pagination/limit
  (s/and int? pos? #(<= % 100)))

(s/def :pagination/offset
  (s/and int? (complement neg?)))

(def patient-attr-names
  [:patient/first-name
   :patient/middle-name
   :patient/last-name
   :patient/gender
   :patient/birthday
   :patient/address
   :patient/insurance-number])

(s/def ::list-patients
  (s/keys :opt [:patient/query
                :patient/genders
                :patient/min-age
                :patient/max-age
                :pagination/limit
                :pagination/offset]))

(s/def ::create-patient
  (s/keys :req [:patient/first-name
                :patient/middle-name
                :patient/last-name
                :patient/gender
                :patient/birthday
                :patient/address
                :patient/insurance-number]))

(s/def ::update-patient
  (s/keys :opt [:patient/first-name
                :patient/middle-name
                :patient/last-name
                :patient/gender
                :patient/birthday
                :patient/address
                :patient/insurance-number]))

(defn- wrap-in-vector [v]
  (if (vector? v)
    v
    (vector v)))

(defn- update-if-exists [m k f]
  (if (contains? m k)
    (update m k f)
    m))

(defn process-patient-list-params [params]
  (-> params
      (update-keys keyword)
      (update-if-exists :patient/genders (comp (partial mapv keyword)
                                               wrap-in-vector))
      (update-if-exists :patient/min-age parse-long)
      (update-if-exists :patient/max-age parse-long)
      (update-if-exists :pagination/limit parse-long)
      (update-if-exists :pagination/offset parse-long)))

(defn- list-patients [db-conn {:keys [params]}]
  (let [processed-params (process-patient-list-params params)]
    (if (s/valid? ::list-patients processed-params)
      (response (db/list-patients db-conn processed-params))
      (bad-request nil))))

(defn- get-patient [db-conn {:keys [params]}]
  (let [id (-> params :id parse-long)]
    (if-let [patient (db/get-patient db-conn id)]
      (response patient)
      (not-found nil))))

(defn- create-patient [db-conn {:keys [form-params]}]
  (if (s/valid? ::create-patient form-params)
    (let [attrs (select-keys form-params patient-attr-names)
          new-patient-id (db/create-patient db-conn attrs)]
      (created {:patient/id new-patient-id}))
    (unprocessable-entity (s/explain-data ::create-patient form-params))))

(defn- update-patient [db-conn {:keys [params form-params]}]
  (if (s/valid? ::update-patient form-params)
    (let [id (-> params :id parse-long)
          new-attrs (select-keys form-params patient-attr-names)]
      (when (seq new-attrs)
        (db/update-patient db-conn id new-attrs))
      (response nil))
    (unprocessable-entity (s/explain-data ::update-patient form-params))))

(defn- delete-patient [db-conn {:keys [params]}]
  (let [id (-> params :id parse-long)]
    (db/delete-patient db-conn id)
    (response nil)))

(defn- make-routes [db-conn]
  [""
   {"/api" {"/patients" {"" {:get (partial list-patients db-conn)
                             :post (partial create-patient db-conn)}
                         ["/" [#"\d+" :id]] {:get (partial get-patient db-conn)
                                             :patch (partial update-patient db-conn)
                                             :delete (partial delete-patient db-conn)}}}}])

(defn- wrap-edn-request [handler]
  (fn [request]
    (try
      (let [form-params (let [request-body (some-> request :body slurp)]
                          (when (and (= (get-in request [:headers "content-type"])
                                        "application/edn")
                                     (not (str/blank? request-body)))
                            (edn/read-string {:readers time-literals.read-write/tags}
                                             request-body)))]
        (handler (cond-> request
                   (some? form-params)
                   (assoc :form-params form-params))))
      (catch java.time.format.DateTimeParseException _
        {:status 400
         :headers {}
         :body nil}))))

(defn- wrap-edn-response [handler]
  (fn [request]
    (when-let [handler-response (handler request)]
      (cond-> handler-response
        (some? (:body handler-response)) (update :body pr-str)
        (some? (:body handler-response)) (content-type "application/edn")))))

(defn- wrap-html-page-response [handler]
  (fn [request]
    (or (handler request)
        (-> (response html/page)
            (content-type "text/html")))))

(defn handler [db-conn]
  (let [routes (make-routes db-conn)]
    (-> (make-handler routes)
        wrap-edn-request
        wrap-edn-response
        wrap-params
        (wrap-resource "public")
        wrap-html-page-response)))

(defn start [{:keys [db-conn server-opts]}]
  (run-jetty (handler db-conn) server-opts))

(defn stop [server]
  (.stop server))
