(ns movie-to-image.film
  (:import [org.bytedeco.javacv FFmpegFrameGrabber OpenCVFrameConverter Java2DFrameConverter Java2DFrameUtils])
  (:require [movie-to-image.image :as image]))

(defn frame-dimensions
  [grabber]
  [(.getImageWidth grabber) (.getImageHeight grabber)])

(defn get-next-frame-as-buffered-image
  [frame-grabber]
  (loop [i 0]
    (if (> i 1000) (do (println "No more frames!") nil)
        (let [f (.grabImage frame-grabber)
              c (Java2DFrameConverter.)
              image (.getBufferedImage c f)]
          (if image 
            image
            (recur (inc i)))))))

(defn get-film-length
  [film-path]
  (image/with-image-grabber [g (FFmpegFrameGrabber. film-path)]
    (.getLengthInFrames g)))

(defn calculate-offset
  [i scaled-width scaled-height desired-width]
  [(mod (* i scaled-width) desired-width) (int (* scaled-height (/ (* i scaled-width) desired-width)))])
