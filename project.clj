(defproject notifsta_clone "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj"]

  :test-paths ["test/clj"]

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.122" :scope "provided"]
                 [org.clojure/core.async "0.2.374"]
                 [ring "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [slester/ring-browser-caching "0.1.1"]
                 [bk/ring-gzip "0.1.1"]
                 [compojure "1.4.0"]
                 [enlive "1.1.6"]
                 [org.omcljs/om "0.9.0"]
                 [secretary "1.2.3"]
                 [cljsjs/bootstrap "3.3.5-0"]
                 [cljsjs/bootstrap-timepicker "0.5.1-0"]
                 [cljsjs/d3 "3.5.5-3"]
                 [cljsjs/facebook "v20150729-0"]
                 [cljsjs/google-maps "3.18-1"]
                 [cljsjs/jquery "1.9.1-0"]
                 [cljsjs/jquery-ui "1.11.3-1"]
                 [cljsjs/moment "2.10.6-0"]
                 [cljsjs/plottable "1.12.0-0"]
                 [cljsjs/highcharts "4.1.8-1"]
                 [environ "1.0.0"]]

  :plugins [[lein-cljsbuild "1.0.5"]
            [lein-environ "1.0.0"]]

  :min-lein-version "2.5.0"

  :uberjar-name "notifsta_clone.jar"

  :cljsbuild {:builds {:app {:source-paths ["src/cljs"]
                             :compiler {:output-to     "resources/public/js/app.js"
                                        :output-dir    "resources/public/js/out"
                                        :source-map    "resources/public/js/out.js.map"
                                        :preamble      ["react/react.min.js"]
                                        :optimizations :none
                                        :pretty-print  true}}}}

  :profiles {:dev {:source-paths ["env/dev/clj"]
                   :test-paths ["test/clj"]

                   :dependencies [[figwheel "0.3.9"]
                                  [figwheel-sidecar "0.3.9"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [org.clojure/tools.nrepl "0.2.10"]
                                  [weasel "0.7.0"]]

                   :repl-options {:init-ns notifsta-clone.server
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

                   :plugins [[lein-figwheel "0.3.9"]]

                   :figwheel {:http-server-root "public"
                              :server-port 3449
                              :css-dirs ["resources/public/css"]
                              :ring-handler notifsta-clone.server/http-handler}

                   :env {:is-dev true
                         :browser-caching {"text/javascript" 0
                                           "text/html" 0}}

                   :cljsbuild {:test-commands { "test" ["phantomjs" "env/test/js/unit-test.js" "env/test/unit-test.html"] }
                               :builds {:app {:source-paths ["env/dev/cljs"]}
                                        :test {:source-paths ["src/cljs" "test/cljs"]
                                               :compiler {:output-to     "resources/public/js/app_test.js"
                                                          :output-dir    "resources/public/js/test"
                                                          :source-map    "resources/public/js/test.js.map"
                                                          :preamble      ["react/react.min.js"]
                                                          :optimizations :whitespace
                                                          :pretty-print  false}
                                               :notify-command  ["phantomjs" "bin/speclj" "resources/public/js/app_test.js"]
                                               }}}}

             :uberjar {:source-paths ["env/prod/clj"]
                       :hooks [leiningen.cljsbuild]
                       :env {:production true
                             :browser-caching {"text/javascript" 604800
                                               "text/html" 0}}
                       :omit-source true
                       :aot :all
                       :main notifsta-clone.server
                       :cljsbuild {:builds {:app
                                            {:source-paths ["env/prod/cljs"]
                                             :compiler
                                             {:optimizations :advanced
                                              :pretty-print false}}}}}})
