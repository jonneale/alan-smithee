(ns movie-to-image.util)

(defn now [] (str (java.time.LocalDateTime/now)))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn progress-report
  [film scale-factor total current]
  (when (zero? (mod current 500))
    (println (format "%s: %s scaled to %dx%d is %f percent complete" (now) film scale-factor scale-factor (double (* 100 (/ current (double total))))))))
