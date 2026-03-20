(ns build
  (:require [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as dd]))

(def lib     'io.github.dokosho02/clj-epub)
(def version "1.0.0")
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

;; AOT 编译目标 — 只编译 interop 层，其余保持 Clojure 源码
(def aot-namespaces '[clj-epub.interop])

;; ---------------------------------------------------------------------------
;; Tasks
;; ---------------------------------------------------------------------------

(defn clean [_]
  (b/delete {:path "target"}))

(defn compile-java
  "AOT-compile the gen-class interop namespace so Java .class files are in jar."
  [_]
  (b/compile-clj {:basis      basis
                  :src-dirs   ["src"]
                  :class-dir  class-dir
                  :ns-compile aot-namespaces}))

(defn jar [_]
  (clean nil)
  (compile-java nil)
  (b/write-pom {:class-dir class-dir
                :lib       lib
                :version   version
                :basis     basis
                :src-dirs  ["src"]
                :pom-data
                [[:description "Clojure EPUB3 generation library with Java interop"]
                 [:url "https://github.com/dokosho02/clj-epub"]
                 [:licenses
                  [:license
                   [:name "Eclipse Public License 2.0"]
                   [:url "https://www.eclipse.org/legal/epl-2.0/"]]]
                 [:developers
                  [:developer
                   [:name "dokosho02"]]]
                 [:scm
                  [:url "https://github.com/dokosho02/clj-epub"]
                  [:connection "scm:git:git://github.com/dokosho02/clj-epub.git"]
                  [:developerConnection "scm:git:ssh://git@github.com/dokosho02/clj-epub.git"]
                  [:tag (str "v" version)]]]})
  (b/copy-dir {:src-dirs  ["src" "resources"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file  jar-file}))

(defn install
  "Install to local ~/.m2 for local testing."
  [_]
  (jar nil)
  (dd/deploy {:installer :local
              :artifact  jar-file
              :pom-file  (b/pom-path {:lib lib :class-dir class-dir})}))

(defn deploy
  "Deploy to Clojars.
   Requires env vars:
     CLOJARS_USERNAME — your Clojars username
     CLOJARS_PASSWORD — your Clojars deploy token (not your login password)"
  [_]
  (jar nil)
  (dd/deploy {:installer  :remote
              :artifact   jar-file
              :pom-file   (b/pom-path {:lib lib :class-dir class-dir})
              :repository {"clojars" {:url      "https://repo.clojars.org"
                                     :username (System/getenv "CLOJARS_USERNAME")
                                     :password (System/getenv "CLOJARS_PASSWORD")}}}))
