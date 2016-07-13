(ns nrepl-gui.core
  (:import org.pushingpixels.substance.api.SubstanceLookAndFeel)
  (:gen-class)
  (:require [clojure.string :as str]
            [clojure.tools.nrepl :as repl]))

(use 'clojure.repl)
(use 'seesaw.core)
(use 'seesaw.font)
(use 'seesaw.dev)
(use 'seesaw.keymap)
(use 'clojure.java.shell)
(use 'clojure.pprint)
(use '[clojure.tools.nrepl.server :only (start-server stop-server)])
(use 'seesaw.dev)
(native!)

;; Add selection to the text
;;

(declare display-area)
(declare direct-input)

;; add nrepl functionallity to the frame
(defonce server (start-server :port 7888))

(defn format-output [val]
  (let [myvec (str/split (apply str val) #" ")]
    (println myvec)))

(defn run-command []
  (with-open [conn (repl/connect :port 7888)]
    (text! display-area (format-output (doall (repl/message (repl/client conn 1000) {:op :eval :code (text direct-input)}))))))

(defn keypress [e]
  (let [k (.getKeyChar e)]
    (println k (type k))
    (if (= k \newline)
      (run-command))))

(def display-area
  (text :multi-line? true :text "Clojure 1.8.0 nRepl GUI (Salman Hossain)                                                                                               \n\n\n\n\n\n\n\n\n\n\n" :foreground "white" :background "black" :wrap-lines? true :columns 40))

(def direct-input
  (text :multi-line? false :text "" :foreground "yellow" :listen [:key-typed keypress]))

(def label-for-input
  (label :text "> "))

(defn clojure-input []
  (horizontal-panel :items [label-for-input direct-input]))

(def content (border-panel

              :center display-area
              :south (clojure-input)
              :vgap 5 :hgap 5 :border 5))

(def f (frame :title "nRepl GUI"
              :id 1
              :width 640
              :height 480
              :on-close :exit
              :content content))

;; fix the nrepl server so that it displays to the display area rather than pprinting to server.
(defn -main []
  (println "GUI up and running...")
  (invoke-later
   (-> f pack! show!)
   (SubstanceLookAndFeel/setSkin "org.pushingpixels.substance.api.skin.GraphiteAquaSkin")))
