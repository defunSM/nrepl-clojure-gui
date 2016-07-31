(ns sshgui.core
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

;; Adds the theme for the frame. (Need to make my own theme eventually)

;; Written poorly but allows for users to enter commands through the GUI.
;; Add more counters to support more command arguments.

;; Add ssh abilities.

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

(defn checkwifi []
  (text! display-area (:out (sh "ping" "-c" "3" "www.google.com"))))

(defn lsblk-command []
  (text! display-area (:out (sh "lsblk"))))

(defn devOnNetwork []
  (str
   "[Devices on your network]\n\n"
   (:out (sh "arp" "-a"))
   "\n"
   "[IP Information]\n\n"
   (:out (sh "ip" "link"))))

(defn network-command []
  (devOnNetwork))

;; Features of the GUI

(defn keypress [e]
  (let [k (.getKeyChar e)]
    (println k (type k))
    (if (= k \newline)
      (sh-command (text input-command)))))


(def input-command (text :multi-line? false :text "" :listen [:key-typed keypress]))
(def display-area (text :multi-line? true :text (network-command) :foreground "white" :background "black"))
(def enter-command (button :text "Enter"))

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


(def content (border-panel

              :north  input-command
              :center (scrollable display-area)
              :vgap 5 :hgap 5 :border 5))


;; Handler for the menu bar

(defn handler [event]
  (let [e (.getActionCommand event)]
    (if (= e "Refresh Network Connections")
      (text! display-area (:out (sh "arp" "-a"))))
    (if (= e "Directory Contents")
      (text! display-area (str "[Current Directory]: " (:out (sh "pwd")) "\n" (:out (sh "ls")))))
    (if (= e "Exit")
      (System/exit 0))
    (if (= e "Check Wifi")
      (checkwifi))
    (if (= e "Display USB devices")
      (lsblk-command))
    (if (= e "Select Theme")
      (-> (frame :title "Themes" :id 3 :content (laf-selector) :on-close :hide :height 600 :width 300) pack! show!))))

(listen enter-command :action (fn [e] (sh-command (text input-command))))

(def refresh-ip-network (menu-item :text "Refresh Network Connections"
                                   :tip "Runs the command 'arp -a'. Displaying the devices on your network."
                                   :listen [:action handler]))

(def list-directories (menu-item :text "Directory Contents"
                                 :tip "Runs the command 'ls -a'. Displays the directories in the display area."
                                 :listen [:action handler]))

(def close-program (menu-item :text "Exit"
                              :tip "This will cause the entire program to exit."
                              :listen [:action handler]))

(def wificonnection (menu-item :text "Check Wifi"
                               :tip "Runs the command 'ping -c 3 www.google.com'. Depending on the packets transmitted can determine if you are connected to the internet."
                               :listen [:action handler]))

(def displaylsblk (menu-item :text "Display USB devices"
                             :tip "Runs the command 'lsblk'. This will display all USB's that may be mounted onto your laptop/computer."
                             :listen [:action handler]))

(def select-theme (menu-item :text "Select Theme"
                             :tip "Allows you to change the current theme. Opens up another window to select a varity of different themes. Default Theme: GraphiteAquaSkin"
                             :listen [:action handler]))

;; make the display-area scrollable [Need to do]

(def f (frame :title "SSH Graphical User Interface (SGUI) alpha 1.0.0"
              :id 1
              :menubar (menubar :items
                                [(menu :text "File" :items [refresh-ip-network list-directories wificonnection displaylsblk close-program])
                                 (menu :text "Settings" :items [select-theme])])
              :height 300
              :width 300
              :on-close :hide
              :content content))

(defn -main []
  (println "GUI up and running...")
  (invoke-later
   (-> f pack! show!)
   (SubstanceLookAndFeel/setSkin "org.pushingpixels.substance.api.skin.GraphiteAquaSkin")))
