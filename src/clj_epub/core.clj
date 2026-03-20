(ns clj-epub.core
  "clj-epub — Clojure EPUB3 generation library.

  ┌─────────────────────────────────────────────────────────────────────────┐
  │  Primary entry point: `make-epub`                                       │
  │  Convenience entry point: `make-epub!` (write to file)                  │
  │  In-memory entry point: `make-epub-bytes`                               │
  └─────────────────────────────────────────────────────────────────────────┘

  Quick start:

    (require '[clj-epub.core :as epub])

    (epub/make-epub!
      {:metadata {:title    \"My Book\"
                  :author   \"Jane Smith\"
                  :language \"en\"}
       :chapters [{:id    \"ch01\"
                   :title \"Chapter 1\"
                   :href  \"Text/ch01.xhtml\"
                   :content \"<h1>Chapter 1</h1><p>Hello, EPUB!</p>\"}]}
      \"mybook.epub\")

  ─────────────────────────────────────────────────────────────────────────
  Full option reference:

  :metadata    map — book metadata
    :title        string  REQUIRED — book title
    :language     string  REQUIRED — BCP-47 language tag (e.g. \"en\", \"zh\")
    :identifier   string  — URN/UUID. Auto-generated if nil
    :author       string  — single author name
    :creators     seq     — [{:name \"...\" :role \"aut\" :file-as \"...\"}]
    :publisher    string
    :date         string  — ISO-8601 date string (e.g. \"2024-01-15\")
    :description  string
    :subject      string  — single subject tag
    :subjects     seq     — multiple subject tags
    :rights       string
    :source       string
    :series       string  — series title
    :series-position  int/string
    :modified     string  — ISO-8601 datetime (auto-generated if nil)

  :chapters    seq of chapter maps
    :id       string  REQUIRED — unique manifest id (no spaces)
    :title    string  REQUIRED — display title (used in TOC)
    :href     string  REQUIRED — path relative to OEBPS root (e.g. \"Text/ch01.xhtml\")
    :content  string  — raw XHTML body content (inner HTML of the chapter)
                        mutually exclusive with :data
    :data     bytes   — pre-rendered byte array (use if you've already built the XHTML)
    :linear   boolean — whether to include in spine linear order (default: true)
    :toc      seq     — nested sub-entries [{:title :href :toc}]

  :cover       map or nil
    :image-href  string  — path to cover image inside OEBPS (e.g. \"Images/cover.jpg\")
    :image-data  bytes   — raw image bytes (required if image not in :resources)
    :image-id    string  — manifest id for image (default: \"cover-image\")

  :css         string or nil — stylesheet content. Uses built-in default if nil.
               Pass false to include no stylesheet at all.

  :resources   seq of resource maps — additional files (fonts, images, etc.)
    :id          string  REQUIRED
    :href        string  REQUIRED — path relative to OEBPS root
    :data        bytes   REQUIRED — file content
    :media-type  string  — inferred from extension if omitted
    :properties  string  — EPUB3 manifest properties (e.g. \"cover-image\")

  :options     map of advanced options
    :opf-path     string  — path of OPF inside ZIP (default: \"OEBPS/content.opf\")
    :nav-href     string  — href of nav doc relative to OEBPS (default: \"nav.xhtml\")
    :ncx-href     string  — href of NCX doc relative to OEBPS (default: \"toc.ncx\")
    :css-href     string  — href of CSS relative to OEBPS (default: \"Styles/stylesheet.css\")
    :epub2-compat boolean — include toc.ncx (default: true)
    :guide        seq     — override guide refs (auto-built if not provided)
  ─────────────────────────────────────────────────────────────────────────"
  (:require [clj-epub.container :as container]
            [clj-epub.ncx       :as ncx]
            [clj-epub.nav       :as nav]
            [clj-epub.opf       :as opf]
            [clj-epub.xhtml     :as xhtml]
            [clj-epub.zip       :as zip]
            [clojure.string     :as str])
  (:import [java.util UUID]))

;; ---------------------------------------------------------------------------
;; Internal helpers
;; ---------------------------------------------------------------------------

(defn- gen-uuid []
  (str "urn:uuid:" (UUID/randomUUID)))

(defn- now-iso []
  (.format ^java.time.format.DateTimeFormatter
   java.time.format.DateTimeFormatter/ISO_INSTANT
           (.truncatedTo (java.time.Instant/now)
                         java.time.temporal.ChronoUnit/SECONDS)))

(defn- text->bytes ^bytes [^String s]
  (.getBytes s "UTF-8"))

(defn- ->oebps
  "Prepend OEBPS/ prefix to a relative path."
  [path]
  (str "OEBPS/" path))

(defn- css-href-from-chapter
  "Compute relative path from a chapter file in Text/ to the CSS file."
  [chapter-href css-href]
  ;; Chapters are typically in Text/, so CSS at Styles/x.css is at ../Styles/x.css
  (let [depth (dec (count (str/split chapter-href #"/")))]
    (str (str/join (repeat depth "../")) css-href)))

(defn- infer-media-type [href]
  (or (opf/media-type-for href) "application/octet-stream"))

;; ---------------------------------------------------------------------------
;; Build pipeline
;; ---------------------------------------------------------------------------

(defn- build-metadata [metadata]
  (-> metadata
      (update :identifier #(or % (gen-uuid)))
      (update :modified   #(or % (now-iso)))))

(defn- build-chapters
  "Render chapter content to bytes, returning augmented chapter maps."
  [chapters css-href lang]
  (mapv (fn [{:keys [content data href title] :as ch}]
          (let [rel-css (when css-href (css-href-from-chapter href css-href))
                bytes   (or data
                            (when content
                              (xhtml/wrap-chapter-bytes
                               content
                               {:title    title
                                :css-href rel-css
                                :lang     lang})))]
            (assoc ch :rendered-data bytes)))
        chapters))

(defn- build-manifest-items
  [{:keys [chapters cover css-href nav-href ncx-href resources
           epub2-compat cover-image-id]
    :or   {epub2-compat true cover-image-id "cover-image"}}]
  (concat
   ;; Nav document (EPUB3 required)
   [{:id "nav" :href nav-href :media-type "application/xhtml+xml" :properties "nav"}]

   ;; NCX (EPUB2 compat)
   (when epub2-compat
     [{:id "ncx" :href ncx-href :media-type "application/x-dtbncx+xml"}])

   ;; CSS
   (when css-href
     [{:id "stylesheet" :href css-href :media-type "text/css"}])

   ;; Cover image
   (when-let [img-href (-> cover :image-href)]
     [{:id         cover-image-id
       :href        img-href
       :media-type  (infer-media-type img-href)
       :properties  "cover-image"}])

   ;; Chapters
   (map (fn [{:keys [id href]}]
          {:id id :href href :media-type "application/xhtml+xml"})
        chapters)

   ;; Extra resources
   (map (fn [{:keys [id href media-type properties]}]
          (cond-> {:id id :href href
                   :media-type (or media-type (infer-media-type href))}
            properties (assoc :properties properties)))
        resources)))

;; (defn- build-spine [chapters cover nav-href]
(defn- build-spine [chapters]
  (concat
   ;; All chapters in order: linear=false items first (e.g. cover), then linear=true
   ;; nav.xhtml is NOT in spine — epubcheck OPF-096 requires non-linear items
   ;; to have a hyperlink pointing to them, which we cannot guarantee for nav.
   ;; Sigil and most validators accept nav outside spine when it is in the manifest
   ;; with properties="nav".
   (->> chapters
        (filter #(false? (:linear %)))
        (map #(hash-map :idref (:id %) :linear false)))
   (->> chapters
        (remove #(false? (:linear %)))
        (map #(hash-map :idref (:id %) :linear true)))))

;; (defn- build-guide [chapters cover nav-href]
(defn- build-guide [chapters nav-href]
  (let [;; Cover: first linear=false chapter, OR any chapter with id="cover"
        ;; Does not require user to name it "cover" specifically
        cover-ch   (or (first (filter #(= (:id %) "cover") chapters))
                       (first (filter #(false? (:linear %)) chapters)))
        ;; Title page: id="titlepage", or second non-linear chapter if cover already found
        title-ch   (first (filter #(= (:id %) "titlepage") chapters))
        first-body (first (filter #(not (false? (:linear %))) chapters))]
    (remove nil?
            [(when cover-ch   {:type "cover"      :title "Cover"      :href (:href cover-ch)})
             (when title-ch   {:type "title-page" :title "Title Page" :href (:href title-ch)})
             {:type "toc"     :title "Table of Contents" :href nav-href}
             (when first-body {:type "text"       :title "Begin Reading" :href (:href first-body)})])))

(defn- build-nav-document
  [{:keys [chapters css-href lang title landmarks]}]
  (let [;; Strip fragment identifiers from sub-toc hrefs.
        ;; User may pass {:href "Text/ch01.xhtml#s1"} but if the rendered XHTML
        ;; does not contain id="s1", epubcheck RSC-012 fires.
        ;; Safest default: remove fragments from auto-generated TOC entries.
        ;; Users who need fragments should supply pre-rendered :data chapters
        ;; with the matching id attributes already present in the HTML.
        strip-fragments (fn strip-fragments [entries]
                          (mapv (fn [e]
                                  (-> e
                                      ;; Guard: href may be nil for entries without explicit href
                                      (update :href #(when % (first (clojure.string/split % #"#"))))
                                      (update :children #(when (seq %) (strip-fragments %)))))
                                entries))
        toc-entries (strip-fragments
                     (nav/chapters->toc-entries
                      ;; Skip non-linear cover page from TOC
                      (remove #(false? (:linear %)) chapters)))
        ;; Cover: first linear=false chapter (no id naming convention required)
        cover-ch    (or (first (filter #(= (:id %) "cover") chapters))
                        (first (filter #(false? (:linear %)) chapters)))
        ;; body-ch     (first (filter #(not (false? (:linear %))) chapters))
        lmarks      (or landmarks
                        (nav/default-landmarks
                         {:cover-href  (some-> cover-ch :href)
                          ;; toc-href intentionally nil: nav.xhtml is not in spine,
                          ;; pointing landmarks toc at it triggers epubcheck RSC-011
                          :toc-href    nil
                          ;; :body-href   (some-> body-ch :href)
                          }))
        ;; nav.xhtml is at OEBPS root, so CSS path is relative from there
        nav-css     (when css-href css-href)]
    (nav/generate {:title     (or title "Contents")
                   :css-href  nav-css
                   :lang      (or lang "en")
                   :toc       toc-entries
                   :landmarks lmarks})))

(defn- build-ncx-document
  [{:keys [metadata chapters epub2-compat]}]
  (when epub2-compat
    (ncx/generate {:uid    (:identifier metadata)
                   :title  (:title metadata)
                   :author (or (-> metadata :creators first :name)
                               (:author metadata))
                   ;; Strip fragments from NCX nav-points for same reason as nav.xhtml:
                   ;; epubcheck RSC-012 fires if fragment target id is absent in XHTML
                   :points (let [strip (fn strip [pts]
                                         ;; Strip fragments, then deduplicate by :src.
                                         ;; Without dedup, a chapter and its sub-toc entry both resolve
                                         ;; to the same file after stripping, causing epubcheck RSC-005
                                         ;; (different playOrder values for same target).
                                         (->> pts
                                              (mapv #(-> %
                                                         ;; NCX nav-points use :src (not :href)
                                                         ;; Guard: src may be nil
                                                         (update :src (fn [s] (when s (first (clojure.string/split s #"#")))))
                                                         (assoc :children nil)))
                                              (reduce (fn [acc pt]
                                                        (if (some #(= (:src %) (:src pt)) acc)
                                                          acc
                                                          (conj acc pt)))
                                                      [])))]
                             (strip (ncx/chapters->nav-points
                                     (remove #(false? (:linear %)) chapters))))
                   :depth  2})))

;; ---------------------------------------------------------------------------
;; Public: make-epub
;; ---------------------------------------------------------------------------

(defn make-epub
  "Build an EPUB3 publication and return a seq of ZIP entry maps.

  Each map has :path (string) and :data (byte-array).
  This is the lowest-level function; prefer `make-epub-bytes` or `make-epub!`.

  See namespace docstring for full option reference."
  [{:keys [metadata chapters cover css resources options]
    :or   {chapters  []
           resources []
           options   {}}}]
  {:pre [(map? metadata) (string? (:title metadata)) (string? (:language metadata))]}

  (let [{:keys [opf-path nav-href ncx-href epub2-compat guide]
         :or   {opf-path     "OEBPS/content.opf"
                nav-href     "nav.xhtml"
                ncx-href     "toc.ncx"
                epub2-compat true}}  options

        ;; Resolve css-href (auto if not explicitly false/nil)
        effective-css-href (cond
                             (false? css)      nil
                             (string? options) options
                             :else             (or (:css-href options) "Styles/stylesheet.css"))

        metadata*  (build-metadata metadata)
        lang       (:language metadata*)
        chapters*  (build-chapters chapters effective-css-href lang)

        manifest   (build-manifest-items
                    {:chapters        chapters*
                     :cover           cover
                     :css-href        effective-css-href
                     :nav-href        nav-href
                     :ncx-href        ncx-href
                     :resources       resources
                     :epub2-compat    epub2-compat})

        spine      (build-spine chapters*)

        guide*     (or guide (build-guide chapters* nav-href))

        opf-bytes  (opf/generate
                    {:metadata metadata*
                     :manifest manifest
                     :spine    spine
                     :guide    guide*
                     :ncx-id   (when epub2-compat "ncx")})

        nav-bytes  (build-nav-document
                    {:chapters  chapters*
                     :css-href  effective-css-href
                     :nav-href  nav-href
                     :lang      lang
                     :title     (:title metadata*)})

        ncx-bytes  (build-ncx-document
                    {:metadata     metadata*
                     :chapters     chapters*
                     :epub2-compat epub2-compat})

        css-bytes  (cond
                     (false? css)    nil
                     (string? css)   (text->bytes css)
                     :else           (text->bytes xhtml/default-stylesheet))

        ;; Assemble all ZIP entries in order
        entries    (remove nil?
                           (concat
                            ;; META-INF
                            [{:path "META-INF/container.xml"
                              :data (container/generate {:opf-path opf-path})}]
                            ;; OPF
                            [{:path opf-path
                              :data opf-bytes}]
                            ;; Nav
                            [{:path (->oebps nav-href)
                              :data nav-bytes}]
                            ;; NCX
                            (when (and epub2-compat ncx-bytes)
                              [{:path (->oebps ncx-href) :data ncx-bytes}])
                            ;; CSS
                            (when (and effective-css-href css-bytes)
                              [{:path (->oebps effective-css-href) :data css-bytes}])
                            ;; Cover image
                            (when-let [img (:image-data cover)]
                              [{:path (->oebps (-> cover :image-href))
                                :data img}])
                            ;; Chapters
                            (for [{:keys [href rendered-data]} chapters*
                                  :when rendered-data]
                              {:path (->oebps href) :data rendered-data})
                            ;; Extra resources
                            (for [{:keys [href data]} resources
                                  :when data]
                              {:path (->oebps href) :data data})))]
    entries))

(defn make-epub-bytes
  "Build an EPUB and return its raw bytes.

  See namespace docstring for full option reference."
  ^bytes [opts]
  (zip/epub->bytes (make-epub opts)))

(defn make-epub!
  "Build an EPUB and write it to `output-path` (a file path string or File).

  Returns the output path.

  See namespace docstring for full option reference."
  [opts output-path]
  (zip/write-epub! output-path (make-epub opts))
  output-path)
