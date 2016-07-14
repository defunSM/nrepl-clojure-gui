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
  (let [myvec (apply str val)
        modvec (str/split myvec #",")
        counter (count modvec)]
    (println "Amount: " (count modvec))
    (println myvec)
    (if (= counter 6)
      (str (nth (str/split (nth modvec 3) #"}") 0) "\n" ":out nil")
      (if (= counter 8)
        (str (nth (str/split (nth modvec 1) #"}") 0) "\n" (nth (str/split (nth modvec 5) #"}") 0))
        (if (= counter 10)
          (str (nth modvec 0)))))))

(defn run-command []
  (with-open [conn (repl/connect :port 7888)]
    (text! display-area (format-output (doall (repl/message (repl/client conn 1000) {:op :eval :code (text direct-input)}))))))

(defn keypress [e]
  (let [k (.getKeyChar e)]
    (println k (type k))
    (if (= k \newline)
      (run-command))))

(def display-area
  (text :multi-line? true :editable? false :text "Clojure 1.8.0 nRepl GUI (Salman Hossain)                                                                                               \n\n\n\n\n\n\n\n\n\n\n" :foreground "white" :background "black" :wrap-lines? true :columns 40))

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

(defn laf-selector []
  (horizontal-panel
    :items [
            (combobox
              :model    (vals (SubstanceLookAndFeel/getAllSkins))
              :renderer (fn [this {:keys [value]}]
                          (text! this (.getClassName value)))
              :listen   [:selection (fn [e]
                                      ; Invoke later because CB doens't like changing L&F while
                                      ; it's doing stuff.
                                      (invoke-later
                                        (-> e
                                          selection
                                          .getClassName
SubstanceLookAndFeel/setSkin)))])]))

(defn handler [event]
  (let [e (.getActionCommand event)]
    (if (= e "Exit")
      (System/exit 0))
    (if (= e "Stop nREPL server")
      (stop-server server))
    (if (= e "Select Theme")
      (-> (frame :title "Themes" :id 3 :content (laf-selector) :on-close :hide :height 600 :width 300) pack! show!))))

(def exit-program (menu-item :text "Exit"
                              :tip "Closes the entire program."
                              :listen [:action handler]))

(def stopping-server (menu-item :text "Stop nREPL server"
                             :tip "Stops the current nREPL server."
                             :listen [:action handler]))

(def select-theme (menu-item :text "Select Theme"
                             :tip "Allows you to change the current theme."
                             :listen [:action handler]))

(def f (frame :title "nRepl GUI"
              :id 1
              :menubar (menubar :items
                                [(menu :text "File" :items [stopping-server select-theme exit-program])])
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
