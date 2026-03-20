;; =============================================================================
;; Example 01 — Minimal usage
;; =============================================================================
;;
;; Only three required fields: title, language, and chapters.
;; Everything else is handled automatically:
;;   - UUID identifier is auto-generated
;;   - dcterms:modified timestamp is auto-generated
;;   - Built-in default CSS is attached
;;   - toc.ncx (EPUB2 compatibility) is auto-generated
;;
;; The output passes epubcheck validation and opens in Sigil, Calibre, and Apple Books.

(require '[clj-epub.core :as epub])

(epub/make-epub!
 {:metadata {:title    "My First Book"
             :language "en"}

  :chapters [{:id      "ch01"
              :title   "Chapter 1"
              :href    "Text/ch01.xhtml"
              :content "<h1>Chapter 1</h1>
                        <p>Welcome to my first EPUB.</p>"}]}

 "01-minimal.epub")
