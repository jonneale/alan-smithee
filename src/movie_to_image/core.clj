(ns movie-to-image.core
  (:import [org.bytedeco.javacv FFmpegFrameGrabber OpenCVFrameConverter Java2DFrameConverter Java2DFrameUtils]

           [java.io ByteArrayOutputStream ByteArrayInputStream]
           [java.awt.image BufferedImage]
           [java.awt Color]
           [javax.imageio ImageIO])
  (:require [clojure.java.io :as io])
  (:gen-class))


(def p "/Users/jon.neale/Documents/videos/Magnolia.1999/Magnolia.1999.mp4")

(def buffered-image
  (let [g (FFmpegFrameGrabber. p)
        c (Java2DFrameConverter.)]
    (. g start)
    (. c getBufferedImage (. g grab))))

(defn write-image
  [image & [i]]
  (let [output-file (io/file (str "/tmp/result" i ".jpg"))]
    (ImageIO/write image "png" output-file)))

(defn get-frame
  []
  (let [g (FFmpegFrameGrabber. p)]
    (. g start)
    (println "Frame Rate - " (. g getFrameRate))
    (let [f (. g grab)
          c (Java2DFrameConverter.)
          i (. c getBufferedImage f)]
      (. g stop)
      i)))

(defn run-and-resize
  []
  (let [g (FFmpegFrameGrabber. p)]
    (. g start)
    (doseq [i (range (* 10 24))]
      (let [f (. g grab)
            c (Java2DFrameConverter.)
            original-image  (. c getBufferedImage f)]
        (when original-image
          (let [[height width]  (scale-image original-image)
                new-image      (BufferedImage. 10
                                               10
                                               BufferedImage/TYPE_INT_RGB)
                g              (. new-image createGraphics)]
            (. g drawImage original-image 0 0 10 10 nil)
            (. g dispose)
            (write-image new-image i)))))
    (. g stop)))

(defn get-next-frame-as-buffered-image
  [frame-grabber]
  (loop []
    (let [f (. frame-grabber grab)
          c (Java2DFrameConverter.)
          image (. c getBufferedImage f)]
      (if image 
        image
        (recur)))))

(defn tiling
  [seconds-to-capture frame-rate scale-factor]
  (let [g (FFmpegFrameGrabber. p)
        new-image (BufferedImage. (* frame-rate scale-factor)
                                  (* seconds-to-capture scale-factor)
                                  BufferedImage/TYPE_INT_RGB)
        new-image-graphics (. new-image createGraphics)]
    (. g start)
    (doseq [time-in-seconds (range seconds-to-capture)]
      (doseq [frames (range frame-rate)]
        (let [original-image (get-next-frame-as-buffered-image g)]
          (do
            (. new-image-graphics drawImage original-image (* frames scale-factor) (* time-in-seconds scale-factor) scale-factor scale-factor nil)))))
    (. g stop)
    (. new-image-graphics dispose)
    (write-image new-image "FINAL")))
