(ns clj-epub.interop
  "Java interop layer — exposes clj-epub as a proper Java API.

  Java/Kotlin/Scala developers use EpubBuilder instead of calling
  Clojure functions directly.

  Usage from Java:
    import org.clojars.dokosho02.EpubBuilder;

    byte[] bytes = new EpubBuilder()
        .title(\"My Book\")
        .language(\"en\")
        .author(\"John Doe\")
        .addChapter(\"ch01\", \"Chapter 1\", \"Text/ch01.xhtml\", \"<h1>Hello</h1>\")
        .toBytes();
  "
  (:require [clj-epub.core :as epub])
  (:gen-class
   :name    org.clojars.dokosho02.EpubBuilder
   :state   state
   :init    init
   :prefix  "builder-"
   :methods [;; Return Object to avoid AOT self-referential circular dependency.
             ;; Java/Kotlin callers can cast as needed, or just call methods directly.
             [title        [String]  Object]
             [language     [String]  Object]
             [author       [String]  Object]
             [publisher    [String]  Object]
             [description  [String]  Object]
             [addChapter   [String String String String]  Object]
             [css          [String]  Object]
             [toBytes      []        bytes]
             [writeToFile  [String]  void]]))

(defn builder-init [] [[] (atom {:metadata {} :chapters []})])

(defn- swap-meta! [this k v]
  (when (seq v)
    (swap! (.state this) update :metadata assoc k v))
  this)

(defn builder-title       [this v] (swap-meta! this :title v))
(defn builder-language    [this v] (swap-meta! this :language v))
(defn builder-author      [this v] (swap-meta! this :author v))
(defn builder-publisher   [this v] (swap-meta! this :publisher v))
(defn builder-description [this v] (swap-meta! this :description v))

(defn builder-css [this css-string]
  (swap! (.state this) assoc :css css-string)
  this)

(defn builder-addChapter [this id title href content]
  (swap! (.state this) update :chapters conj
         {:id id :title title :href href :content content})
  this)

(defn builder-toBytes [this]
  (epub/make-epub-bytes @(.state this)))

(defn builder-writeToFile [this path]
  (epub/make-epub! @(.state this) path))
