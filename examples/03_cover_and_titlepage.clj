;; =============================================================================
;; Example 03 — Cover image + cover page + title page
;; =============================================================================
;;
;; Demonstrates:
;;   - :cover field adds the cover image (automatically marked properties="cover-image")
;;   - Cover chapter uses :linear false to exclude it from the linear reading flow
;;   - Title page chapter
;;   - Using xhtml/cover-page and xhtml/title-page templates
;;
;; Notes:
;;   - The cover chapter id does not need to be "cover"; the library detects it via linear=false
;;   - The cover image and cover XHTML page are separate: image goes in manifest, page goes in spine

(require '[clj-epub.core  :as epub]
         '[clj-epub.xhtml :as xhtml])

;; Helper function to read file bytes
(defn read-bytes [path]
  (java.nio.file.Files/readAllBytes
   (java.nio.file.Paths/get path (into-array String []))))

(epub/make-epub!
 {:metadata {:title    "The Great Novel"
             :language "en"
             :author   "Alex Chen"
             :publisher "Fiction House"
             :date     "2024-09-15"}

  ;; Cover image: automatically marked as properties="cover-image".
  ;; Readers (Apple Books, Kobo, etc.) use this for the bookshelf thumbnail.
  :cover {:image-href "Images/cover.jpg"
          :image-data (read-bytes "cover.jpg")}

  :chapters [
   ;; Cover page: linear=false, excluded from the linear reading flow.
   ;; The library detects it as the cover via linear=false and adds it to landmarks.
   {:id      "cover-page"
    :title   "Cover"
    :href    "Text/cover.xhtml"
    :content "<div class=\"cover\">
                <img src=\"../Images/cover.jpg\" alt=\"Cover\"/>
              </div>"
    :linear  false}

   ;; Title page: use the built-in xhtml/title-page template.
   ;; xhtml/title-page returns a complete XHTML string.
   {:id      "titlepage"
    :title   "Title Page"
    :href    "Text/titlepage.xhtml"
    ;; Pass the pre-rendered XHTML bytes directly; the library skips its wrapping step.
    :data    (xhtml/->bytes
              (xhtml/title-page {:title     "The Great Novel"
                                 :author    "Alex Chen"
                                 :publisher "Fiction House"
                                 :date      "2024"
                                 :css-href  "../Styles/stylesheet.css"}))}

   {:id      "ch01"
    :title   "Chapter 1: The Beginning"
    :href    "Text/ch01.xhtml"
    :content "<h1>Chapter 1: The Beginning</h1>
              <p>It was a dark and stormy night.</p>"}

   {:id      "ch02"
    :title   "Chapter 2: The Journey"
    :href    "Text/ch02.xhtml"
    :content "<h1>Chapter 2: The Journey</h1>
              <p>The road stretched endlessly before them.</p>"}]}

 "03-cover-and-titlepage.epub")
