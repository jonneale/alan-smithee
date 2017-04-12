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
            [movie-to-image.util  :as util]
            [movie-to-image.films :refer :all])
  (:gen-class))

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
          final-height                 (calculate-final-height desired-width scale-factor frames-to-capture)
          new-image                    (image/new-image desired-width final-height)
          new-image-graphics           (.createGraphics new-image)]
      (doseq [i (range frames-to-capture)]
        (when-let [frame (film/get-next-frame-as-buffered-image g)]
          (let [[x-offset y-offset] (image/calculate-offset i scale-factor desired-width)]
            (.drawImage new-image-graphics frame x-offset y-offset scale-factor scale-factor nil))))
      (.dispose new-image-graphics)
      (image/write-image new-image film-title scale-factor))))

(defn create-tiled-image-from-movie-path
  [[film-title film-path] s width] 
  (let [duration-in-frames (film/get-film-length film-path)]
    (println duration-in-frames)
    (create-tiled-image film-title film-path duration-in-frames s width)))

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
