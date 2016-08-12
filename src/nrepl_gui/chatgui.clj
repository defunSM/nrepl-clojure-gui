(ns chatgui.core
    (:import org.pushingpixels.substance.api.SubstanceLookAndFeel)
    (:gen-class)
    (:require [clojure.string :as str]))

;; This is the code for the chatbox frame.

(use 'clojure.repl)
(use 'seesaw.core)
(use 'seesaw.font)
(use 'seesaw.dev)
(use 'clojure.java.shell)
(native!)

(declare f)
(declare display-area)
(declare input-command)
(def chatname (atom "Unknown"))



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

;; Finish this keypress so that it'll remove the <br></br> from the text using regex.

(defn keypress [e]
  (let [k (.getKeyChar e)]
    (println k (type k))
    (if (= k \newline)
      (do (sh-command (str "curl localhost:5000/chat?" @chatname "=" (text input-command) " "))))))

(def input-command (text :multi-line? false :text "" :listen [:key-typed keypress]))
(def display-area (text :multi-line? true :text "You have launched ChatBox.\n\n\n\n\n" :foreground "white" :background "black"))

(defn southcontent []
  (horizontal-panel :items [(label :text ">") input-command]))

(defn content []
  (border-panel
   :south (southcontent)
   :center (scrollable display-area)
   :vgap 5 :hgap 5 :border 5))

(defn handler [event]
  (let [e (.getActionCommand event)]
    (if (= e "Close ChatBox")
      (do (-> f hide!)))
    (if (= e "Refresh ChatBox")
      (do (sh-command "curl http://localhost:5000/chat?")))
    (if (= e "Change Chat Name")
      (do (reset! chatname (input "Enter the name you want in the chatbox: "))))))

(def close-chatbox (menu-item :text "Close ChatBox"
                              :tip "Closes the ChatBox."
                              :listen [:action handler]))

(def refresh-chat (menu-item :text "Refresh ChatBox"
                             :tip "Displays the new messages in the display area."
                             :listen [:action handler]))

(def enter-chat-name (menu-item :text "Change Chat Name"
                                :tip "This allows you to change your chat name."
                                :listen [:action handler]))

(def f (frame :title "Chat"
              :id 100
              :menubar (menubar :items [(menu :text "File" :items [close-chatbox refresh-chat enter-chat-name])])
              :height 300
              :width 300
              :on-close :hide
              :content (content)))

(defn -main []
  (println "GUI up and running...")
  (invoke-later
   (-> f pack! show!)
   (SubstanceLookAndFeel/setSkin "org.pushingpixels.substance.api.skin.GraphiteAquaSkin")))
