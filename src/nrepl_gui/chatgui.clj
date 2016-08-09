(ns chatgui.core
    (:import org.pushingpixels.substance.api.SubstanceLookAndFeel)
    (:gen-class)
    (:require [clojure.string :as str]))

(use 'clojure.repl)
(use 'seesaw.core)
(use 'seesaw.font)
(use 'seesaw.dev)
(use 'clojure.java.shell)
(native!)

(declare display-area)
(declare input-command)

(defn sh-command [command-args]
  (let [val (str/split command-args #" ")
        counter (count val)]
    (if (= counter 1)
      (text! display-area (:out (sh (first val)))))
    (if (= counter 2)
      (text! display-area (:out (sh (first val) (second val)))))
    (if (= counter 3)
      (text! display-area (:out (sh (first val) (second val) (nth val 2)))))
    (if (= counter 4)
      (text! display-area (:out (sh (first val) (second val) (nth val 2) (nth val 3)))))
    (if (= counter 5)
      (text! display-area (:out (sh (first val) (second val) (nth val 2) (nth val 3) (nth val 4)))))))

(defn keypress [e]
  (let [k (.getKeyChar e)]
    (println k (type k))
    (if (= k \newline)
      (sh-command (text input-command)))))


(def input-command (text :multi-line? false :text "" :listen [:key-typed keypress]))
(def display-area (text :multi-line? true :text "You have launched ChatBox.\n\n\n\n\n" :foreground "white" :background "black"))

(defn southcontent []
  (horizontal-panel :items [(label :text ">") input-command]))

(defn content []
  (border-panel
   :south (southcontent)
   :center (scrollable display-area)
   :vgap 5 :hgap 5 :border 5))

;; Must fix since makeing the frame is not working at the moment when running lein run in nrepl-gui.

(def f (frame :title "Chat"
              :id 100
              :menubar (menubar :items [(menu :text "File" :items [])])
              :height 300
              :width 300
              :on-close :hide
              :content content))

(defn -main []
  (println "GUI up and running...")
  (invoke-later
   (-> f pack! show!)
   (SubstanceLookAndFeel/setSkin "org.pushingpixels.substance.api.skin.GraphiteAquaSkin")))
