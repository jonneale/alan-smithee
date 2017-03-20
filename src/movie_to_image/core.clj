(ns movie-to-image.core
  (:import [org.bytedeco.javacv FFmpegFrameGrabber OpenCVFrameConverter Java2DFrameConverter Java2DFrameUtils]

           [java.io ByteArrayOutputStream ByteArrayInputStream]
           [java.awt.image BufferedImage]
           [java.awt Color]
           [javax.imageio ImageIO])
  (:require [clojure.java.io :as io])
  (:gen-class))


(def magnolia ["Magnolia" "/Users/jon.neale/Documents/videos/Magnolia.1999/Magnolia.1999.mp4"])
(def ghost-in-the-shell ["Ghost In The Shell" "/Users/jon.neale/Documents/videos/Ghost in the Shell (1995)/Ghost in the Shell (1995).mp4"])
(def open-range ["Open Range" "/Users/jon.neale/Documents/videos/Open Range (2003)/Open.Range.2003.mp4"])
(def high-rise ["High Rise" "/Users/jon.neale/Documents/videos/High-Rise 2015/High-Rise.mkv"])
(def the-royal-tenenbaums ["The Royal Tenenbaums" "/Users/jon.neale/Documents/videos/The.Royal.Tenenbaums.2001.mp4"])



(defn buffered-image
  [path]
  (let [g (FFmpegFrameGrabber. path)
        c (Java2DFrameConverter.)]
    (. g start)
    (. c getBufferedImage (. g grab))))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn write-image
  [image film-title scale-factor]
  (let [output-file (io/file (str "/Users/jon.neale/scratch/movie-outputs" film-title "-" scale-factor "-" (uuid) ".jpg"))]
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
  (loop []
    (let [f (. frame-grabber grab)
          c (Java2DFrameConverter.)
          image (. c getBufferedImage f)]
      (if image 
        image
        (recur)))))

(defn calculate-offset
  [i scale-factor desired-width]
  [(mod (* i scale-factor) desired-width) (int (/ (* i scale-factor) desired-width))])

(defn progress-report
  [film scale-factor total current]
  (let [percentage-complete (* 100 (/ current total))]
    (when (zero? (mod percentage-complete 1.0))
      (println film " scaled to " scale-factor " is " percentage-complete "% complete"))))


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
        _          (. g start)
        frames     (. g getLengthInFrames)
        _          (. g stop)]
    frames))

(defn tiling
  [film-title film-path frames-to-capture scale-factor desired-width]
  (let [g (FFmpegFrameGrabber. film-path)
        new-image (BufferedImage. desired-width
                                  (inc (int (/ (* frames-to-capture scale-factor) desired-width)))
                                  BufferedImage/TYPE_INT_RGB)
        new-image-graphics (. new-image createGraphics)]
    (println "width - " desired-width " height - " (inc (int (/ (* frames-to-capture scale-factor) desired-width))))
    (. g start)
    (doseq [i (range frames-to-capture)]
      (progress-report film-title scale-factor frames-to-capture i)
      (let [[x-offset y-offset] (calculate-offset i scale-factor desired-width)
            original-image (get-next-frame-as-buffered-image g)]
        (. new-image-graphics drawImage original-image x-offset y-offset scale-factor scale-factor nil)))
    (. g stop)
    (. new-image-graphics dispose)
    (write-image new-image film-title scale-factor)))


(defn generate
  [[film-title film-path]]
  (time
   (doall (pmap #(tiling film-title film-path (* 4800 24) % 720) [1 2 5 10]))))

(defn generate-for-films
  []
  (time
   (doall
    (pmap (fn [[film-title film-path]] 
            (let [duration-in-frames (get-film-length film-path)]
              (tiling film-title film-path duration-in-frames 5 720)))
          [magnolia open-range high-rise the-royal-tenenbaums]))))

(defn -main
  [& args]
  (generate-for-films))
