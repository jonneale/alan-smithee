(ns movie-to-image.core
  (:import [org.bytedeco.javacv FFmpegFrameGrabber OpenCVFrameConverter Java2DFrameConverter Java2DFrameUtils]

           [java.io ByteArrayOutputStream ByteArrayInputStream]
           [java.awt.image BufferedImage]
           [java.awt Color]
           [javax.imageio ImageIO]
           [net.coobird.thumbnailator Thumbnails])
  (:require [clojure.java.io :as io]
            [movie-to-image.films :refer :all])
  (:gen-class))


(def films [open-range 
            high-rise 
            the-royal-tenenbaums 
            magnolia])

(def more-films
  [bad-day-at-black-rock
   nine-to-five
   locke
   lone-star])

(defn buffered-image
  [path]
  (let [g (FFmpegFrameGrabber. path)
        c (Java2DFrameConverter.)]
    (. g start)
    (. c getBufferedImage (. g grab))))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn write-image
  [image film-title scale-factor]
  (let [output-file (io/file (str "/Users/jon.neale/scratch/movie-outputs/" film-title "-" scale-factor "-" (uuid) ".jpg"))]
    (ImageIO/write image "png" output-file)))

(defn get-frame
  [p]
  (let [g (FFmpegFrameGrabber. p)]
    (. g start)
    (println "Frame Rate - " (. g getFrameRate))
    (let [f (. g grab)
          c (Java2DFrameConverter.)
          i (. c getBufferedImage f)]
      (. g stop)
      i)))

(defn get-next-frame-as-buffered-image
  [frame-grabber]
  (loop [i 0]
    (if (> i 1000) (do (println "No more frames!") nil)
        (let [f (. frame-grabber grab)
              c (Java2DFrameConverter.)
              image (. c getBufferedImage f)]
          (if image 
            image
            (recur (inc i)))))))

(defn calculate-offset
  [i scale-factor desired-width]
  [(mod (* i scale-factor) desired-width) (int (/ (* i scale-factor) desired-width))])

(defn now [] (str (java.time.LocalDateTime/now)))

(defn progress-report
  [film scale-factor total current]
  (when (zero? (mod current 500))
    (println (format "%s: %s scaled to %dx%d is %f percent complete" (now) film scale-factor scale-factor (double (* 100 (/ current (double total))))))))


(defn get-frame
  [p]
  (let [g (FFmpegFrameGrabber. p)]
    (. g start)
    (println "Frame Rate - " (. g getFrameRate))
    (let [f (. g grab)
          c (Java2DFrameConverter.)
          i (. c getBufferedImage f)]
      (. g stop)
      i)))

(defn get-film-length
  [film-path]
  (let [g          (FFmpegFrameGrabber. film-path)
        _          (. g start)
        frames     (. g getLengthInFrames)
        _          (. g stop)]
    frames))

(defn get-thumbnail
  [buffered-image width height]
  (. (. (Thumbnails/of (into-array BufferedImage [buffered-image]))
        size width height)
     asBufferedImage))

(defn tiling
  [film-title film-path frames-to-capture scale-factor desired-width]
  (let [g (FFmpegFrameGrabber. film-path)
        new-image (BufferedImage. desired-width
                                  (inc (int (/ (* frames-to-capture scale-factor) desired-width)))
                                  BufferedImage/TYPE_INT_RGB)
        new-image-graphics (. new-image createGraphics)]
    (println "width - " desired-width " height - " (inc (int (/ (* frames-to-capture scale-factor) desired-width))))
    (. g start)
    (doseq [i (range frames-to-capture)]
      (progress-report film-title scale-factor frames-to-capture i)
      (when-let [frame               (get-next-frame-as-buffered-image g)]
        (let [[x-offset y-offset] (calculate-offset i scale-factor desired-width)]
          (. new-image-graphics drawImage frame x-offset y-offset scale-factor scale-factor nil))))
    (println film-title " scaled to " scale-factor " is complete ")
    (. new-image-graphics dispose)
    (write-image new-image film-title scale-factor)
    (println film-title " scaled to " scale-factor " processed")
    (. g stop)))

(defn do-it
  [[film-title film-path] s width] 
  (let [duration-in-frames (get-film-length film-path)]
    (println duration-in-frames)
    (tiling film-title film-path duration-in-frames s width)))

(defn generate
  [film]
  (time
   (doall (pmap (partial do-it film) [5 10 20]))))

(defn generate-for-films
  []
  (time
   (doall
    (pmap #(apply do-it %)
          (for [f harry-potter-films]
            [f 5 1280])))))

(defn -main
  [& args]
  (generate-for-films))
