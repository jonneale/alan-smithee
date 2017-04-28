(ns alan-smithee.resize-alan-smithee
  (:import [org.bytedeco.javacv FFmpegFrameGrabber OpenCVFrameConverter Java2DFrameConverter Java2DFrameUtils]
           [java.io ByteArrayOutputStream ByteArrayInputStream]
           [java.awt.image BufferedImage]
           [java.awt Color Dimension]
           [javax.imageio ImageIO]
           [net.coobird.thumbnailator Thumbnails]
           [net.coobird.thumbnailator.makers FixedSizeThumbnailMaker]
           [net.coobird.thumbnailator.resizers DefaultResizerFactory])
  (:require [clojure.java.io :as io]
            [alan-smithee.films :refer :all]
            [alan-smithee.film :as film]            
            [alan-smithee.image :as image]
            [alan-smithee.util :as util]
            [clojure.pprint])
  (:gen-class))

(defn- calculate-final-height
  [desired-width scaled-width scaled-height frames-to-capture]
  (let [total-length     (* scaled-width frames-to-capture)
        number-of-lines  (inc (int (/ total-length desired-width)))]
    (* scaled-height number-of-lines)))

(defn scale-image
  [graphics image-to-scale scaled-width scaled-height x-position y-position thumbnail-maker]
  (let [resized-image       (image/scale thumbnail-maker image-to-scale)]
    (.drawImage graphics resized-image x-position y-position nil)))

(defn write-tiled-images
  [frames-to-capture image-grabber scaled-width scaled-height desired-width new-image-graphics thumbnail-maker]
  (doseq [i (range frames-to-capture)]
    (when-let [frame (film/get-next-frame-as-buffered-image image-grabber)]
      (let  [[x-offset y-offset] (image/calculate-offset i scaled-width scaled-height desired-width)]
        (scale-image new-image-graphics frame scaled-width scaled-height x-offset y-offset thumbnail-maker)))))

(defn- create-tiled-image
  [film-title film-path frames-to-capture scaled-width desired-width]
  (image/with-image-grabber [g (FFmpegFrameGrabber. film-path)]
    (let [[image-width image-height]   (film/frame-dimensions g)
          scaled-height                (image/scaled-height-preserving-aspect-ratio image-width image-height scaled-width)
          final-height                 (calculate-final-height desired-width scaled-width scaled-height frames-to-capture)
          thumbnail-maker              (image/get-thumbnail-maker image-width image-height scaled-width scaled-height)
          new-image                    (image/new-image desired-width final-height)
          new-image-graphics           (.createGraphics new-image)]
      (write-tiled-images frames-to-capture g scaled-width scaled-height desired-width new-image-graphics thumbnail-maker)
      (.dispose new-image-graphics)
      (image/write-image new-image film-title scaled-width))))

(defn do-it
  []
  (create-tiled-image (first ghost-in-the-shell) (last ghost-in-the-shell) (film/get-film-length (last ghost-in-the-shell)) 5 1920))

(defn -main
  [& args]
  (apply create-tiled-image-from-movie-path args))