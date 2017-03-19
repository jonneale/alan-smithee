(ns movie-to-image.core
  (:import [org.bytedeco.javacv FFmpegFrameGrabber OpenCVFrameConverter Java2DFrameConverter Java2DFrameUtils]
           [java.io ByteArrayOutputStream ByteArrayInputStream]
           [java.awt.image BufferedImage]
           [javax.imageio ImageIO])
  (:require [clojure.java.io :as io])
  (:gen-class))


(def p "/Users/jon.neale/Documents/videos/Magnolia.1999/Magnolia.1999.mp4")

(defn to-rgb
  [pixel-value]
  [(bit-and (bit-shift-right pixel-value 24) 0xFF)
   (bit-and (bit-shift-right pixel-value 16) 0xFF)
   (bit-and (bit-shift-right pixel-value 8) 0xFF)
   (bit-and pixel-value 0xFF)])

(defn to-byte
  [[a r g b]]
  (bit-or (bit-shift-left a 24) 
          (bit-shift-left r 16)
          (bit-shift-left g 8)
          b))

(defn average
  [rgb]
  (let [pixel-count (float (count rgb))
        summed-rgb  (apply map + rgb)]
    (map (fn [summed-pixel-total] (int (/ summed-pixel-total pixel-count))) summed-rgb)))


;; (defn averaged-pixels
;;   [buffered-image]
;;   (when buffered-image
;;     (let [width  (. buffered-image getWidth)
;;           height (. buffered-image getHeight)]
;;       (for [x (range width)
;;             y (range height)]
;;         (to-rgb (. buffered-image getRGB x y))))))

(defn to-byte-array
  [buffered-image]
  (let [baos (ByteArrayOutputStream.)]
        (ImageIO/write buffered-image "png" baos)
        (. baos flush)
        (. baos toByteArray)))

(defn averaged-pixels
  [buffered-image]  
  (let [byte-array (to-byte-array buffered-image)]
    (map (fn [v] (int (/ v (float (count byte-array)))))
         (apply map + (map to-rgb byte-array)))))

  ;; (when buffered-image
  ;;   (let [data (. (. buffered-image getRaster) getDataBuffer)
  ;;         ]
  ;;     (for [x (range width)
  ;;           y (range height)]
  ;;       (to-rgb (. buffered-image getRGB x y)))))

(defn rgb
  [i]
  (. i getRGB))

(def buffered-image
  (let [g (FFmpegFrameGrabber. p)
        c (Java2DFrameConverter.)]
    (. g start)
    (. c getBufferedImage (. g grab))))

(defn write-frame
  [i frame-grabber]
  (let [output-file (io/file (str "/tmp/output" i ".png"))
        f (. frame-grabber grab)
        c (Java2DFrameConverter.)
        i (. c getBufferedImage f)
        p (averaged-pixels i)]
    (when i
      (ImageIO/write i "png" output-file))))

(def last-image
  (atom nil))

(defn average-image
  [i frame-grabber]
  (let [output-file (io/file (str "/tmp/output" i ".png"))
        f (. frame-grabber grab)
        c (Java2DFrameConverter.)
        i (. c getBufferedImage f)]
    (when i (do
              (reset! last-image i)
              (averaged-pixels i)))))

(defn get-averaged-values-for-frames
  [frame-count]
  (let [g (FFmpegFrameGrabber. p)]
    (. g start)
    (remove nil?
            (for [i frame-count]
              (average-image i g)))))

(defn run-x
  []
  (let [frame-average-colours (get-averaged-values-for-frames (range (* 10 24)))
        bais (ByteArrayInputStream. (byte-array frame-average-colours))
        image (ImageIO/read bais)]))

(defn to-int-array
  [averages]
  (int-array (map to-byte averages)))

(defn get-pixels
  [seconds-to-extract]
  (let [frame-average-colours (get-averaged-values-for-frames (range (* seconds-to-extract 24)))
        pixels                (to-int-array frame-average-colours)]
    pixels))

(defn make-image
  [pixels]
  (let [width                 8
        height                (max 1 (int (/ (dec (count pixels)) width)))
        image (BufferedImage. width height BufferedImage/TYPE_INT_ARGB)]
    (println "index - " (* width height))
    (println "actual count " (count pixels))
    (. (. image getData)
       setDataElements (int 0) (int 0) (int width) (int height) pixels)
    image))

(defn write-image
  [image]
  (let [output-file (io/file (str "/tmp/result.jpg"))]
    (ImageIO/write image "jpg" output-file)))

(defn run
  []
  (-> 10
      get-pixels
      make-image
      write-image))
