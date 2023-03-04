(ns build
  (:require [clojure.tools.build.api :as b]))

(def class-dir
  "target/classes")

(def basis
  (b/create-basis {:project "deps.edn"
                   :aliases [:clj]}))

(def jar-file
  "target/registratura.jar")

(defn uberjar [_]
  (b/delete {:path "target"})
  (b/copy-dir {:src-dirs ["src/clj" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis basis
                  :src-dirs ["src/clj"]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file jar-file
           :basis basis
           :main 'registratura.core}))
