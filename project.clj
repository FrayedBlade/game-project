(defproject game-project "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main ^:skip-aot game-project.core
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [quil "4.3.1323"]
                 [com.github.seancorfield/next.jdbc "1.3.939"]
                 [org.xerial/sqlite-jdbc "3.46.1.0"]]
  :plugins [[cider/cider-nrepl "0.49.0"]])