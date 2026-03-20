;; =============================================================================
;; Example 08 — Pre-rendered XHTML (:data field)
;; =============================================================================
;;
;; Demonstrates:
;;   - :data passing a pre-rendered complete XHTML byte array
;;   - Useful when content comes from an external source (Pandoc, database, template engine)
;;   - :data and :content are mutually exclusive; :data bypasses the library's XHTML wrapping
;;   - Direct use of xhtml/wrap-chapter and xhtml/wrap-chapter-bytes

(require '[clj-epub.core  :as epub]
         '[clj-epub.xhtml :as xhtml])

;; --- Manually wrap using xhtml/wrap-chapter ---
;; When you need precise control over a chapter,
;; call wrap-chapter yourself and pass the result via :data.

(def ch01-bytes
  (xhtml/wrap-chapter-bytes
   ;; Body content (inner HTML)
   "<h1>Chapter 1: Manual Wrapping</h1>
    <p>This chapter was wrapped manually using xhtml/wrap-chapter.</p>
    <p>You have full control over the XHTML structure.</p>"
   ;; Options
   {:title    "Chapter 1: Manual Wrapping"
    :css-href "../Styles/stylesheet.css"
    :lang     "en"
    :epub-type "chapter"}))

;; --- Fetch content from an external system ---
;; Assume the database returns a complete XHTML string
(defn fetch-chapter-from-db [chapter-id]
  ;; In a real project, query the database here
  (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
       "<!DOCTYPE html>\n"
       "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">\n"
       "<head><meta charset=\"UTF-8\"/><title>Chapter " chapter-id "</title></head>\n"
       "<body><section epub:type=\"chapter\" "
       "xmlns:epub=\"http://www.idpf.org/2007/ops\">\n"
       "<h1>Chapter " chapter-id "</h1>\n"
       "<p>Content from database.</p>\n"
       "</section></body></html>\n"))

(epub/make-epub!
 {:metadata {:title    "Pre-rendered XHTML Demo"
             :language "en"
             :author   "Demo Author"}

  :chapters [
   ;; Option 1: manually wrap using wrap-chapter-bytes
   {:id   "ch01"
    :title "Chapter 1: Manual Wrapping"
    :href  "Text/ch01.xhtml"
    :data  ch01-bytes}   ; Pass byte array; bypasses automatic wrapping

   ;; Option 2: fetch complete XHTML from external system and convert to bytes
   {:id   "ch02"
    :title "Chapter 2: From Database"
    :href  "Text/ch02.xhtml"
    :data  (.getBytes (fetch-chapter-from-db "2") "UTF-8")}

   ;; Option 3: :content string (normal usage, shown for comparison)
   {:id      "ch03"
    :title   "Chapter 3: Normal Content"
    :href    "Text/ch03.xhtml"
    :content "<h1>Chapter 3: Normal Content</h1>
              <p>This uses the normal :content field.
                 The library wraps it automatically.</p>"}]}

 "08-pre-rendered.epub")
