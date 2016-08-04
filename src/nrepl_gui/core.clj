(ns nrepl-gui.core
  (:import org.pushingpixels.substance.api.SubstanceLookAndFeel)
  (:gen-class)
  (:require [clojure.string :as str]
            [clojure.tools.nrepl :as repl]
            [nrepl-gui.sshgui]))

(use '[clojure.tools.nrepl.server :only (start-server stop-server)])
(use 'clojure.repl)
(use 'seesaw.core)
(use 'seesaw.font)
(use 'seesaw.dev)
(use 'seesaw.keymap)
(use 'clojure.java.shell)
(use 'clojure.pprint)
(use 'clojure.java.io)
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

;; This defn helps format the result of the clojure code in the display-area widget.

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

;; This is the nrepl to run clojure code. Takes the input given to direct input widget and than runs the code and displays the code in the display-area widget.

(defn run-command []
  (if (= (text direct-input) ":q")
    (System/exit 0)
    (with-open [conn (repl/connect :port @serverport)]
      (text! display-area (format-output (doall (repl/message (repl/client conn 1000) {:op :eval :code (text direct-input)})))))))

;; This listens to the direct-input widget and detects newlines.

(defn keypress [e]
  (let [k (.getKeyChar e)]
    (println k (type k))
    (if (= k \newline)
      (run-command))))

;; This is the display area for any clojure code inputted into the direct input.

(def display-area
  (text :multi-line? true :editable? false :text "Clojure 1.8.0 nRepl GUI (Salman Hossain)                                                                                               \n\n\n\n\n\n\n\n\n\n\n" :foreground "white" :background "black" :wrap-lines? true :columns 40))

;; This is the text widget that is part of the clojure input defn.

(def direct-input
  (text :multi-line? false :text "" :foreground "yellow" :listen [:key-typed keypress]))

;; This is a label for the ">" that appears on the left of the clojure input.

(def label-for-input
  (label :text "> "))

;; This is a label widget that is supposed to tell the current status of the frame.

(defn status-label []
  (label :text "Status: Ready" :h-text-position :left :v-text-position :bottom))

;; This contains the clojure text input where you can type clojure code and press enter to display the output of the code.

(defn clojure-input []
  (vertical-panel :items [(horizontal-panel :items [label-for-input direct-input]) (status-label)]))

;; This is the area for where you can edit text on the second tab labeled text editor.

(def text-editor
  (text :multi-line? true :text "\n\n\n\n"))

;; This is the widgets for the main frame.

(def content (border-panel
              :center (tabbed-panel :tabs [{ :title "nREPL" :content display-area}
                                           { :title "Text Editor" :content text-editor}])
              :south (clojure-input)
              :vgap 5 :hgap 5 :border 5))

;; This is the theme selector.

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

;; This converts a string to a integer.

(defn converToInt [string]
  (Integer. (re-find #"\d+" string)))

;; The handler function for the menubar of the main frame.

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

;; The documentation text that appears when clicking the documentation in the help menubar.

(defn doc-f [] (text :multi-line? true :text "[Documentation]\n\nBy default the server started is on 127.0.0.1:8000\n\nTo Stop the Server:\n\n1) Go to File.\n2) Click 'Stop nREPL server'.\n3) This will terminate the server and you'll be notified of the server being terminated.\n\nTo Start a New Server:\n\n1) Go to File.\n2) Click 'Start nREPL server'.\n3) Enter the port you want in the input box.\n4) The host is by default 127.0.0.1 and should show that you are connected.\n\nConnect to External nREPL Server:\n\n1) Go to File.\n2) Click connect to external nREPL using the port number.\n3)Make sure that the external is running or there will be an error.\n\n For more information check the github:\nhttps://github.com/defunSM/nrepl-clojure-gui" :wrap-lines? true :columns 30))

;; Exits the program

(def exit-program (menu-item :text "Exit"
                             :tip "Closes the entire program."
                             :listen [:action handler]))

;; Stops the nrepl server

(def stopping-server (menu-item :text "Stop nREPL server"
                                :tip "Stops the current nREPL server."
                                :listen [:action handler]))

;; This brings up a theme selection widget that allows you to select a different theme.

(def select-theme (menu-item :text "Select Theme"
                             :tip "Allows you to change the current theme."
                             :listen [:action handler]))

;; This starts a nrepl server. Allows you to select the port.

(def starting-server (menu-item :text "Start nREPL Server"
                                :tip "Starts a nREPL server."
                                :listen [:action handler]))

;; Connects to an external nrepl server.

(def connecting-server (menu-item :text "Connect to external nREPL server"
                                  :tip "Connect to an external nREPL server."
                                  :listen [:action handler]))

;; This brings up the docs frame which contains documentation.

(def docs (menu-item :text "Documentation"
                     :tip "Provides documentation and help."
                     :listen [:action handler]))

;; This brings up a terminal which can run bash commands.

(def run-terminal (menu-item :text "Run Terminal"
                             :tip "Provides a terminal allowing you to run bash commands."
                             :listen [:action handler]))

;; This opens a file which than displays the file in the text editor.

(def open-file (menu-item :text "Open File..."
                          :tip "Opens a file and displays it in the text editor."
                          :listen [:action handler]))

;; This saves the file that is opened in the text editor.

(def save-file (menu-item :text "Save File..."
                          :tip "Saves the file to the one opened at"
                          :listen [:action handler]))

;; saves the text in the text editor to a specific file that can be set by typing out the path.

(def save-as-file (menu-item :text "Save File As..."
                             :tip "Saves the file to a specific path chosen by user."
                             :listen [:action handler]))

;; text from the text editor is sent and evaluated as clojure code.

(def eval-text (menu-item :text "Send to nREPL"
                          :tip "Sends the text in nREPL as clojure code to the nREPL server."
                          :listen [:action handler]))

;; This is the main frame that is first displayed and contains a menubar and content which includes individual widgets.

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

;; Main function

(defn -main []
  (println "GUI up and running...")
  (invoke-later
   (-> f pack! show!)
   (SubstanceLookAndFeel/setSkin "org.pushingpixels.substance.api.skin.ChallengerDeepSkin")))
