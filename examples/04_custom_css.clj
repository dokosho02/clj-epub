;; =============================================================================
;; Example 04 — Custom CSS
;; =============================================================================
;;
;; Demonstrates:
;;   - Passing a string to replace the built-in default CSS
;;   - Passing false to include no stylesheet at all
;;   - The built-in default CSS is available at clj-epub.xhtml/default-stylesheet

(require '[clj-epub.core  :as epub]
         '[clj-epub.xhtml :as xhtml])

(def my-chapters
  [{:id      "ch01"
    :title   "Chapter 1"
    :href    "Text/ch01.xhtml"
    :content "<h1>Chapter 1</h1>
              <p>This is styled with custom CSS.</p>
              <blockquote>A quote for testing blockquote styles.</blockquote>
              <pre><code>(defn hello [] (println \"Hello, World!\"))</code></pre>"}])

;; --- Option 1: custom CSS string ---
(epub/make-epub!
 {:metadata {:title "Custom CSS Demo" :language "en"}
  :chapters my-chapters
  :css "
body {
  font-family: 'Helvetica Neue', Arial, sans-serif;
  font-size: 1.1em;
  line-height: 1.8;
  margin: 8%;
  color: #2c2c2c;
}
h1 {
  font-size: 2em;
  border-bottom: 2px solid #333;
  padding-bottom: 0.3em;
  margin-bottom: 1em;
}
p { margin-bottom: 1em; }
blockquote {
  border-left: 4px solid #aaa;
  margin: 1.5em 0;
  padding: 0.5em 1em;
  color: #666;
  font-style: italic;
}
pre {
  background: #f4f4f4;
  padding: 1em;
  border-radius: 4px;
  font-size: 0.9em;
  overflow-x: auto;
}
"}
 "04a-custom-css.epub")

;; --- Option 2: disable CSS entirely ---
(epub/make-epub!
 {:metadata {:title "No CSS Demo" :language "en"}
  :chapters my-chapters
  :css false}
 "04b-no-css.epub")

;; --- Option 3: inspect the built-in default CSS ---
(println "Built-in CSS preview:")
(println (subs xhtml/default-stylesheet 0 200))
