(ns clj-epub.runner
  (:require [cognitect.test-runner.api :as runner]))

(defn -main [& _]
  (runner/test {:dirs ["test"]}))
