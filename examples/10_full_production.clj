;; =============================================================================
;; Example 10 — Full production-grade usage
;; =============================================================================
;;
;; Combines all features:
;;   - Full metadata (multiple creators, series, rights)
;;   - Cover image + cover page + title page
;;   - Multiple chapters, one with nested TOC sub-entries
;;   - Custom fonts and image resources
;;   - Custom CSS
;;   - EPUB2 compatibility
;;
;; This is the closest to a real-world publication configuration.

(require '[clj-epub.core  :as epub]
         '[clj-epub.xhtml :as xhtml])

(defn read-bytes [path]
  (java.nio.file.Files/readAllBytes
   (java.nio.file.Paths/get path (into-array String []))))

(def book-css
  "@font-face {
  font-family: 'BookSerif';
  src: url('../Fonts/SourceSerif4-Regular.otf') format('opentype');
  font-weight: normal;
}
@font-face {
  font-family: 'BookSerif';
  src: url('../Fonts/SourceSerif4-Bold.otf') format('opentype');
  font-weight: bold;
}
body {
  font-family: 'BookSerif', Georgia, serif;
  font-size: 1em;
  line-height: 1.7;
  margin: 6% 8%;
  color: #1a1a1a;
}
h1 { font-size: 1.9em; margin-top: 2em; margin-bottom: 0.6em; }
h2 { font-size: 1.4em; margin-top: 1.5em; margin-bottom: 0.4em; color: #333; }
p  { margin: 0; text-indent: 1.5em; }
p:first-of-type, p + h2 + p { text-indent: 0; }
blockquote {
  margin: 1.2em 2em;
  padding: 0.4em 0.8em;
  border-left: 3px solid #999;
  color: #555;
  font-style: italic;
}
code {
  font-family: 'Menlo', monospace;
  font-size: 0.88em;
  background: #f0f0f0;
  padding: 0.1em 0.3em;
  border-radius: 2px;
}
#cover { text-align: center; padding: 0; margin: 0; }
#cover img { width: 100%; height: auto; }")

(epub/make-epub!
 {:metadata {:title           "The Art of Clojure"
             :language        "en"
             :identifier      "urn:uuid:c1d2e3f4-a5b6-7890-cdef-012345678901"
             :creators        [{:name    "Rich Hickey"
                                :role    "aut"
                                :file-as "Hickey, Rich"}
                               {:name    "Stuart Halloway"
                                :role    "aut"
                                :file-as "Halloway, Stuart"}]
             :publisher       "Pragmatic Bookshelf"
             :date            "2024-03-01"
             :description     "A comprehensive guide to Clojure — its philosophy,
                               data model, concurrency primitives, and ecosystem."
             :subjects        ["Clojure" "Functional Programming"
                               "JVM" "Software Design"]
             :rights          "© 2024 Pragmatic Bookshelf. All rights reserved."
             :series          "The Art of Programming"
             :series-position 3}

  :cover {:image-href "Images/cover.jpg"
          :image-data (read-bytes "cover.jpg")}

  :chapters [
   ;; Cover page: linear=false, excluded from linear reading flow.
   {:id      "cover-page"
    :title   "Cover"
    :href    "Text/cover.xhtml"
    :content "<div id=\"cover\">
                <img src=\"../Images/cover.jpg\" alt=\"The Art of Clojure\"/>
              </div>"
    :linear  false}

   ;; Title page: generated using the xhtml/title-page template.
   {:id    "titlepage"
    :title "Title Page"
    :href  "Text/titlepage.xhtml"
    :data  (xhtml/->bytes
            (xhtml/title-page {:title     "The Art of Clojure"
                               :author    "Rich Hickey & Stuart Halloway"
                               :publisher "Pragmatic Bookshelf"
                               :date      "2024"
                               :css-href  "../Styles/stylesheet.css"}))}

   ;; Body chapters
   {:id      "ch01"
    :title   "Part I: Philosophy"
    :href    "Text/ch01.xhtml"
    :content "<h1>Part I: Philosophy</h1>
              <h2 id=\"simplicity\">Simplicity</h2>
              <p>Simplicity is the art of maximizing the amount of work not done.</p>
              <blockquote>\"Simple is not easy.\" — Rich Hickey</blockquote>
              <h2 id=\"immutability\">Immutability</h2>
              <p>Values never change. Identity is separate from state.</p>"
    ;; Nested TOC: sub-entry hrefs contain fragments, which the library strips automatically.
    ;; To preserve fragments, add matching id attributes in the chapter :content.
    :toc    [{:title "Simplicity"   :href "Text/ch01.xhtml#simplicity"}
             {:title "Immutability" :href "Text/ch01.xhtml#immutability"}]}

   {:id      "ch02"
    :title   "Part II: Data Model"
    :href    "Text/ch02.xhtml"
    :content "<h1>Part II: Data Model</h1>
              <p>Clojure's persistent data structures provide structural sharing
              for efficient immutable updates.</p>
              <p>The four fundamental structures are: lists, vectors, maps, sets.</p>"}

   {:id      "ch03"
    :title   "Part III: Concurrency"
    :href    "Text/ch03.xhtml"
    :content "<h1>Part III: Concurrency</h1>
              <p>Clojure separates identity from state. Atoms, refs, and agents
              provide different concurrency semantics.</p>
              <p>Use <code>atom</code> for uncoordinated, synchronous updates.
              Use <code>ref</code> with STM for coordinated updates.</p>"}

   {:id      "appendix"
    :title   "Appendix: Cheat Sheet"
    :href    "Text/appendix.xhtml"
    :content "<h1>Appendix: Cheat Sheet</h1>
              <p>Quick reference for the most commonly used functions.</p>"}]

  :css my-book-css  ; Use the full CSS defined above

  :resources [
   {:id         "font-regular"
    :href        "Fonts/SourceSerif4-Regular.otf"
    :media-type  "font/otf"
    :data        (read-bytes "SourceSerif4-Regular.otf")}
   {:id         "font-bold"
    :href        "Fonts/SourceSerif4-Bold.otf"
    :media-type  "font/otf"
    :data        (read-bytes "SourceSerif4-Bold.otf")}]

  :options {:epub2-compat true}}  ; Include toc.ncx for compatibility with older readers

 "10-full-production.epub")
