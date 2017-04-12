(ns movie-to-image.core
  (:import [org.bytedeco.javacv FFmpegFrameGrabber OpenCVFrameConverter Java2DFrameConverter Java2DFrameUtils]
           [java.io ByteArrayOutputStream ByteArrayInputStream]
           [java.awt.image BufferedImage]
           [java.awt Color]
           [javax.imageio ImageIO]
           [net.coobird.thumbnailator Thumbnails])
  (:require [clojure.java.io :as io]
            [movie-to-image.image :as image]
            [movie-to-image.film  :as film]
            [movie-to-image.films :refer :all])
  (:gen-class))

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
        _          (.start g)
        frames     (.getLengthInFrames g)
        _          (. g stop)]
    frames))

(defn get-thumbnail
  [buffered-image width height]
  (.asBufferedImage 
   (.size 
    (Thumbnails/of (into-array BufferedImage [buffered-image]))
    width height)))

(defn- calculate-final-height
  [desired-width scale-factor frames-to-capture]
  (let [total-length     (* scale-factor frames-to-capture)
        number-of-lines  (inc (int (/ total-length desired-width)))]
    (* scale-factor number-of-lines)))

(defn create-tiled-image
  [film-title film-path frames-to-capture scale-factor desired-width]
  (image/with-image-grabber [g (FFmpegFrameGrabber. film-path)]
    (let [[image-width image-height]   (film/frame-dimensions g)
          [scaled-width scaled-height] (image/scale-preserving-aspect-ratio image-width image-height desired-width)
          final-height    (calculate-final-height desired-width scale-factor frames-to-capture)
          new-image       (image/new-image desired-width final-height)
          new-image-graphics (.createGraphics new-image)]
      (doseq [i (range frames-to-capture)]
        (when-let [frame               (get-next-frame-as-buffered-image g)]
          (let [[x-offset y-offset] (calculate-offset i scale-factor desired-width)]
            (.drawImage new-image-graphics frame x-offset y-offset scale-factor scale-factor nil))))
      (.dispose new-image-graphics)
      (write-image new-image film-title scale-factor))))

(defn create-tiled-image-from-movie-path
  [[film-title film-path] s width] 
  (let [duration-in-frames (get-film-length film-path)]
    (println duration-in-frames)
    (create-tiled-image film-title film-path duration-in-frames s width)))

(defn generate
  [film]
  (time
   (doall (pmap (partial create-tiled-image-from-movie-path film) [5 10 20]))))

(defn generate-for-films
  [films]
  (time
   (doall
    (pmap #(apply create-tiled-image-from-movie-path %)
          (for [f films]
            [f 5 1920])))))

(defn -main
  [& args]
  (generate-for-films))
