(ns clj-epub.core-test
  "Integration tests for clj-epub.core/make-epub.

  These tests verify the complete EPUB generation pipeline:
  - Correct ZIP structure (mimetype first, STORED)
  - Mandatory files present (container.xml, OPF, nav.xhtml)
  - Content correctness (metadata appears, chapters appear)
  - Edge cases (custom CSS, pre-rendered bytes, no NCX)"
  (:require [clojure.test :refer [deftest is testing]]
            [clj-epub.core :as epub]
            [clj-epub.test-helpers :as h]
            [clojure.string :as str])
  (:import [java.util.zip ZipEntry]))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn- build [opts]
  (epub/make-epub-bytes opts))

;; ---------------------------------------------------------------------------
;; Minimal spec tests
;; ---------------------------------------------------------------------------

(deftest test-minimal-epub-builds
  (testing "make-epub-bytes succeeds with minimal options"
    (let [epub (build h/minimal-opts)]
      (is (bytes? epub))
      (is (pos? (alength epub))))))

(deftest test-make-epub-returns-entries
  (testing "make-epub returns a seq of entry maps"
    (let [entries (epub/make-epub h/minimal-opts)]
      (is (seq entries))
      (is (every? #(and (:path %) (:data %)) entries)))))

(deftest test-make-epub-bang-writes-file
  (testing "make-epub! writes file and returns path"
    (let [tmp (java.io.File/createTempFile "clj-epub-test" ".epub")]
      (.deleteOnExit tmp)
      (let [result (epub/make-epub! h/minimal-opts (.getAbsolutePath tmp))]
        (is (= (.getAbsolutePath tmp) result))
        (is (.exists tmp))
        (is (pos? (.length tmp)))))))

;; ---------------------------------------------------------------------------
;; ZIP structure tests
;; ---------------------------------------------------------------------------

(deftest test-mimetype-first-and-stored
  (testing "mimetype is first entry and STORED"
    (let [epub  (build h/minimal-opts)
          names (h/entry-names epub)]
      (is (= "mimetype" (first names)) "mimetype must be first ZIP entry")
      (is (= ZipEntry/STORED (h/entry-method epub "mimetype"))
          "mimetype must be STORED (not compressed)"))))

(deftest test-mandatory-files-present
  (testing "mandatory EPUB files are all present in the archive"
    (let [epub    (build h/minimal-opts)
          entries (h/unzip-entries epub)]
      (is (contains? entries "mimetype"))
      (is (contains? entries "META-INF/container.xml"))
      (is (contains? entries "OEBPS/content.opf"))
      (is (contains? entries "OEBPS/nav.xhtml"))
      (is (contains? entries "OEBPS/toc.ncx")      "EPUB2 NCX should be included by default"))))

(deftest test-chapter-file-present
  (testing "chapter XHTML files appear in the archive"
    (let [epub    (build h/minimal-opts)
          entries (h/unzip-entries epub)]
      (is (contains? entries "OEBPS/Text/ch01.xhtml")))))

(deftest test-default-stylesheet-present
  (testing "default CSS stylesheet is included"
    (let [epub    (build h/minimal-opts)
          entries (h/unzip-entries epub)]
      (is (contains? entries "OEBPS/Styles/stylesheet.css")))))

;; ---------------------------------------------------------------------------
;; Content correctness
;; ---------------------------------------------------------------------------

(deftest test-container-points-to-opf
  (testing "container.xml references content.opf"
    (let [epub    (build h/minimal-opts)
          entries (h/unzip-entries epub)
          xml     (h/entry-str entries "META-INF/container.xml")]
      (is (str/includes? xml "OEBPS/content.opf")))))

(deftest test-metadata-in-opf
  (testing "book title and language appear in OPF"
    (let [epub    (build h/minimal-opts)
          entries (h/unzip-entries epub)
          opf     (h/entry-str entries "OEBPS/content.opf")]
      (is (str/includes? opf "Test Book"))
      (is (str/includes? opf "<dc:language"))
      (is (str/includes? opf ">en<")))))

(deftest test-chapter-in-manifest
  (testing "chapter is referenced in OPF manifest"
    (let [epub    (build h/minimal-opts)
          entries (h/unzip-entries epub)
          opf     (h/entry-str entries "OEBPS/content.opf")]
      (is (str/includes? opf "ch01"))
      (is (str/includes? opf "Text/ch01.xhtml")))))

(deftest test-chapter-content-written
  (testing "chapter content is correctly written to XHTML file"
    (let [epub    (build h/minimal-opts)
          entries (h/unzip-entries epub)
          chapter (h/entry-str entries "OEBPS/Text/ch01.xhtml")]
      (is (str/includes? chapter "Chapter One"))
      (is (str/includes? chapter "Hello, world!")))))

(deftest test-nav-contains-chapters
  (testing "nav.xhtml contains TOC entries for all chapters"
    (let [epub    (build h/minimal-opts)
          entries (h/unzip-entries epub)
          nav     (h/entry-str entries "OEBPS/nav.xhtml")]
      (is (str/includes? nav "Chapter One"))
      (is (str/includes? nav "Text/ch01.xhtml")))))

(deftest test-ncx-contains-uid
  (testing "toc.ncx contains the book identifier"
    (let [epub    (build h/minimal-opts)
          entries (h/unzip-entries epub)
          ncx     (h/entry-str entries "OEBPS/toc.ncx")]
      (is (str/includes? ncx "urn:uuid:test-0001")))))

;; ---------------------------------------------------------------------------
;; Full options test
;; ---------------------------------------------------------------------------

(deftest test-full-opts-epub
  (testing "full-opts build produces all expected files"
    (let [epub    (build h/full-opts)
          entries (h/unzip-entries epub)]
      ;; Structure
      (is (contains? entries "META-INF/container.xml"))
      (is (contains? entries "OEBPS/content.opf"))
      (is (contains? entries "OEBPS/nav.xhtml"))
      (is (contains? entries "OEBPS/toc.ncx"))
      ;; Chapters
      (is (contains? entries "OEBPS/Text/cover.xhtml"))
      (is (contains? entries "OEBPS/Text/titlepage.xhtml"))
      (is (contains? entries "OEBPS/Text/ch01.xhtml"))
      (is (contains? entries "OEBPS/Text/ch02.xhtml"))
      ;; Image resource
      (is (contains? entries "OEBPS/Images/cover.png")))))

(deftest test-full-opts-metadata
  (testing "full-opts metadata appears in OPF"
    (let [epub    (build h/full-opts)
          entries (h/unzip-entries epub)
          opf     (h/entry-str entries "OEBPS/content.opf")]
      (is (str/includes? opf "Full Test Book"))
      (is (str/includes? opf "张三"))
      (is (str/includes? opf "Test Press"))
      (is (str/includes? opf "belongs-to-collection"))
      (is (str/includes? opf "Test Series")))))

(deftest test-cover-linear-no
  (testing "cover chapter has linear=no in spine regardless of its :id value"
    ;; full-opts uses :id "cover-page" (not "cover") — library must detect
    ;; the cover by linear=false, not by id name convention
    (let [epub    (build h/full-opts)
          entries (h/unzip-entries epub)
          opf     (h/entry-str entries "OEBPS/content.opf")]
      (is (str/includes? opf "linear=\"no\"")))))

;; ---------------------------------------------------------------------------
;; Custom CSS test
;; ---------------------------------------------------------------------------

(deftest test-custom-css
  (testing "custom CSS string replaces default stylesheet"
    (let [epub    (build (assoc h/minimal-opts :css "body { color: red; }"))
          entries (h/unzip-entries epub)
          css     (h/entry-str entries "OEBPS/Styles/stylesheet.css")]
      (is (str/includes? css "color: red")))))

(deftest test-no-css-when-false
  (testing "passing :css false omits stylesheet from archive"
    (let [epub    (build (assoc h/minimal-opts :css false))
          entries (h/unzip-entries epub)]
      (is (not (contains? entries "OEBPS/Styles/stylesheet.css"))))))

;; ---------------------------------------------------------------------------
;; Pre-rendered bytes test
;; ---------------------------------------------------------------------------

(deftest test-pre-rendered-chapter
  (testing ":data bytes are used as-is for chapter content"
    (let [raw-xhtml "<?xml version=\"1.0\"?><html xmlns=\"http://www.w3.org/1999/xhtml\"><body><p>Pre-rendered</p></body></html>"
          raw-bytes (.getBytes raw-xhtml "UTF-8")
          opts      (assoc-in h/minimal-opts [:chapters 0 :data] raw-bytes)
          opts      (update-in opts [:chapters 0] dissoc :content)
          epub      (build opts)
          entries   (h/unzip-entries epub)
          chapter   (h/entry-str entries "OEBPS/Text/ch01.xhtml")]
      (is (str/includes? chapter "Pre-rendered")))))

;; ---------------------------------------------------------------------------
;; EPUB2-compat disabled
;; ---------------------------------------------------------------------------

(deftest test-no-ncx-when-epub2-compat-false
  (testing "toc.ncx is omitted when epub2-compat is false"
    (let [opts    (assoc h/minimal-opts :options {:epub2-compat false})
          epub    (build opts)
          entries (h/unzip-entries epub)]
      (is (not (contains? entries "OEBPS/toc.ncx"))))))

;; ---------------------------------------------------------------------------
;; Auto UUID generation
;; ---------------------------------------------------------------------------

(deftest test-auto-uuid-generated
  (testing "UUID is auto-generated when :identifier is omitted"
    (let [opts    (update h/minimal-opts :metadata dissoc :identifier)
          epub    (build opts)
          entries (h/unzip-entries epub)
          opf     (h/entry-str entries "OEBPS/content.opf")]
      (is (str/includes? opf "urn:uuid:")))))

;; ---------------------------------------------------------------------------
;; Fragment stripping test
;; ---------------------------------------------------------------------------

(deftest test-toc-fragments-stripped
  (testing "fragment identifiers in :toc hrefs are stripped from nav.xhtml and toc.ncx"
    ;; full-opts has ch01 with :toc [{:href "Text/ch01.xhtml#s1"}]
    ;; The library must strip #s1 so epubcheck RSC-012 does not fire
    ;; when the rendered XHTML does not contain id="s1"
    (let [epub    (build h/full-opts)
          entries (h/unzip-entries epub)
          nav     (h/entry-str entries "OEBPS/nav.xhtml")
          ncx     (h/entry-str entries "OEBPS/toc.ncx")]
      (is (not (str/includes? nav "#s1"))
          "nav.xhtml must not contain bare fragment #s1")
      (is (not (str/includes? ncx "#s1"))
          "toc.ncx must not contain bare fragment #s1")
      ;; The chapter href without fragment must still be present
      (is (str/includes? nav "Text/ch01.xhtml"))
      (is (str/includes? ncx "Text/ch01.xhtml")))))

;; ---------------------------------------------------------------------------
;; Cover detection by linear=false (not by id name)
;; ---------------------------------------------------------------------------

(deftest test-cover-in-guide-by-linear
  (testing "first linear=false chapter appears as cover in OPF guide regardless of id"
    ;; full-opts uses :id "cover-page", not "cover"
    ;; library must still put it in the guide as type="cover"
    (let [epub    (build h/full-opts)
          entries (h/unzip-entries epub)
          opf     (h/entry-str entries "OEBPS/content.opf")]
      (is (str/includes? opf "type=\"cover\""))
      (is (str/includes? opf "Text/cover.xhtml")))))

;; ---------------------------------------------------------------------------
;; Validation: missing required fields
;; ---------------------------------------------------------------------------

(deftest test-missing-title-throws
  (testing "make-epub throws when :title is missing"
    (is (thrown? AssertionError
                 (epub/make-epub {:metadata {:language "en"}
                                  :chapters []})))))

(deftest test-missing-language-throws
  (testing "make-epub throws when :language is missing"
    (is (thrown? AssertionError
                 (epub/make-epub {:metadata {:title "Book"}
                                  :chapters []})))))
