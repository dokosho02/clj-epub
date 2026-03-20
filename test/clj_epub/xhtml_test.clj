(ns clj-epub.xhtml-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-epub.xhtml :as xhtml]
            [clojure.string :as str]))

(deftest test-wrap-chapter-doctype
  (testing "wrapped chapter has HTML5 doctype"
    (let [html (xhtml/wrap-chapter "<p>test</p>" {})]
      (is (str/includes? html "<!DOCTYPE html>")))))

(deftest test-wrap-chapter-xhtml-namespace
  (testing "wrapped chapter declares XHTML namespace"
    (let [html (xhtml/wrap-chapter "<p>test</p>" {})]
      (is (str/includes? html "http://www.w3.org/1999/xhtml")))))

(deftest test-wrap-chapter-epub-namespace
  (testing "wrapped chapter declares epub namespace"
    (let [html (xhtml/wrap-chapter "<p>test</p>" {})]
      (is (str/includes? html "http://www.idpf.org/2007/ops")))))

(deftest test-wrap-chapter-epub-type
  (testing "section has epub:type attribute"
    (let [html (xhtml/wrap-chapter "<p>test</p>" {:epub-type "chapter"})]
      (is (str/includes? html "epub:type=\"chapter\"")))))

(deftest test-wrap-chapter-content-included
  (testing "body content is included in output"
    (let [html (xhtml/wrap-chapter "<h1>My Title</h1><p>My content.</p>" {})]
      (is (str/includes? html "My Title"))
      (is (str/includes? html "My content.")))))

(deftest test-wrap-chapter-title
  (testing "title option sets the <title> element"
    (let [html (xhtml/wrap-chapter "" {:title "Chapter One"})]
      (is (str/includes? html "<title>Chapter One</title>")))))

(deftest test-wrap-chapter-css
  (testing "css-href generates a link element"
    (let [html (xhtml/wrap-chapter "" {:css-href "../Styles/stylesheet.css"})]
      (is (str/includes? html "stylesheet.css"))
      (is (str/includes? html "text/css")))))

(deftest test-wrap-chapter-lang
  (testing "xml:lang attribute is set"
    (let [html (xhtml/wrap-chapter "" {:lang "zh"})]
      (is (str/includes? html "xml:lang=\"zh\"")))))

(deftest test-wrap-chapter-bytes
  (testing "wrap-chapter-bytes returns a byte array"
    (let [b (xhtml/wrap-chapter-bytes "<p>test</p>" {})]
      (is (bytes? b))
      (is (pos? (alength b))))))

(deftest test-cover-page
  (testing "cover-page generates valid XHTML with image"
    (let [html (xhtml/cover-page {:image-href "../Images/cover.jpg"
                                  :title "Test Book"})]
      (is (str/includes? html "cover.jpg"))
      (is (str/includes? html "epub:type=\"cover\""))
      (is (str/includes? html "id=\"cover\"")))))

(deftest test-title-page
  (testing "title-page generates valid XHTML with title and author"
    (let [html (xhtml/title-page {:title "My Book" :author "Author Name"})]
      (is (str/includes? html "My Book"))
      (is (str/includes? html "Author Name"))
      (is (str/includes? html "epub:type=\"titlepage\"")))))

(deftest test-default-stylesheet-not-empty
  (testing "default stylesheet is non-empty and is valid CSS"
    (let [css xhtml/default-stylesheet]
      (is (string? css))
      (is (pos? (count css)))
      (is (str/includes? css "body"))
      (is (str/includes? css "font-family")))))
