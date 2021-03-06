(defproject dbas-fb-hook "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [org.clojure/tools.logging "0.4.0"]
                 [compojure "1.6.0"]
                 [ring/ring-defaults "0.3.1"]
                 [ring/ring-json "0.4.0"]
                 [ring-logger "0.7.7"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-http "3.7.0"]
                 [clj-fuzzy "0.4.0"]
                 [org.clojure/core.async "0.4.474"]]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler dbas-fb-hook.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}})
