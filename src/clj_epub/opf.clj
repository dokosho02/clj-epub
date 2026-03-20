(ns clj-epub.opf
  "Generates content.opf — the EPUB Package Document.

  The OPF file is the single most important file in an EPUB.  It contains:

    <metadata>  - Dublin Core + EPUB3 meta elements
    <manifest>  - every resource file in the publication
    <spine>     - the linear reading order
    <guide>     - EPUB2 structural semantics (optional, for Sigil compat)

  ---------------------------------------------------------------------------
  manifest-item map shape:
    {:id         \"chapter001\"
     :href        \"Text/chapter001.xhtml\"
     :media-type  \"application/xhtml+xml\"
     :properties  \"nav\"}

  spine-item map shape:
    {:idref   \"chapter001\"
     :linear  true}
  ---------------------------------------------------------------------------"
  (:require [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; XML escaping
;; ---------------------------------------------------------------------------

(defn- xml-esc [s]
  (when s
    (-> (str s)
        (str/replace "&"  "&amp;")
        (str/replace "<"  "&lt;")
        (str/replace ">"  "&gt;")
        (str/replace "\"" "&quot;"))))

;; ---------------------------------------------------------------------------
;; Metadata
;; ---------------------------------------------------------------------------

(defn- dc-tag [tag text & kv-pairs]
  (when (seq text)
    (let [attrs (partition 2 kv-pairs)
          attr-str (str/join " " (map (fn [[k v]] (str k "=\"" (xml-esc v) "\"")) attrs))]
      (if (seq attr-str)
        (str "    <dc:" tag " " attr-str ">" (xml-esc text) "</dc:" tag ">")
        (str "    <dc:" tag ">" (xml-esc text) "</dc:" tag ">")))))

(defn- meta-tag [attrs & [content]]
  (let [attr-str (str/join " " (map (fn [[k v]] (str k "=\"" (xml-esc v) "\"")) attrs))]
    (if content
      (str "    <meta " attr-str ">" (xml-esc content) "</meta>")
      (str "    <meta " attr-str "/>"))))

(defn- build-metadata-str
  [{:keys [identifier title language
           creator creators
           publisher date description
           subject subjects
           rights source
           series series-position
           modified
           cover-id]
    :or   {cover-id "cover-image"}}]
  (let [all-creators (or creators (when creator [{:name creator}]))
        modified*    (or modified
                        ;; EPUB spec requires CCYY-MM-DDThh:mm:ssZ, no milliseconds
                         (.format ^java.time.format.DateTimeFormatter
                          java.time.format.DateTimeFormatter/ISO_INSTANT
                                  (.truncatedTo (java.time.Instant/now)
                                                java.time.temporal.ChronoUnit/SECONDS)))
        lines (remove nil?
                      (concat
                       ["  <metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n            xmlns:opf=\"http://www.idpf.org/2007/opf\">"]
                       [(dc-tag "identifier" identifier "id" "BookId")
                        (dc-tag "title"      title)
                        (dc-tag "language"   language)]
                 ;; EPUB3: role and file-as must be expressed as <meta refines>
                 ;; not as attributes on dc:creator (that was EPUB2 opf: attribute syntax)
                       (mapcat (fn [{:keys [name role file-as] :or {role "aut"}}]
                                 (let [creator-id (str "creator-" (hash name))]
                                   (remove nil?
                                           [(dc-tag "creator" name "id" creator-id)
                                            (meta-tag [["refines" (str "#" creator-id)]
                                                       ["property" "role"]
                                                       ["scheme" "marc:relators"]]
                                                      role)
                                            (when (or file-as name)
                                              (meta-tag [["refines" (str "#" creator-id)]
                                                         ["property" "file-as"]]
                                                        (or file-as name)))])))
                               all-creators)
                       [(dc-tag "publisher"   publisher)
                        (dc-tag "date"        date)
                        (dc-tag "description" description)]
                       (for [s (cond (seq subjects) subjects
                                     subject        [subject]
                                     :else          [])]
                         (dc-tag "subject" s))
                       [(dc-tag "rights" rights)
                        (dc-tag "source" source)
                        (meta-tag [["property" "dcterms:modified"]] modified*)
                        (meta-tag [["name" "cover"] ["content" cover-id]])]
                       (when series
                         (remove nil?
                                 [(meta-tag [["property" "belongs-to-collection"] ["id" "series"]] series)
                                  (meta-tag [["refines" "#series"] ["property" "collection-type"]] "series")
                                  (when series-position
                                    (meta-tag [["refines" "#series"] ["property" "group-position"]]
                                              (str series-position)))]))
                       ["  </metadata>"]))]
    (str/join "\n" lines)))

;; ---------------------------------------------------------------------------
;; Manifest
;; ---------------------------------------------------------------------------

(defn- manifest-item-str [{:keys [id href media-type properties]}]
  (str "    <item id=\""         (xml-esc id)
       "\" href=\""              (xml-esc href)
       "\" media-type=\""        (xml-esc media-type) "\""
       (when (seq properties)
         (str " properties=\""  (xml-esc properties) "\""))
       "/>"))

(defn- build-manifest-str [items]
  (str "  <manifest>\n"
       (str/join "\n" (map manifest-item-str items))
       "\n  </manifest>"))

;; ---------------------------------------------------------------------------
;; Spine
;; ---------------------------------------------------------------------------

(defn- spine-item-str [{:keys [idref linear] :or {linear true}}]
  (if linear
    (str "    <itemref idref=\"" (xml-esc idref) "\"/>")
    (str "    <itemref idref=\"" (xml-esc idref) "\" linear=\"no\"/>")))

(defn- build-spine-str [ncx-id items]
  (str "  <spine"
       (when (seq ncx-id) (str " toc=\"" (xml-esc ncx-id) "\""))
       ">\n"
       (str/join "\n" (map spine-item-str items))
       "\n  </spine>"))

;; ---------------------------------------------------------------------------
;; Guide (EPUB2 / Sigil compat)
;; ---------------------------------------------------------------------------

(defn- guide-ref-str [{:keys [type title href]}]
  (str "    <reference type=\""  (xml-esc type)
       "\" title=\""             (xml-esc title)
       "\" href=\""              (xml-esc href) "\"/>"))

(defn- build-guide-str [refs]
  (when (seq refs)
    (str "  <guide>\n"
         (str/join "\n" (map guide-ref-str refs))
         "\n  </guide>")))

;; ---------------------------------------------------------------------------
;; Public API
;; ---------------------------------------------------------------------------

(defn generate
  "Returns a UTF-8 byte array containing a valid content.opf.

  Options:
    :metadata   - metadata map
    :manifest   - seq of manifest-item maps
    :spine      - seq of spine-item maps {:idref \"...\" :linear true/false}
    :guide      - seq of guide-ref maps (optional)
    :ncx-id     - manifest id of the NCX file (default: \"ncx\")"
  [{:keys [metadata manifest spine guide ncx-id]
    :or   {ncx-id "ncx"}}]
  (let [lang     (or (:language metadata) "en")
        sections (remove nil?
                         [(build-metadata-str metadata)
                          (build-manifest-str manifest)
                          (build-spine-str ncx-id spine)
                          (build-guide-str guide)])
        xml (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                 "<package version=\"3.0\"\n"
                 "         xmlns=\"http://www.idpf.org/2007/opf\"\n"
                 "         unique-identifier=\"BookId\"\n"
                 "         xml:lang=\"" (xml-esc lang) "\">\n"
                 (str/join "\n" sections)
                 "\n</package>\n")]
    (.getBytes xml "UTF-8")))

;; ---------------------------------------------------------------------------
;; Media type helpers
;; ---------------------------------------------------------------------------

(def standard-media-types
  {"xhtml" "application/xhtml+xml"
   "html"  "application/xhtml+xml"
   "css"   "text/css"
   "jpg"   "image/jpeg"
   "jpeg"  "image/jpeg"
   "png"   "image/png"
   "gif"   "image/gif"
   "svg"   "image/svg+xml"
   "ttf"   "font/ttf"
   "otf"   "font/otf"
   "woff"  "font/woff"
   "woff2" "font/woff2"
   "mp3"   "audio/mpeg"
   "mp4"   "video/mp4"
   "js"    "application/javascript"
   "ncx"   "application/x-dtbncx+xml"
   "opf"   "application/oebps-package+xml"})

(defn media-type-for
  "Infer media-type from a filename/path. Returns nil if unknown."
  [filename]
  (when-let [ext (last (str/split filename #"\."))]
    (get standard-media-types (str/lower-case ext))))
