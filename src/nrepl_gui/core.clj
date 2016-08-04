(ns nrepl-gui.core
  (:import org.pushingpixels.substance.api.SubstanceLookAndFeel)
  (:gen-class)
  (:require [clojure.string :as str]
            [clojure.tools.nrepl :as repl]
            [nrepl-gui.sshgui]))

(use 'clojure.repl)
(use 'seesaw.core)
(use 'seesaw.font)
(use 'seesaw.dev)
(use 'seesaw.keymap)
(use 'clojure.java.shell)
(use 'clojure.pprint)
(use 'clojure.java.io)
(use '[clojure.tools.nrepl.server :only (start-server stop-server)])
(use 'seesaw.dev)
(native!)

;; Add selection to the text
;;

(declare display-area)
(declare direct-input)
(declare doc-f)
(declare f)

(def serverport (atom 8000))

(defonce server (start-server :port @serverport))
(def current-file (atom "example213123.txt"))
;; add nrepl functionallity to the frame

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
  (if (= (text direct-input) ":q")
    (System/exit 0)
    (with-open [conn (repl/connect :port @serverport)]
      (text! display-area (format-output (doall (repl/message (repl/client conn 1000) {:op :eval :code (text direct-input)})))))))

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

(defn status-label []
  (label :text "Status: Ready" :h-text-position :left :v-text-position :bottom))

(defn clojure-input []
  (vertical-panel :items [(horizontal-panel :items [label-for-input direct-input]) (status-label)]))

(def text-editor
  (text :multi-line? true :text "\n\n\n\n"))

(def content (border-panel

              :center (tabbed-panel :tabs [{ :title "nREPL" :content display-area}
                                           { :title "Text Editor" :content text-editor}])
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
                                      (invoke-later
                                        (-> e
                                          selection
                                          .getClassName
SubstanceLookAndFeel/setSkin)))])]))

(defn converToInt [string]
  (Integer. (re-find #"\d+" string)))

(defn handler [event]
  (let [e (.getActionCommand event)]
    (if (= e "Exit")
      (System/exit 0))
    (if (= e "Stop nREPL server")
      (do (stop-server server)
          (text! display-area (str "Clojure nREPL 127.0.0.1:" @serverport " is terminated."))))
    (if (= e "Select Theme")
      (-> (frame :title "Themes" :id 3 :content (laf-selector) :on-close :hide :height 600 :width 300) pack! show!))
    (if (= e "Start nREPL Server")
      (do (reset! serverport (converToInt (input "Enter a port number: " :title "Create nREPL Server" :value "3000")))
          (start-server :port @serverport)
          (text! display-area (str "Clojure nREPL 127.0.0.1:" @serverport " has been created."))))
    (if (= e "Connect to external nREPL server")
      (do (reset! serverport (converToInt (input "Enter the port of the external nREPL server." :title "Connect to external nREPL server.")))
          (stop-server server)))
    (if (= e "Documentation")
      (do (-> (frame :title "Documentation" :id 6 :content (scrollable (doc-f)) :on-close :hide :height 600 :width 300) pack! show!)))
    (if (= e "Run Terminal")
      (do (invoke-later (-> sshgui.core/f pack! show!))))
    (if (= e "Open File...")
      (do (reset! current-file (input "Enter the path to the file File you want to access: "))
          (text! text-editor (slurp @current-file))))
    (if (= e "Save File...")
      (do (spit @current-file (text text-editor))))
    (if (= e "Save File As...")
      (do (spit (input "Enter the path you want to save to: ") (text text-editor))))
    (if (= e "Send to nREPL")
      (do (with-open [conn (repl/connect :port @serverport)]
            (text! display-area (format-output (doall (repl/message (repl/client conn 1000) {:op :eval :code (text text-editor)})))))))))

(defn doc-f [] (text :multi-line? true :text "[Documentation]\n\nBy default the server started is on 127.0.0.1:8000\n\nTo Stop the Server:\n\n1) Go to File.\n2) Click 'Stop nREPL server'.\n3) This will terminate the server and you'll be notified of the server being terminated.\n\nTo Start a New Server:\n\n1) Go to File.\n2) Click 'Start nREPL server'.\n3) Enter the port you want in the input box.\n4) The host is by default 127.0.0.1 and should show that you are connected.\n\nConnect to External nREPL Server:\n\n1) Go to File.\n2) Click connect to external nREPL using the port number.\n3)Make sure that the external is running or there will be an error.\n\n For more information check the github:\nhttps://github.com/defunSM/nrepl-clojure-gui" :wrap-lines? true :columns 30))

(def exit-program (menu-item :text "Exit"
                             :tip "Closes the entire program."
                             :listen [:action handler]))

(def stopping-server (menu-item :text "Stop nREPL server"
                                :tip "Stops the current nREPL server."
                                :listen [:action handler]))

(def select-theme (menu-item :text "Select Theme"
                             :tip "Allows you to change the current theme."
                             :listen [:action handler]))

(def starting-server (menu-item :text "Start nREPL Server"
                                :tip "Starts a nREPL server."
                                :listen [:action handler]))

(def connecting-server (menu-item :text "Connect to external nREPL server"
                                  :tip "Connect to an external nREPL server."
                                  :listen [:action handler]))

(def docs (menu-item :text "Documentation"
                     :tip "Provides documentation and help."
                     :listen [:action handler]))

(def run-terminal (menu-item :text "Run Terminal"
                             :tip "Provides a terminal allowing you to run bash commands."
                             :listen [:action handler]))

(def open-file (menu-item :text "Open File..."
                          :tip "Opens a file and displays it in the text editor."
                          :listen [:action handler]))

(def save-file (menu-item :text "Save File..."
                          :tip "Saves the file to the one opened at"
                          :listen [:action handler]))

(def save-as-file (menu-item :text "Save File As..."
                             :tip "Saves the file to a specific path chosen by user."
                             :listen [:action handler]))

(def eval-text (menu-item :text "Send to nREPL"
                          :tip "Sends the text in nREPL as clojure code to the nREPL server."
                          :listen [:action handler]))

(def f (frame :title "nRepl GUI"
              :id 1
              :menubar (menubar :items
                                [(menu :text "File" :items [open-file save-file save-as-file run-terminal exit-program])
                                 (menu :text "nREPL" :items [starting-server stopping-server connecting-server eval-text])
                                 (menu :text "Settings" :items [select-theme])
                                 (menu :text "Help" :items [docs])])
              :width 640
              :height 480
              :on-close :exit
              :content content))

;; fix the nrepl server so that it displays to the display area rather than pprinting to server.
(defn -main []
  (println "GUI up and running...")
  (invoke-later
   (-> f pack! show!)
   (SubstanceLookAndFeel/setSkin "org.pushingpixels.substance.api.skin.ChallengerDeepSkin")))
