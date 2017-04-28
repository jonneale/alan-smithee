(ns alan-smithee.image
  (:import [org.bytedeco.javacv FFmpegFrameGrabber OpenCVFrameConverter Java2DFrameConverter Java2DFrameUtils]

           [java.io ByteArrayOutputStream ByteArrayInputStream]
           [java.awt.image BufferedImage]
           [java.awt Color Dimension]
           [javax.imageio ImageIO]
           [net.coobird.thumbnailator Thumbnails]
           [net.coobird.thumbnailator.makers FixedSizeThumbnailMaker]
           [net.coobird.thumbnailator.resizers DefaultResizerFactory])
  (:require [clojure.java.io :as io]
            [alan-smithee.util :as util])
  (:gen-class))

(defn calculate-offset
  [i scaled-width scaled-height desired-width]
  [(mod (* i scaled-width) desired-width) (* scaled-height (int (/ (* i scaled-width) desired-width)))])

(defmacro with-image-grabber
  [grabber-binding & body]
  `(let ~(subvec grabber-binding 0 2)
     (do (. ~(grabber-binding 0) start)
         (let [result# ~@body]
           (. ~(grabber-binding 0) stop)
           result#))))

(defn calculate-offset
  [i scaled-width scaled-height desired-width]
  [(mod (* i scaled-width) desired-width) (* scaled-height (int (/ (* i scaled-width) desired-width)))])

(defn dimension
  [width height]
  (Dimension. width height))

(defn- get-resizer
  [image-width image-height intended-width intended-height]
  (.getResizer (DefaultResizerFactory/getInstance)
               (dimension image-width image-height) (dimension intended-width intended-height)))

(defn scaled-height-preserving-aspect-ratio
  [image-width image-height scaled-width]
  (inc (int (* (/ scaled-width image-width) image-height))))

(defn get-thumbnail-maker
  [image-width image-height intended-width intended-height]
  (.resizer (FixedSizeThumbnailMaker. intended-width intended-height false true)
            (get-resizer image-width image-height intended-width intended-height)))

(defn scale
  [thumbnail-maker buffered-image-to-scale]
  (.make thumbnail-maker buffered-image-to-scale))

(defn write-image
  [image film-title scale-factor]
  (let [filename    (str film-title "-" scale-factor "-" (util/uuid) ".png") 
        output-file (io/file (str "/Users/jon.neale/scratch/movie-outputs/" filename))]
    (println "Writing file - " filename)
    (ImageIO/write image "png" output-file)))

(defn new-image
  [desired-width desired-height]
  (BufferedImage. desired-width
                  desired-height
                  BufferedImage/TYPE_INT_RGB))

(defn graphics
  [image]
  (. image createGraphics))
