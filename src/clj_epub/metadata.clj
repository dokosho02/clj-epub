(ns clj-epub.metadata

  "EPUB metadata (Dublin Core + EPUB3 meta elements).

  This namespace defines the data model and XML generation for the
  <metadata> section of content.opf.

  Required fields per EPUB3 spec:
    - :title      (dc:title)
    - :language   (dc:language, BCP-47 tag, e.g. \"en\" \"zh\")
    - :identifier (dc:identifier, preferably a URN UUID)

  All other fields are optional but recommended."
  (:require [clojure.set :as set]))

;; ---------------------------------------------------------------------------
;; Defaults / helpers
;; ---------------------------------------------------------------------------

(defn gen-uuid []
  (str "urn:uuid:" (java.util.UUID/randomUUID)))

(defn now-iso []
  (.format ^java.time.format.DateTimeFormatter
   java.time.format.DateTimeFormatter/ISO_INSTANT
           (.truncatedTo (java.time.Instant/now)
                         java.time.temporal.ChronoUnit/SECONDS)))

;; ---------------------------------------------------------------------------
;; Schema / validation
;; ---------------------------------------------------------------------------

(def required-keys #{:title :language :identifier})

(defn validate!
  "Throws ex-info if required metadata keys are missing."
  [m]
  (let [missing (set/difference required-keys (set (keys m)))]
    (when (seq missing)
      (throw (ex-info "Missing required metadata fields"
                      {:missing missing
                       :provided (keys m)}))))
  m)

;; ---------------------------------------------------------------------------
;; Default metadata builder
;; ---------------------------------------------------------------------------

(defn with-defaults
  "Fill in sensible defaults for any missing optional fields.
   Always call this before passing metadata to the XML generators."
  [{:keys [identifier modified] :as m}]
  (cond-> m
    (nil? identifier) (assoc :identifier (gen-uuid))
    (nil? modified)   (assoc :modified   (now-iso))))

;; ---------------------------------------------------------------------------
;; XML element builders (hiccup-style vectors for clojure.data.xml)
;; ---------------------------------------------------------------------------

;; (defn- dc [tag text & attrs]
;;   (when (seq text)
;;     (into [:dc/tag {} text]
;;           attrs)))

;; (defn- meta-prop [prop value]
;;   (when value
;;     [:opf/meta {:property prop} value]))

;; (defn- meta-name [n value]
;;   (when value
;;     [:opf/meta {:name n :content value}]))

(defn build-metadata-elements
  "Returns a seq of XML element vectors for the <metadata> block."
  [{:keys [identifier title language
           creator creators
           publisher date description
           subject subjects
           rights source
           series series-position
           modified
           cover-id]
    :or   {cover-id "cover-image"}}]
  (let [all-creators (or creators (when creator [{:name creator}]))]
    (remove nil?
            (concat
             ;; -- Required DC fields --
             [[:dc/identifier {:id "BookId"} identifier]
              [:dc/title      {} title]
              [:dc/language   {} language]]

             ;; -- Creator(s) --
             (for [{:keys [name role file-as]
                    :or   {role "aut"}} all-creators]
               [:dc/creator {:opf/role    role
                             :opf/file-as (or file-as name)} name])

             ;; -- Optional DC fields --
             (when publisher [[:dc/publisher {} publisher]])
             (when date      [[:dc/date      {} date]])
             (when description [[:dc/description {} description]])

             ;; subjects (single kw or seq)
             (for [s (cond
                       (seq subjects) subjects
                       subject        [subject]
                       :else          [])]
               [:dc/subject {} s])

             (when rights [[:dc/rights {} rights]])
             (when source  [[:dc/source  {} source]])

             ;; -- EPUB3 required: modified --
             [[:opf/meta {:property "dcterms:modified"} (or modified (now-iso))]]

             ;; -- Cover image reference (legacy readers) --
             [[:opf/meta {:name "cover" :content cover-id}]]

             ;; -- Series --
             (when series
               [[:opf/meta {:property "belongs-to-collection" :id "series"} series]
                [:opf/meta {:refines "#series" :property "collection-type"} "series"]
                (when series-position
                  [:opf/meta {:refines "#series" :property "group-position"}
                   (str series-position)])])))))
