;; =============================================================================
;; Example 02 — Multiple chapters + full metadata
;; =============================================================================
;;
;; Demonstrates:
;;   - Multiple chapters and automatic TOC generation
;;   - Common metadata fields (author, publisher, date, description, subjects)
;;   - Automatic navigation document generation

(require '[clj-epub.core :as epub])

(epub/make-epub!
 {:metadata {:title       "Clojure in Depth"
             :language    "en"
             :author      "Jane Smith"
             :publisher   "REPL Press"
             :date        "2024-06-01"
             :description "A comprehensive guide to Clojure programming."
             :subjects    ["Programming" "Clojure" "Functional Programming"]}

  :chapters [{:id      "intro"
              :title   "Introduction"
              :href    "Text/intro.xhtml"
              :content "<h1>Introduction</h1>
                        <p>Clojure is a dynamic, functional language on the JVM.</p>
                        <p>This book covers its core concepts from first principles.</p>"}

             {:id      "ch01"
              :title   "Chapter 1: Data Structures"
              :href    "Text/ch01.xhtml"
              :content "<h1>Chapter 1: Data Structures</h1>
                        <p>Clojure's persistent data structures are immutable by default.</p>
                        <h2>Lists</h2>
                        <p>Lists are linked lists, ideal for sequential access.</p>
                        <h2>Vectors</h2>
                        <p>Vectors provide O(log n) random access.</p>"}

             {:id      "ch02"
              :title   "Chapter 2: Functions"
              :href    "Text/ch02.xhtml"
              :content "<h1>Chapter 2: Functions</h1>
                        <p>Functions are first-class citizens in Clojure.</p>
                        <p>Higher-order functions like <code>map</code>, <code>filter</code>,
                        and <code>reduce</code> form the core of idiomatic Clojure.</p>"}

             {:id      "ch03"
              :title   "Chapter 3: Concurrency"
              :href    "Text/ch03.xhtml"
              :content "<h1>Chapter 3: Concurrency</h1>
                        <p>Clojure provides atoms, refs, agents, and core.async
                        for managing shared state.</p>"}

             {:id      "appendix"
              :title   "Appendix: Quick Reference"
              :href    "Text/appendix.xhtml"
              :content "<h1>Appendix: Quick Reference</h1>
                        <p>Common functions and their signatures.</p>"}]}

 "02-multiple-chapters.epub")
