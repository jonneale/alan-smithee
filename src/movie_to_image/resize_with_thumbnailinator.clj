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
            [movie-to-image.film :as film]            
            [movie-to-image.image :as image]
            [movie-to-image.util :as util])
  (:gen-class))

(defn- calculate-final-height
  [desired-width scaled-width scaled-height frames-to-capture]
  (let [total-length     (* scaled-width frames-to-capture)
        number-of-lines  (inc (int (/ total-length desired-width)))]
    (* scaled-height number-of-lines)))

(defn tiling
  [film-title film-path frames-to-capture scale-factor desired-final-width]
  (image/with-image-grabber [g (FFmpegFrameGrabber. film-path)]
    (let [[image-width image-height]   (film/frame-dimensions g)
          [scaled-width scaled-height] (image/scale-preserving-aspect-ratio image-width image-height desired-final-width)
          final-height                 (calculate-final-height desired-final-width scaled-width scaled-height frames-to-capture)
          thumbnail-maker              (image/get-thumbnail-maker g scale-factor)
          new-image                    (image/new-image desired-final-width final-height)
          new-image-graphics           (.createGraphics new-image)]
      (doseq [i (range frames-to-capture)]
        (when-let [frame (film/get-next-frame-as-buffered-image g)]
          (let [resized-image       (image/scale-image thumbnail-maker frame)
                [x-offset y-offset] (image/calculate-offset i scaled-width scaled-height desired-final-width)]
            (.drawImage new-image-graphics resized-image x-offset y-offset nil))))
      (.dispose new-image-graphics)
      (image/write-image new-image film-title scale-factor))))


(defn create-tiled-image-from-movie-path
  [[film-title film-path] s width] 
  (let [duration-in-frames (get-film-length film-path)]
    (println duration-in-frames)
    (tiling film-title film-path duration-in-frames s width)))

(defn generate-tiled-images-for-films
  [titles-and-paths]
  (time 
   (doall
    (pmap #(apply create-tiled-image-from-movie-path %)
          (for [f titles-and-paths
                size [5 10 20]]
            [f size 1920])))))

(defn -main
  [& args]
  (generate-tiled-images-for-films args))
