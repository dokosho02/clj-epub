(ns clj-epub.ncx
  "Generates the EPUB2-compatible toc.ncx Navigation Control document."
  (:require [clojure.string :as str]))

(defn- xml-esc [s]
  (when s
    (-> (str s)
        (str/replace "&"  "&amp;")
        (str/replace "<"  "&lt;")
        (str/replace ">"  "&gt;")
        (str/replace "\"" "&quot;"))))

(defn- nav-points-str
  "Recursively render <navPoint> elements. play-order is an atom<int>."
  [points play-order depth]
  (str/join "\n"
    (for [{:keys [id label src children]} points
          :when src]
      (let [order (swap! play-order inc)
            pad   (str/join (repeat (* 2 depth) " "))
            pid   (or id (str "np-" order))]
        (str pad "<navPoint id=\"" (xml-esc pid) "\" playOrder=\"" order "\">\n"
             pad "  <navLabel><text>" (xml-esc label) "</text></navLabel>\n"
             pad "  <content src=\"" (xml-esc src) "\"/>\n"
             (when (seq children)
               (str (nav-points-str children play-order (+ depth 1)) "\n"))
             pad "</navPoint>")))))

(defn generate
  "Returns a UTF-8 byte array containing a valid toc.ncx.

  Required options:
    :uid    - must match dc:identifier in the OPF
    :title  - book title
    :points - seq of nav-point maps

  Optional options:
    :depth  - max nesting depth (default: 1)
    :author - book author string

  Nav-point map shape:
    {:id       \"np1\"
     :label    \"Chapter 1\"
     :src      \"Text/ch01.xhtml\"
     :children [{...}]}"
  [{:keys [uid title author points depth]
    :or   {points [] depth 1}}]
  (let [play-order (atom 0)
        nav-str    (nav-points-str points play-order 2)]
    (.getBytes
     (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
          "<!DOCTYPE ncx PUBLIC \"-//NISO//DTD ncx 2005-1//EN\"\n"
          "  \"http://www.daisy.org/z3986/2005/ncx-2005-1.dtd\">\n"
          "<ncx version=\"2005-1\" xmlns=\"http://www.daisy.org/z3986/2005/ncx/\">\n"
          "  <head>\n"
          "    <meta name=\"dtb:uid\" content=\"" (xml-esc uid) "\"/>\n"
          "    <meta name=\"dtb:depth\" content=\"" depth "\"/>\n"
          "    <meta name=\"dtb:totalPageCount\" content=\"0\"/>\n"
          "    <meta name=\"dtb:maxPageNumber\" content=\"0\"/>\n"
          "  </head>\n"
          "  <docTitle><text>" (xml-esc title) "</text></docTitle>\n"
          (when author
            (str "  <docAuthor><text>" (xml-esc author) "</text></docAuthor>\n"))
          "  <navMap>\n"
          nav-str "\n"
          "  </navMap>\n"
          "</ncx>\n")
     "UTF-8")))

;; ---------------------------------------------------------------------------
;; Helper: convert chapters spec → nav-points
;; ---------------------------------------------------------------------------

(defn chapters->nav-points
  "Convert a seq of chapter maps to NCX nav-point maps.

  Chapter map shape:
    {:id      manifest-id
     :title   display-title
     :href    \"Text/ch01.xhtml\"
     :toc     [{:title \"§1\" :href \"Text/ch01.xhtml#s1\"}]}"
  [chapters]
  (letfn [(convert [{:keys [id title href toc]}]
            {:id       id
             :label    title
             :src      href
             :children (map convert (or toc []))})]
    (map convert chapters)))
