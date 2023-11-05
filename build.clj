(ns build
  (:require [clojure.string :as string]
            [clojure.tools.build.api :as b]))

(def version
  (let [predefined-version (System/getenv "VERSION")]
    (if (string/blank? predefined-version)
      (format "v0.1.%s" (b/git-count-revs nil))
      predefined-version)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def uber-file (format "target/lijstje-%s-standalone.jar" version))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn uber [_]
  (b/delete {:path "target"})
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis basis
                  :src-dirs ["src"]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis basis
           :main 'lijstje.core}))
