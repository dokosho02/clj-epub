;; =============================================================================
;; Example 05 — Multiple creators + series metadata
;; =============================================================================
;;
;; Demonstrates:
;;   - :creators with multiple authors, each with a different MARC relator role
;;   - :series and :series-position for series metadata
;;   - :rights copyright string
;;   - role/file-as are expressed using the EPUB3 <meta refines> syntax in the OPF
;;
;; Common MARC relator roles:
;;   "aut"  Author        "edt"  Editor
;;   "trl"  Translator    "ill"  Illustrator
;;   "pbl"  Publisher     "bkp"  Book producer

(require '[clj-epub.core :as epub])

(epub/make-epub!
 {:metadata {:title           "Functional Design Patterns, Vol. 1"
             :language        "en"
             :identifier      "urn:uuid:a1b2c3d4-e5f6-7890-abcd-ef1234567890"

             ;; Multiple creators
             :creators        [{:name    "Alice Zhang"
                                :role    "aut"
                                :file-as "Zhang, Alice"}
                               {:name    "Bob Müller"
                                :role    "aut"
                                :file-as "Müller, Bob"}
                               {:name    "Carol Kim"
                                :role    "edt"       ; editor
                                :file-as "Kim, Carol"}]

             :publisher       "Lambda Books"
             :date            "2024-01-20"
             :description     "Volume 1 of a series on functional design patterns."
             :subjects        ["Software Design" "Functional Programming" "Patterns"]
             :rights          "© 2024 Lambda Books. CC BY-NC 4.0."

             ;; Series info (visible in Calibre, Apple Books, and other readers)
             :series          "Functional Design Patterns"
             :series-position 1}

  :chapters [{:id      "foreword"
              :title   "Foreword"
              :href    "Text/foreword.xhtml"
              :content "<h1>Foreword</h1>
                        <p>This series began as a series of blog posts...</p>"}

             {:id      "ch01"
              :title   "Pattern 1: Functor"
              :href    "Text/ch01.xhtml"
              :content "<h1>Pattern 1: Functor</h1>
                        <p>A functor is a structure that can be mapped over.</p>"}

             {:id      "ch02"
              :title   "Pattern 2: Monad"
              :href    "Text/ch02.xhtml"
              :content "<h1>Pattern 2: Monad</h1>
                        <p>Monads sequence computations with context.</p>"}]}

 "05-series-and-creators.epub")
