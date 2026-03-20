(ns clj-epub.opf-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-epub.opf :as opf]
            [clojure.string :as str]))

(def ^:private base-opts
  {:metadata {:title      "Test Book"
              :language   "en"
              :identifier "urn:uuid:test-001"
              :author     "Author Name"
              :publisher  "Publisher"
              :date       "2024-01-01"}
   :manifest [{:id "nav"  :href "nav.xhtml"  :media-type "application/xhtml+xml" :properties "nav"}
              {:id "ncx"  :href "toc.ncx"    :media-type "application/x-dtbncx+xml"}
              {:id "css"  :href "Styles/stylesheet.css" :media-type "text/css"}
              {:id "ch01" :href "Text/ch01.xhtml" :media-type "application/xhtml+xml"}]
   :spine    [{:idref "ch01" :linear true}]
   :guide    [{:type "toc" :title "Contents" :href "nav.xhtml"}]
   :ncx-id  "ncx"})

(defn- opf-str [opts]
  (String. (opf/generate opts) "UTF-8"))

(deftest test-required-dc-fields
  (testing "dc:title, dc:language, dc:identifier are present"
    (let [xml (opf-str base-opts)]
      (is (str/includes? xml "Test Book"))
      (is (str/includes? xml "en"))
      (is (str/includes? xml "urn:uuid:test-001")))))

(deftest test-opf-version-attribute
  (testing "package element has version 3.0"
    (let [xml (opf-str base-opts)]
      (is (str/includes? xml "version=\"3.0\"")))))

(deftest test-manifest-items
  (testing "all manifest items appear in output"
    (let [xml (opf-str base-opts)]
      (is (str/includes? xml "id=\"nav\""))
      (is (str/includes? xml "id=\"ncx\""))
      (is (str/includes? xml "id=\"ch01\"")))))

(deftest test-nav-properties
  (testing "nav item has properties=\"nav\""
    (let [xml (opf-str base-opts)]
      (is (str/includes? xml "properties=\"nav\"")))))

(deftest test-spine-includes-chapters
  (testing "spine contains chapter idrefs"
    (let [xml (opf-str base-opts)]
      (is (str/includes? xml "idref=\"ch01\"")))))

(deftest test-spine-linear-no
  (testing "linear=false items have linear=\"no\" in spine"
    (let [opts (update base-opts :spine conj {:idref "cover" :linear false})
          opts (update opts :manifest conj {:id "cover" :href "Text/cover.xhtml"
                                            :media-type "application/xhtml+xml"})
          xml  (opf-str opts)]
      (is (str/includes? xml "linear=\"no\"")))))

(deftest test-modified-meta
  (testing "dcterms:modified meta element is present"
    (let [xml (opf-str base-opts)]
      (is (str/includes? xml "dcterms:modified")))))

(deftest test-unique-identifier
  (testing "unique-identifier attribute on package matches BookId"
    (let [xml (opf-str base-opts)]
      (is (str/includes? xml "unique-identifier=\"BookId\""))
      (is (str/includes? xml "id=\"BookId\"")))))

(deftest test-guide-element
  (testing "guide element is included"
    (let [xml (opf-str base-opts)]
      (is (str/includes? xml "<guide") (str "No guide in: " (subs xml 0 (min 500 (count xml))))))))

(deftest test-media-type-for
  (testing "media-type inference from filename"
    (is (= "application/xhtml+xml" (opf/media-type-for "chapter.xhtml")))
    (is (= "image/jpeg"            (opf/media-type-for "cover.jpg")))
    (is (= "image/jpeg"            (opf/media-type-for "cover.JPEG")))
    (is (= "text/css"              (opf/media-type-for "style.css")))
    (is (= "font/ttf"              (opf/media-type-for "font.ttf")))
    (is (nil?                      (opf/media-type-for "unknown.xyz")))))

(deftest test-multiple-creators
  (testing "multiple creators are all included with EPUB3 refines syntax"
    (let [opts (assoc-in base-opts [:metadata :creators]
                         [{:name "Alice" :role "aut" :file-as "Alice, A"}
                          {:name "Bob"   :role "aut"}])
          xml  (opf-str opts)]
      ;; Names must appear
      (is (str/includes? xml "Alice"))
      (is (str/includes? xml "Bob"))
      ;; EPUB3: role must be expressed as <meta refines> not as opf:role attribute
      (is (str/includes? xml "property=\"role\"")
          "role must use meta refines, not opf:role attribute")
      (is (str/includes? xml "marc:relators")
          "role scheme must be marc:relators")
      ;; file-as must also use refines
      (is (str/includes? xml "property=\"file-as\"")
          "file-as must use meta refines, not opf:file-as attribute")
      (is (str/includes? xml "Alice, A"))
      ;; Must NOT contain old EPUB2 attribute syntax
      (is (not (str/includes? xml "opf:role"))
          "opf:role attribute must not appear in EPUB3 output")
      (is (not (str/includes? xml "opf:file-as"))
          "opf:file-as attribute must not appear in EPUB3 output"))))

(deftest test-series-metadata
  (testing "series metadata is included when provided"
    (let [opts (-> base-opts
                   (assoc-in [:metadata :series] "My Series")
                   (assoc-in [:metadata :series-position] 2))
          xml  (opf-str opts)]
      (is (str/includes? xml "belongs-to-collection"))
      (is (str/includes? xml "My Series")))))
