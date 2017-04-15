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

(defmulti write-tiled-images 
  (fn [frames-to-capture image-grabber scaled-width scaled-height desired-width new-image-graphics thumbnail-maker scaling-method]
    scaling-method))

(defmethod write-tiled-images :java
  [frames-to-capture image-grabber scaled-width scaled-height desired-width new-image-graphics thumbnail-maker _]
  (doseq [i (range frames-to-capture)]
    (when-let [frame (film/get-next-frame-as-buffered-image image-grabber)]
      (let [resized-image       (image/scale-image thumbnail-maker frame)
            [x-offset y-offset] (image/calculate-offset i scaled-width scaled-height desired-width)]
        (.drawImage new-image-graphics resized-image x-offset y-offset nil)))))

(defmethod write-tiled-images :thumbnailinator
  [frames-to-capture image-grabber scaled-width scaled-height desired-width new-image-graphics _ _]
  (doseq [i (range frames-to-capture)]
    (when-let [frame (film/get-next-frame-as-buffered-image image-grabber)]
      (let [[x-offset y-offset] (image/calculate-offset i scaled-width scaled-height desired-width)]
        (.drawImage new-image-graphics frame x-offset y-offset scaled-width scaled-height nil)))))

(defn- create-tiled-image
  [film-title film-path frames-to-capture scaled-width desired-width scaling-method]
  (image/with-image-grabber [g (FFmpegFrameGrabber. film-path)]
    (let [[image-width image-height]   (film/frame-dimensions g)
          scaled-height                (image/scaled-height-preserving-aspect-ratio g desired-width)
          final-height                 (calculate-final-height desired-width scaled-width scaled-height frames-to-capture)
          thumbnail-maker              (image/get-thumbnail-maker image-width image-height scaled-width scaled-height)
          new-image                    (image/new-image desired-width final-height)
          new-image-graphics           (.createGraphics new-image)]
      (write-tiled-images frames-to-capture g scaled-width scaled-height desired-width new-image-graphics thumbnail-maker scaling-method)
      (.dispose new-image-graphics)
      (image/write-image new-image film-title scaled-width))))

(defn create-tiled-image-from-movie-path
  [[film-title film-path] s width scaling-method] 
  (let [duration-in-frames (int (* 0.1 (film/get-film-length film-path)))]
    (create-tiled-image film-title film-path duration-in-frames s width scaling-method)))

(defn time-film-image-generation
  [movie-info]
  (println "Generating image for using Java default scaling")
  (time (create-tiled-image-from-movie-path movie-info 1 640 :java))
  (println "Generating image for using Thumbnailinator scaling")
  (time (create-tiled-image-from-movie-path movie-info 1 640 :thumbnailinator)))

;; (defn generate-tiled-images-for-films
;;   [titles-and-paths]
;;   (time 
;;    (doall
;;     (pmap #(apply create-tiled-image-from-movie-path %)
;;           (for [f titles-and-paths
;;                 size [5 10 20]]
;;             [f size 1920])))))

(defn generate-tiled-images-for-films
  [_]
  (time-film-image-generation skyfall))

(defn -main
  [& args]
  (generate-tiled-images-for-films args))
