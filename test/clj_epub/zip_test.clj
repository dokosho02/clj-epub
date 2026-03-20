(ns clj-epub.zip-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-epub.zip :as zip]
            [clj-epub.test-helpers :as h])
  (:import [java.util.zip ZipEntry]))

(deftest test-mimetype-is-first
  (testing "mimetype is the first entry in the ZIP"
    (let [entries [{:path "META-INF/container.xml" :data (byte-array [1 2 3])}
                   {:path "OEBPS/content.opf"      :data (byte-array [4 5 6])}]
          epub    (zip/epub->bytes entries)
          names   (h/entry-names epub)]
      (is (= "mimetype" (first names))
          "mimetype must be the first ZIP entry"))))

(deftest test-mimetype-is-stored
  (testing "mimetype entry uses STORE (no compression)"
    (let [epub (zip/epub->bytes [])]
      (is (= ZipEntry/STORED (h/entry-method epub "mimetype"))
          "mimetype must use STORED compression method"))))

(deftest test-mimetype-content
  (testing "mimetype file contains exactly 'application/epub+zip'"
    (let [epub    (zip/epub->bytes [])
          entries (h/unzip-entries epub)
          content (String. (get entries "mimetype") "US-ASCII")]
      (is (= "application/epub+zip" content)))))

(deftest test-other-entries-are-deflated
  (testing "non-mimetype entries use DEFLATE"
    (let [epub (zip/epub->bytes [{:path "test.txt"
                                  :data (.getBytes "hello world" "UTF-8")}])]
      (is (= ZipEntry/DEFLATED (h/entry-method epub "test.txt"))))))

(deftest test-all-entries-present
  (testing "all added entries appear in the output"
    (let [paths   ["META-INF/container.xml" "OEBPS/content.opf" "OEBPS/nav.xhtml"]
          entries (mapv #(hash-map :path % :data (.getBytes % "UTF-8")) paths)
          epub    (zip/epub->bytes entries)
          names   (set (h/entry-names epub))]
      (is (contains? names "mimetype"))
      (doseq [p paths]
        (is (contains? names p) (str "Missing entry: " p))))))

(deftest test-write-epub-to-file
  (testing "write-epub! creates a file at the specified path"
    (let [tmp (java.io.File/createTempFile "clj-epub-test" ".epub")]
      (.deleteOnExit tmp)
      (zip/write-epub! (.getAbsolutePath tmp)
                       [{:path "META-INF/container.xml"
                         :data (.getBytes "test" "UTF-8")}])
      (is (.exists tmp))
      (is (pos? (.length tmp))))))
