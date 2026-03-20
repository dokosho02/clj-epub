(ns clj-epub.nav
  "Generates the EPUB3 Navigation Document (nav.xhtml).

  This file serves as the primary table-of-contents mechanism in EPUB3.
  It MUST be included in the OPF manifest with the 'nav' property.

  Three navigation types are supported:
    toc       - main table of contents   (required by EPUB3 spec)
    page-list - page number navigation   (optional)
    landmarks - structural landmarks     (optional, recommended by Sigil)

  Nav-entry map shape:
    {:title    \"Chapter 1\"
     :href     \"Text/ch01.xhtml\"        ; relative to nav.xhtml location
     :children [{...}]}                 ; optional nested entries"
  (:require [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn- indent [n] (str/join (repeat (* 2 n) " ")))

(defn- nav-li
  "Render a <li> entry (recursive for nested children)."
  [depth {:keys [title href children epub-type]}]
  (let [type-attr (when epub-type (str " epub:type=\"" epub-type "\""))]
    (if (seq children)
      (str (indent depth) "<li>\n"
           (indent (inc depth)) "<a href=\"" href "\">" title "</a>\n"
           (indent (inc depth)) "<ol>\n"
           (str/join "\n" (map #(nav-li (+ depth 2) %) children)) "\n"
           (indent (inc depth)) "</ol>\n"
           (indent depth) "</li>")
      (str (indent depth) "<li><a href=\"" href "\"" type-attr ">" title "</a></li>"))))

(defn- nav-section
  "Render a full <nav> block."
  [epub-type id heading hidden? entries]
  (str "  <nav epub:type=\"" epub-type "\" id=\"" id "\""
       (when hidden? " hidden=\"\"") ">\n"
       (when heading (str "    <h1>" heading "</h1>\n"))
       "    <ol>\n"
       (str/join "\n" (map #(nav-li 3 %) entries)) "\n"
       "    </ol>\n"
       "  </nav>"))

;; ---------------------------------------------------------------------------
;; Public API
;; ---------------------------------------------------------------------------

(defn generate
  "Returns a UTF-8 byte array containing a valid EPUB3 nav.xhtml.

  Options:
    :title      - document <title> (default: \"Navigation\")
    :css-href   - relative path to stylesheet (optional)
    :lang       - xml:lang (default: \"en\")
    :toc        - seq of nav-entry maps for the TOC  (required)
    :page-list  - seq of page nav-entry maps         (optional, hidden nav)
    :landmarks  - seq of landmark nav-entry maps     (optional, hidden nav)

  Landmark entry shape:
    {:title \"Cover\" :href \"Text/cover.xhtml\" :epub-type \"cover\"}"
  [{:keys [title css-href lang toc page-list landmarks]
    :or   {title "Navigation" lang "en" toc []}}]
  (let [css-link  (when css-href
                    (str "  <link rel=\"stylesheet\" type=\"text/css\" href=\""
                         css-href "\"/>\n"))
        sections  (remove nil?
                          [(nav-section "toc" "toc" "Contents" false toc)
                           (when (seq page-list)
                             (nav-section "page-list" "page-list" nil true page-list))
                           (when (seq landmarks)
                             ;; landmarks nav: provides reachability for non-linear items
                             ;; (cover, nav itself) — required by epubcheck OPF-096
                             (nav-section "landmarks" "landmarks" nil true landmarks))])
        html
        (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
             "<!DOCTYPE html>\n"
             "<html xmlns=\"http://www.w3.org/1999/xhtml\"\n"
             "      xmlns:epub=\"http://www.idpf.org/2007/ops\"\n"
             "      xml:lang=\"" lang "\">\n"
             "<head>\n"
             "  <meta charset=\"UTF-8\"/>\n"
             "  <title>" title "</title>\n"
             css-link
             "</head>\n"
             "<body>\n"
             (str/join "\n\n" sections) "\n"
             "</body>\n"
             "</html>\n")]
    (.getBytes html "UTF-8")))

;; ---------------------------------------------------------------------------
;; Helper: chapters spec → nav entries
;; ---------------------------------------------------------------------------

(defn chapters->toc-entries
  "Convert a seq of chapter maps into nav-entry maps.

  Chapter map shape:
    {:title  \"Chapter 1\"
     :href   \"Text/ch01.xhtml\"
     :toc    [{:title \"§1\" :href \"Text/ch01.xhtml#s1\"}]}"
  [chapters]
  (letfn [(convert [{:keys [title href toc]}]
            {:title    title
             :href     href
             :children (map convert (or toc []))})]
    (map convert chapters)))

(defn default-landmarks
  "Build a standard landmarks nav from cover/toc/bodymatter hrefs."
  [{:keys [cover-href toc-href]}]
  (remove nil?
          [(when cover-href   {:title "Cover"       :href cover-href   :epub-type "cover"})
           (when toc-href     {:title "Contents"    :href toc-href     :epub-type "toc"})]))
