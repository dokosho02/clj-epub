(ns clj-epub.test-helpers
  "Shared test utilities and fixtures."
  ;; (:require [clojure.string :as str])
  (:import [java.io ByteArrayInputStream]
           [java.util.zip ZipInputStream ZipEntry]))

;; ---------------------------------------------------------------------------
;; ZIP inspection helpers
;; ---------------------------------------------------------------------------

(defn unzip-entries
  "Read all entries from a byte-array ZIP, returning a map of path → bytes."
  [^bytes epub-bytes]
  (let [zis   (ZipInputStream. (ByteArrayInputStream. epub-bytes))
        result (atom {})]
    (loop []
      (when-let [^ZipEntry entry (.getNextEntry zis)]
        (let [name   (.getName entry)
              baos   (java.io.ByteArrayOutputStream.)
              buf    (byte-array 4096)]
          (loop []
            (let [n (.read zis buf)]
              (when (pos? n)
                (.write baos buf 0 n)
                (recur))))
          (swap! result assoc name (.toByteArray baos))
          (.closeEntry zis)
          (recur))))
    (.close zis)
    @result))

(defn entry-names
  "Return the ordered list of entry names from a ZIP byte array.
   Uses a separate pass to preserve ordering."
  [^bytes epub-bytes]
  (let [zis   (ZipInputStream. (ByteArrayInputStream. epub-bytes))
        names (atom [])]
    (loop []
      (when-let [^ZipEntry e (.getNextEntry zis)]
        (swap! names conj (.getName e))
        (.closeEntry zis)
        (recur)))
    (.close zis)
    @names))

(defn entry-method
  "Return the compression method (0=STORED, 8=DEFLATED) of a named ZIP entry."
  [^bytes epub-bytes entry-name]
  (let [zis (ZipInputStream. (ByteArrayInputStream. epub-bytes))]
    (loop []
      (when-let [^ZipEntry e (.getNextEntry zis)]
        (if (= (.getName e) entry-name)
          (do (.close zis) (.getMethod e))
          (do (.closeEntry zis) (recur)))))))

(defn entry-str
  "Read a named entry from a ZIP byte-array and return it as a UTF-8 string."
  [entries path]
  (when-let [data (get entries path)]
    (String. data "UTF-8")))

;; ---------------------------------------------------------------------------
;; Minimal sample epub opts
;; ---------------------------------------------------------------------------

(def minimal-opts
  {:metadata {:title    "Test Book"
              :language "en"
              :identifier "urn:uuid:test-0001"}
   :chapters [{:id      "ch01"
               :title   "Chapter One"
               :href    "Text/ch01.xhtml"
               :content "<h1>Chapter One</h1><p>Hello, world!</p>"}]})

(def full-opts
  {:metadata {:title       "Full Test Book"
              :language    "zh"
              :identifier  "urn:uuid:test-full-0001"
              :creators    [{:name "张三" :role "aut" :file-as "张, 三"}]
              :publisher   "Test Press"
              :date        "2024-06-01"
              :description "A complete test book."
              :subjects    ["Fiction" "Testing"]
              :rights      "CC-BY 4.0"
              :series      "Test Series"
              :series-position 1}
   :chapters [{:id      "cover-page"  ;; intentionally NOT "cover" — library must not depend on id name
               :title   "Cover"
               :href    "Text/cover.xhtml"
               :content "<div class=\"cover\"><img src=\"../Images/cover.png\" alt=\"cover\"/></div>"
               :linear  false}
              {:id      "titlepage"
               :title   "Title Page"
               :href    "Text/titlepage.xhtml"
               :content "<div class=\"titlepage\"><h1>Full Test Book</h1></div>"}
              {:id      "ch01"
               :title   "第一章"
               :href    "Text/ch01.xhtml"
               :content "<h1>第一章</h1><p>内容。</p>"
               :toc     [{:title "第一节" :href "Text/ch01.xhtml#s1"}]}
              {:id      "ch02"
               :title   "第二章"
               :href    "Text/ch02.xhtml"
               :content "<h1>第二章</h1><p>更多内容。</p>"}]
   :resources [{:id   "cover-img"
                :href "Images/cover.png"
                :media-type "image/png"
                :data (byte-array [0x89 0x50 0x4E 0x47])}]})
