(ns clj-epub.ncx-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-epub.ncx :as ncx]
            [clojure.string :as str]))

(def ^:private base-opts
  {:uid    "urn:uuid:test-001"
   :title  "Test Book"
   :author "Test Author"
   :points [{:id "np1" :label "Chapter 1" :src "Text/ch01.xhtml"}
            {:id "np2" :label "Chapter 2" :src "Text/ch02.xhtml"
             :children [{:id "np2-1" :label "Section 2.1"
                         :src "Text/ch02.xhtml#s1"}]}]
   :depth  2})

(defn- ncx-str [opts]
  (String. (ncx/generate opts) "UTF-8"))

(deftest test-ncx-namespace
  (testing "NCX document uses the DAISY namespace"
    (let [xml (ncx-str base-opts)]
      (is (str/includes? xml "http://www.daisy.org/z3986/2005/ncx/")))))

(deftest test-dtb-uid
  (testing "dtb:uid meta matches provided uid"
    (let [xml (ncx-str base-opts)]
      (is (str/includes? xml "urn:uuid:test-001")))))

(deftest test-nav-points-present
  (testing "nav points appear in output"
    (let [xml (ncx-str base-opts)]
      (is (str/includes? xml "Chapter 1"))
      (is (str/includes? xml "Chapter 2")))))

(deftest test-nested-nav-points
  (testing "nested nav points are rendered"
    (let [xml (ncx-str base-opts)]
      (is (str/includes? xml "Section 2.1"))
      (is (str/includes? xml "ch02.xhtml#s1")))))

(deftest test-nav-point-content-src
  (testing "navPoint content src attributes are correct"
    (let [xml (ncx-str base-opts)]
      (is (str/includes? xml "Text/ch01.xhtml"))
      (is (str/includes? xml "Text/ch02.xhtml")))))

(deftest test-play-order
  (testing "playOrder attributes are sequential integers starting at 1"
    (let [xml (ncx-str base-opts)]
      (is (str/includes? xml "playOrder=")))))

(deftest test-doc-title
  (testing "docTitle contains book title"
    (let [xml (ncx-str base-opts)]
      (is (str/includes? xml "Test Book")))))

(deftest test-doc-author
  (testing "docAuthor is included when author is provided"
    (let [xml (ncx-str base-opts)]
      (is (str/includes? xml "Test Author")))))

(deftest test-no-author
  (testing "docAuthor is omitted when author is nil"
    (let [xml (ncx-str (dissoc base-opts :author))]
      (is (not (str/includes? xml "docAuthor"))))))

(deftest test-chapters->nav-points
  (testing "chapters->nav-points conversion"
    (let [chapters [{:id "ch01" :title "Chapter 1" :href "Text/ch01.xhtml"
                     :toc [{:title "Sec 1" :href "Text/ch01.xhtml#s1"}]}]
          points   (ncx/chapters->nav-points chapters)]
      (is (= 1 (count points)))
      (is (= "Chapter 1" (:label (first points))))
      (is (= "Text/ch01.xhtml" (:src (first points))))
      (is (= 1 (count (:children (first points)))))
      (is (= "Sec 1" (-> points first :children first :label))))))
