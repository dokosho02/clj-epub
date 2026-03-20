(ns clj-epub.nav-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-epub.nav :as nav]
            [clojure.string :as str]))

(def ^:private sample-toc
  [{:title "Title Page" :href "Text/titlepage.xhtml"}
   {:title "Chapter 1"  :href "Text/ch01.xhtml"
    :children [{:title "Section 1.1" :href "Text/ch01.xhtml#s1"}
               {:title "Section 1.2" :href "Text/ch01.xhtml#s2"}]}
   {:title "Chapter 2"  :href "Text/ch02.xhtml"}])

(defn- nav-str [opts]
  (String. (nav/generate opts) "UTF-8"))

(deftest test-xhtml-doctype
  (testing "nav document has XHTML5 doctype"
    (let [html (nav-str {:toc sample-toc})]
      (is (str/includes? html "<!DOCTYPE html>")))))

(deftest test-epub-namespace
  (testing "epub namespace is declared"
    (let [html (nav-str {:toc sample-toc})]
      (is (str/includes? html "xmlns:epub=\"http://www.idpf.org/2007/ops\"")))))

(deftest test-toc-nav-type
  (testing "main nav has epub:type=\"toc\""
    (let [html (nav-str {:toc sample-toc})]
      (is (str/includes? html "epub:type=\"toc\"")))))

(deftest test-toc-entries-present
  (testing "all TOC entries appear in the nav document"
    (let [html (nav-str {:toc sample-toc})]
      (is (str/includes? html "Title Page"))
      (is (str/includes? html "Chapter 1"))
      (is (str/includes? html "Chapter 2")))))

(deftest test-nested-toc
  (testing "nested TOC entries are rendered"
    (let [html (nav-str {:toc sample-toc})]
      (is (str/includes? html "Section 1.1"))
      (is (str/includes? html "ch01.xhtml#s1")))))

(deftest test-css-link
  (testing "CSS link is included when css-href provided"
    (let [html (nav-str {:toc [] :css-href "../Styles/stylesheet.css"})]
      (is (str/includes? html "stylesheet.css")))))

(deftest test-no-css-link
  (testing "No CSS link when css-href omitted"
    (let [html (nav-str {:toc []})]
      (is (not (str/includes? html "link rel=\"stylesheet\""))))))

(deftest test-landmarks-hidden
  (testing "landmarks nav is hidden by default"
    (let [html (nav-str {:toc []
                         :landmarks [{:title "Cover" :href "Text/cover.xhtml"
                                      :epub-type "cover"}]})]
      (is (str/includes? html "epub:type=\"landmarks\""))
      (is (str/includes? html "hidden")))))

(deftest test-lang-attribute
  (testing "xml:lang is set correctly"
    (let [html (nav-str {:toc [] :lang "zh"})]
      (is (str/includes? html "xml:lang=\"zh\"")))))

(deftest test-chapters->toc-entries
  (testing "chapters->toc-entries converts correctly"
    (let [chapters [{:title "Ch1" :href "Text/ch01.xhtml"
                     :toc [{:title "S1" :href "Text/ch01.xhtml#s1"}]}
                    {:title "Ch2" :href "Text/ch02.xhtml"}]
          entries  (nav/chapters->toc-entries chapters)]
      (is (= 2 (count entries)))
      (is (= "Ch1" (:title (first entries))))
      (is (= 1 (count (:children (first entries)))))
      (is (= "S1" (-> entries first :children first :title))))))

(deftest test-default-landmarks
  (testing "default-landmarks generates correct entries"
    (let [lm (nav/default-landmarks {:cover-href "Text/cover.xhtml"
                                     :toc-href   "nav.xhtml"
                                     :body-href  "Text/ch01.xhtml"})]
      (is (= 3 (count lm)))
      (is (some #(= "cover" (:epub-type %)) lm))
      (is (some #(= "toc"   (:epub-type %)) lm))
      (is (some #(= "bodymatter" (:epub-type %)) lm)))))
