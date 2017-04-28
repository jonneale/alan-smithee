(ns alan-smithee.film
  (:import [org.bytedeco.javacv FFmpegFrameGrabber OpenCVFrameConverter Java2DFrameConverter Java2DFrameUtils])
  (:require [alan-smithee.image :as image]))

(defn frame-dimensions
  [grabber]
  [(.getImageWidth grabber) (.getImageHeight grabber)])

(defn get-next-frame-as-buffered-image
  [frame-grabber]
  (let [f (.grabImage frame-grabber)
        c (Java2DFrameConverter.)]
    (.getBufferedImage c f)))

(defn get-film-length
  [film-path]
  (image/with-image-grabber [g (FFmpegFrameGrabber. film-path)]
    (.getLengthInFrames g)))

(defn calculate-offset
  [i scaled-width scaled-height desired-width]
  [(mod (* i scaled-width) desired-width) (int (* scaled-height (/ (* i scaled-width) desired-width)))])
