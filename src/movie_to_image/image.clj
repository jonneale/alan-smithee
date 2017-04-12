(ns movie-to-image.image
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
            [movie-to-image.util :as util])
  (:gen-class))

(defn calculate-offset
  [i scale-factor desired-width]
  [(mod (* i scale-factor) desired-width) (int (/ (* i scale-factor) desired-width))])

(defmacro with-image-grabber
  [grabber-binding & body]
  `(let ~(subvec grabber-binding 0 2)
     (do (. ~(grabber-binding 0) start)
         (let [result# ~@body]
           (. ~(grabber-binding 0) stop)
           result#))))

(defn dimension
  [width height]
  (Dimension. width height))

(defn- get-resizer
  [image-width image-height intended-width intended-height]
  (. (DefaultResizerFactory/getInstance)
     getResizer (dimension image-width image-height) (dimension intended-width intended-height)))

(defn scale-preserving-aspect-ratio
  [image-width image-height scaled-width]
  [scaled-width
   (inc (int (* (/ scaled-width image-width) image-height)))])

(defn get-thumbnail-maker
  [frame-grabber image-width image-height intended-width intended-height]
  (let [[image-width image-height]   (frame-dimensions frame-grabber)
        [scaled-width scaled-height] (scale-preserving-aspect-ratio image-width image-height intended-width)]
    (. (FixedSizeThumbnailMaker. intended-width intended-height false true)
       resizer (get-resizer image-width image-height intended-width intended-height))))

(defn now [] (str (java.time.LocalDateTime/now)))

(defn scale-image
  [thumbnail-maker buffered-image-to-scale]
  (. thumbnail-maker make buffered-image-to-scale))

(defn write-image
  [image film-title scale-factor]
  (let [output-file (io/file (str "/Users/jon.neale/scratch/movie-outputs/" film-title "-" scale-factor "-" (util/uuid) ".png"))]
    (ImageIO/write image "png" output-file)))

(defn new-image
  [desired-width desired-height]
  (BufferedImage. desired-width
                  desired-height
                  BufferedImage/TYPE_INT_RGB))

(defn graphics
  [image]
  (. image createGraphics))
