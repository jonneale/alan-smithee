(defproject movie-to-image "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.bytedeco/javacv-platform "1.3.2"]]
  :main ^:skip-aot movie-to-image.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
