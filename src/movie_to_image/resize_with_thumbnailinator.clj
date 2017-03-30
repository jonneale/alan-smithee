(ns movie-to-image.resize-with-thumbnailinator
  (:import [org.bytedeco.javacv FFmpegFrameGrabber OpenCVFrameConverter Java2DFrameConverter Java2DFrameUtils]

           [java.io ByteArrayOutputStream ByteArrayInputStream]
           [java.awt.image BufferedImage]
           [java.awt Color Dimension]
           [javax.imageio ImageIO]
           [net.coobird.thumbnailator Thumbnails]
           [net.coobird.thumbnailator.makers FixedSizeThumbnailMaker]
           [net.coobird.thumbnailator.resizers DefaultResizerFactory])
  (:require [clojure.java.io :as io]
            [movie-to-image.films :refer :all]
            [movie-to-image.image :as image]
            [movie-to-image.util :as util])
  (:gen-class))

(defn get-next-frame-as-buffered-image
  [frame-grabber]
  (loop [i 0]
    (if (> i 1000) (do (println "No more frames!") nil)
        (let [f (. frame-grabber grabImage)
              c (Java2DFrameConverter.)
              image (. c getBufferedImage f)]
          (if image 
            image
            (recur (inc i)))))))

(defn get-film-length
  [film-path]
  (image/with-image-grabber [g (FFmpegFrameGrabber. film-path)]
    (. g getLengthInFrames)))

(defn calculate-offset
  [i scaled-width scaled-height desired-width]
  [(mod (* i scaled-width) desired-width) (int (* scaled-height (/ (* i scaled-width) desired-width)))])

(defn frame-dimensions
  [grabber]
  [(.getImageWidth grabber) (.getImageHeight grabber)])

(defn scale-preserving-aspect-ratio
  [image-width image-height scaled-width]
  [scaled-width
   (inc (int (* (/ scaled-width image-width) image-height)))])

(defn tiling
  [film-title film-path frames-to-capture scale-factor desired-final-width]
  (let [])
  (image/with-image-grabber [g (FFmpegFrameGrabber. film-path)]
    (let [[image-width image-height] (frame-dimensions g)
          [scaled-width scaled-height] (scale-preserving-aspect-ratio image-width image-height scale-factor)
          thumbnail-maker (image/get-thumbnail-maker image-width image-height scaled-width scaled-height)
          final-height    (inc (int (* (/ (* frames-to-capture scaled-width) desired-final-width) scaled-height)))
          new-image       (image/new-image desired-final-width
                                           final-height)
          new-image-graphics (. new-image createGraphics)]
      (println "width - " desired-final-width " height - " final-height)
      (doseq [i (range frames-to-capture)]
        (util/progress-report film-title scale-factor frames-to-capture i)
        (when-let [frame               (get-next-frame-as-buffered-image g)]
          (let [resized-image          (image/scale-image thumbnail-maker frame)
                [x-offset y-offset] (calculate-offset i scaled-width scaled-height desired-final-width)]
            (. new-image-graphics drawImage resized-image x-offset y-offset nil))))
      (println film-title " scaled to " scale-factor " is complete ")
      (. new-image-graphics dispose)
      (image/write-image new-image film-title scale-factor)
      (println film-title " scaled to " scale-factor " processed"))))


(defn do-it
  [[film-title film-path] s width] 
  (let [duration-in-frames (get-film-length film-path)]
    (println duration-in-frames)
    (tiling film-title film-path duration-in-frames s width)))

(defn generate-for-films
  []
  (time 
   (doall
    (pmap #(apply do-it %)
          (for [f [skyfall]
                size [5 10 20]]
            [f size 1920])))))

(defn -main
  [& args]
  (generate-for-films))
