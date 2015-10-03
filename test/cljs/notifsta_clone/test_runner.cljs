(ns notifsta-clone.test-runner
  (:require
   [cljs.test :refer-macros [run-tests]]
   [notifsta-clone.core-test]))

(enable-console-print!)

(defn runner []
  (if (cljs.test/successful?
       (run-tests
        'notifsta-clone.core-test))
    0
    1))
