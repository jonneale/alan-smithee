(defproject alan-smithee "0.1.0-SNAPSHOT"
  :description "Tools to visualise colours in mpegs"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.bytedeco/javacv-platform "1.3.2"]
                 [net.coobird/thumbnailator "0.4.8"]]
  :main ^:skip-aot alan-smithee.resize-alan-smithee
  :target-path "target/%s"
  :jvm-opts ["-Xmx6G"]
  :profiles {:uberjar {:aot :all}})
