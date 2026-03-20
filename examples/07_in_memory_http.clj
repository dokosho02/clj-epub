;; =============================================================================
;; Example 07 — In-memory generation + HTTP response (Ring)
;; =============================================================================
;;
;; Demonstrates:
;;   - make-epub-bytes returning a byte array instead of writing to disk
;;   - Returning an EPUB download response directly from a Ring HTTP handler
;;   - Suitable for web services that generate ebooks dynamically

(require '[clj-epub.core :as epub])

;; --- Ring handler example ---
(defn generate-epub-handler
  "Ring handler: dynamically generate an EPUB from request params and return a download response."
  [request]
  (let [;; Read book title from request params (in real projects, query a database)
        title    (get-in request [:params :title] "Generated Book")
        author   (get-in request [:params :author] "Anonymous")

        ;; Build opts (in real projects, content comes from a database or template)
        opts     {:metadata {:title    title
                             :language "en"
                             :author   author}
                  :chapters [{:id      "ch01"
                              :title   "Chapter 1"
                              :href    "Text/ch01.xhtml"
                              :content (str "<h1>" title "</h1>"
                                            "<p>By " author "</p>"
                                            "<p>Generated at runtime.</p>")}]}

        ;; Generate byte array (no disk I/O)
        epub-bytes (epub/make-epub-bytes opts)

        ;; Build filename (handle spaces)
        filename   (str (clojure.string/replace title " " "-") ".epub")]

    {:status  200
     :headers {"Content-Type"        "application/epub+zip"
               "Content-Disposition" (str "attachment; filename=\"" filename "\"")
               "Content-Length"      (str (alength epub-bytes))}
     :body    (java.io.ByteArrayInputStream. epub-bytes)}))

;; --- Batch generation example ---
(defn batch-generate
  "Generate multiple books in batch; returns a map of {title -> byte-array}."
  [books]
  (into {}
        (map (fn [{:keys [title] :as book-opts}]
               [title (epub/make-epub-bytes book-opts)])
             books)))

;; Usage examples
(comment
  ;; Single book
  (let [bytes (epub/make-epub-bytes
               {:metadata {:title "Quick Book" :language "en"}
                :chapters [{:id "ch01" :title "Chapter 1"
                            :href "Text/ch01.xhtml"
                            :content "<h1>Hello</h1>"}]})]
    (println "Generated" (alength bytes) "bytes"))

  ;; Batch generation
  (let [result (batch-generate
                [{:metadata {:title "Book A" :language "en"}
                  :chapters [{:id "ch01" :title "Ch1"
                              :href "Text/ch01.xhtml" :content "<h1>A</h1>"}]}
                 {:metadata {:title "Book B" :language "zh"}
                  :chapters [{:id "ch01" :title "Chapter 1"
                              :href "Text/ch01.xhtml" :content "<h1>B</h1>"}]}])]
    (doseq [[title bytes] result]
      (println title "->" (alength bytes) "bytes"))))
