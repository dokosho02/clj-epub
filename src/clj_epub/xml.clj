(ns clj-epub.xml
  "Thin wrappers around clojure.data.xml for serialising EPUB XML documents.

  We use a simple hiccup-style vector notation internally:
    [tag attrs? & children]

  This is converted to clojure.data.xml Element records before serialisation."
  (:require [clojure.data.xml :as xml]
            ;; [clojure.string   :as str]
            )
  (:import [java.io StringWriter]))

;; ---------------------------------------------------------------------------
;; Namespace aliases used across EPUB documents
;; ---------------------------------------------------------------------------

(def ns-aliases-xml
  {"dc"   "http://purl.org/dc/elements/1.1/"
   "opf"  "http://www.idpf.org/2007/opf"
   "epub" "http://www.idpf.org/2007/ops"
   "ncx"  "http://www.daisy.org/z3986/2005/ncx/"
   "xhtml" "http://www.w3.org/1999/xhtml"
   "ocf"  "urn:oasis:names:tc:opendocument:xmlns:container"})

(defn ns-uri [prefix] (get ns-aliases-xml prefix))

;; ---------------------------------------------------------------------------
;; Hiccup → data.xml Element
;; ---------------------------------------------------------------------------

(declare hiccup->element)

(defn- parse-tag
  "Given a keyword like :opf/meta or :dc/title, return [ns-uri local-name]."
  [kw]
  (let [n (name kw)
        ns (namespace kw)]
    (if ns
      [(ns-uri ns) n]
      [nil n])))

(defn hiccup->element
  "Convert a hiccup-style vector to a clojure.data.xml Element.
   Supports:
     [tag children...]
     [tag attrs children...]
   where attrs is a map. Children may be strings or nested vectors."
  [[tag & rest]]
  (let [[attrs children]
        (if (and (seq rest) (map? (first rest)))
          [(first rest) (next rest)]
          [{} rest])
        [ns-uri local] (parse-tag tag)
        ;; Qualify attribute keys the same way
        qattrs (into {}
                     (map (fn [[k v]]
                            (let [[a-ns a-loc] (parse-tag k)
                                  qname (if a-ns
                                          (xml/qname a-ns a-loc)
                                          (keyword a-loc))]
                              [qname v]))
                          attrs))
        qname  (if ns-uri (xml/qname ns-uri local) (keyword local))
        kids   (keep (fn [c]
                       (cond
                         (string? c)  c
                         (vector? c)  (hiccup->element c)
                         (nil? c)     nil
                         :else        (str c)))
                     children)]
    (apply xml/element qname qattrs kids)))

;; ---------------------------------------------------------------------------
;; Serialisation helpers
;; ---------------------------------------------------------------------------

;; (def ^:private xml-declaration
;;   "<?xml version=\"1.0\" encoding=\"UTF-8\"?>")

(defn emit-str
  "Serialise a data.xml Element to a UTF-8 string, with XML declaration."
  [element]
  (let [sw (StringWriter.)]
    (xml/emit element sw)
    (.toString sw)))

(defn emit-bytes
  "Serialise a data.xml Element to a UTF-8 byte array."
  ^bytes [element]
  (let [sw (StringWriter.)]
    (xml/emit element sw)
    (.getBytes (.toString sw) "UTF-8")))

(defn hiccup->bytes
  "Convert a hiccup vector directly to UTF-8 bytes."
  ^bytes [h]
  (emit-bytes (hiccup->element h)))

(defn hiccup->str
  "Convert a hiccup vector directly to a String."
  [h]
  (emit-str (hiccup->element h)))
