(ns clj-epub.container-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-epub.container :as container]
            [clojure.string :as str]))

(defn- container-str
  ([] (container-str {}))
  ([opts] (String. (container/generate opts) "UTF-8")))

(deftest test-default-opf-path
  (testing "default container.xml points to OEBPS/content.opf"
    (let [xml (container-str)]
      (is (str/includes? xml "OEBPS/content.opf")))))

(deftest test-custom-opf-path
  (testing "custom OPF path is honoured"
    (let [xml (container-str {:opf-path "Content/book.opf"})]
      (is (str/includes? xml "Content/book.opf")))))

(deftest test-container-namespace
  (testing "container.xml uses correct OCF namespace"
    (let [xml (container-str)]
      (is (str/includes? xml "urn:oasis:names:tc:opendocument:xmlns:container")))))

(deftest test-container-version
  (testing "container element has version 1.0"
    (let [xml (container-str)]
      (is (str/includes? xml "version=\"1.0\"")))))

(deftest test-media-type-attribute
  (testing "rootfile has the correct media-type"
    (let [xml (container-str)]
      (is (str/includes? xml "application/oebps-package+xml")))))

(deftest test-returns-bytes
  (testing "generate returns a byte array"
    (let [result (container/generate)]
      (is (bytes? result))
      (is (pos? (alength result))))))
