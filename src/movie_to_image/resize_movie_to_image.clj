(ns movie-to-image.resize-movie-to-image
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
            [movie-to-image.util :as util]
            [clojure.pprint])
  (:gen-class))

(defn- calculate-final-height
  [desired-width scaled-width scaled-height frames-to-capture]
  (let [total-length     (* scaled-width frames-to-capture)
        number-of-lines  (inc (int (/ total-length desired-width)))]
    (* scaled-height number-of-lines)))

(defmulti scale-image (fn [graphics image-to-scale scaled-width scaled-height x-position y-position thumbnail-maker scaling-method]
                        scaling-method))

(defmethod scale-image :java
  [graphics image-to-scale scaled-width scaled-height x-position y-position _ _]
  (.drawImage graphics image-to-scale x-position y-position scaled-width scaled-height nil))

(defmethod scale-image :thumbnailinator
  [graphics image-to-scale scaled-width scaled-height x-position y-position thumbnail-maker _]
  (let [resized-image       (image/scale-image thumbnail-maker image-to-scale)]
    (.drawImage graphics resized-image x-position y-position nil)))

(defn write-tiled-images
  [frames-to-capture image-grabber scaled-width scaled-height desired-width new-image-graphics thumbnail-maker scaling-method]
  (doseq [i (range frames-to-capture)]
    (when-let [frame (film/get-next-frame-as-buffered-image image-grabber)]
      (util/progress-report "film" scaled-width frames-to-capture i)
      (let  [[x-offset y-offset] (image/calculate-offset i scaled-width scaled-height desired-width)]
        (scale-image new-image-graphics frame scaled-width scaled-height x-offset y-offset thumbnail-maker scaling-method)))))

(defn- create-tiled-image
  [film-title film-path frames-to-drop frames-to-capture scaled-width desired-width scaling-method]
  (image/with-image-grabber [g (FFmpegFrameGrabber. film-path)]
    (let [[image-width image-height]   (film/frame-dimensions g)
          scaled-height                (image/scaled-height-preserving-aspect-ratio image-width image-height scaled-width)
          final-height                 (calculate-final-height desired-width scaled-width scaled-height frames-to-capture)
          thumbnail-maker              (image/get-thumbnail-maker image-width image-height scaled-width scaled-height)
          new-image                    (image/new-image desired-width final-height)
          new-image-graphics           (.createGraphics new-image)]
      (doseq [i (range frames-to-drop)] (film/get-next-frame-as-buffered-image g))
      (clojure.pprint/pprint {:scaled-height scaled-height
                              :scaled-width scaled-width
                              :desired-width desired-width
                              :final-height final-height})
      (write-tiled-images frames-to-capture g scaled-width scaled-height desired-width new-image-graphics thumbnail-maker scaling-method)
      (.dispose new-image-graphics)
      (image/write-image new-image film-title scaled-width))))

(defn create-tiled-image-from-movie-path
  ([film-title film-path]
   (create-tiled-image-from-movie-path film-title film-path 5 1920 :thumbnailinator))
  ([film-title film-path s width scaling-method]
   (let [duration-in-frames (film/get-film-length film-path)
         opening-credits-duration-in-frames (* 60 24) 
         closing-credits-duration-in-frames  (* 240 24)
         frames-to-take    (- duration-in-frames opening-credits-duration-in-frames closing-credits-duration-in-frames)]
     (create-tiled-image film-title film-path opening-credits-duration-in-frames frames-to-take s width scaling-method))))

(defn -main
  [& args]
  (apply create-tiled-image-from-movie-path args))
