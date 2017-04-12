(ns movie-to-image.image-analysis
  (:import [java.awt.image BufferedImage DataBufferByte]
           [java.io IOException]
           [javax.imageio ImageIO]
           [java.awt Color])
  (:require [clojure.java.io :as io]))

(def p  "/Users/jon.neale/scratch/movie-outputs/older/foo.jpg")

(defn trim-credits
  [raw-pixel-values]
  (let [pixel-count (count raw-pixel-values)]
    (drop (int (* pixel-count 0.05)) (take (int (* pixel-count 0.95)) raw-pixel-values))))

(defn average-pixels
  [raw-pixel-values]
  (let [pixel-values (trim-credits raw-pixel-values)
        pixel-count (double (count pixel-values))]
    (map #(int (/ % pixel-count)) (apply (partial map +) pixel-values))))

(defn rgb->byte
  ([r g b a]
   (.getRGB (Color. r g b)))
  ([r g b]
   (.getRGB (Color. r g b))))

(defn byte-array->rgb
  [colours]
  (reverse (map #(bit-and % 0xff) colours)))

(defn bytes->rgb
  [image-has-alpha? image-data-as-byte-array]
  (let [partitioned-bytes   (partition (if image-has-alpha? 4 3) image-data-as-byte-array)]
    (map byte-array->rgb partitioned-bytes)))

(defn buffered-image->int-array
  [buffered-image]
  (let [image-data-as-byte-array (byte-array (.. buffered-image getRaster getDataBuffer getData))
        image-has-alpha?         (not (nil? (.getAlphaRaster buffered-image)))]
    (bytes->rgb image-has-alpha? image-data-as-byte-array)))


(defn get-buffered-image
  [path]
  (ImageIO/read (io/file path)))


(defn write-averaged-image
  [input-path output-dir image-averages]
  (let [image (BufferedImage. 64 48 BufferedImage/TYPE_INT_RGB)
        image-averages-as-colour (apply rgb->byte image-averages)
        
        output-file              (io/file (str output-dir (.getName input-path)))]
    (doseq [x (range 64)
            y (range 48)]
      (.setRGB image x y image-averages-as-colour))
    (.drawString (.createGraphics image) (str (first (.getName input-path))) 55 45)
    (println image)
    (println output-file)
    (ImageIO/write image "png" output-file)))

(defn image->average-image
  [input-path output-directory]
  (->> input-path
       get-buffered-image
       buffered-image->int-array
       average-pixels
       (write-averaged-image input-path output-directory)))

(defn process-folder-of-images
  [folder-path]
  (let [filenames (remove #(or (.isDirectory %)
                               (nil? (re-matches #".*jpg" (.getName %)))) (file-seq (io/file folder-path)))]
    (doseq [name filenames]
      (println (.getName name))
      (image->average-image name "/tmp/image-output/without-credits/potter/resized/"))))

(def red "/Users/jon.neale/Desktop/red.png")
(def blue "/Users/jon.neale/Desktop/blue.png")
(def green "/Users/jon.neale/Desktop/green.png")
(def purple "/Users/jon.neale/Desktop/purple.png")
