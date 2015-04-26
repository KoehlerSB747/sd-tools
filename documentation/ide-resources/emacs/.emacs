;; SBK's default .emacs initialization file

;; don't show annoying splash screen on startup
(setq inhibit-splash-screen t)
;(setq initial-buffer-choice "~")

;; Are we running XEmacs or Emacs?
(defvar running-xemacs (string-match "XEmacs\\|Lucid" emacs-version))

;; Set up the keyboard so the delete key on both the regular keyboard
;; and the keypad delete the character under the cursor and to the right
;; under X, instead of the default, backspace behavior.
(global-set-key [delete] 'delete-char)
(global-set-key [kp-delete] 'delete-char)

;; Turn on font-lock mode for Emacs
(cond ((not running-xemacs)
       (global-font-lock-mode t)
))

;(set-default-font "7x14")
;(set-default-font "6x13")

;; Always end a file with a newline
(setq require-final-newline t)

;; Stop at the end of the file, not just add lines
(setq next-line-add-newlines nil)

;; Enable wheelmouse support by default
(if (not running-xemacs)
    (require 'mwheel) ; Emacs
  (mwheel-install) ; XEmacs
)

;;;;;;
;;;
;;; sbk's general look and feel/behavior preferences...
;;;

;; wdired mode for interactive file renaming
(require 'wdired)
(define-key dired-mode-map "r" 'wdired-change-to-wdired-mode)

;; enable auto-compression-mode
(auto-compression-mode t)

;; turn on transient mark mode for color-hilights and regional ops
(transient-mark-mode 1)

;;(set-background-color "gray10")  ;cyan, SkyBlue4, dark slate gray, dark slate blue, gray10
;;(set-foreground-color "gainsboro")  ;black, thistle1, gainsboro, powder blue
;;(set-cursor-color "gainsboro")
;(set-background-color "white")  ;cyan, SkyBlue4, dark slate gray, dark slate blue, gray10
;(set-foreground-color "gray10")  ;black, thistle1, gainsboro, powder blue
;(set-cursor-color "gray10")

;(setq default-frame-alist '((top . 1) (left . 680) (width . 170) (height . 85)))
(setq next-line-add-newlines nil)

(setq inferior-lisp-program "/usr/bin/clisp")

(setq column-number-mode t)
(setq display-time-day-and-date t)
(setq display-time-mode 1)

;(require 'sudoku)

;(set-language-environment "English")
(set-language-environment "UTF-8")

; configuration for tramp
(setq tramp-default-method "ssh")

; Keep cursor at the bottom of compilation output
(setq compilation-scroll-output t)

;;;
;;; ...sbk
;;;
;;;;;;

;;;
;;; Load emacs-ide.el from "current" dir.
;;;
(setq load-path (cons "~" load-path))
(load "emacs-ide.el")

;;
;; include gnuserv for integration with eclipse
;;
;(require 'gnuserv-compat)
;(require 'gnuserv)
;(gnuserv-start)
;; if XEmacs gnuserv binary gets in the way:
;; (setq server-program "/opt/emacs/gnuserv/default/gnuserv")
;(setq gnuserv-frame (selected-frame))


;;--------------------------------------------------------------------
;; Lines enabling gnuplot-mode

;; move the files gnuplot.el to someplace in your lisp load-path or
;; use a line like
;;  (setq load-path (append (list "/path/to/gnuplot") load-path))

;; these lines enable the use of gnuplot mode
  (autoload 'gnuplot-mode "gnuplot" "gnuplot major mode" t)
  (autoload 'gnuplot-make-buffer "gnuplot" "open a buffer in gnuplot mode" t)

;; this line automatically causes all files with the .gp extension to
;; be loaded into gnuplot mode
  (setq auto-mode-alist (append '(("\\.gp$" . gnuplot-mode)) auto-mode-alist))

;; This line binds the function-9 key so that it opens a buffer into
;; gnuplot mode 
  (global-set-key [(f9)] 'gnuplot-make-buffer)

;; end of line for gnuplot-mode
;;--------------------------------------------------------------------

;;;;;;
;;;
;;; Initialize buffers
;;;

(find-file "~")

(display-time)
(custom-set-variables
  ;; custom-set-variables was added by Custom.
  ;; If you edit it by hand, you could mess it up, so be careful.
  ;; Your init file should contain only one such instance.
  ;; If there is more than one, they won't work right.
 '(c-basic-offset 2)
 '(case-fold-search t)
 '(delete-selection-mode nil nil (delsel))
 '(find-grep-options "-q -i")
 '(indent-tabs-mode nil)
 '(max-lisp-eval-depth 8192)
 '(nxhtml-autoload-web nil t)
 '(scroll-bar-mode (quote right))
 '(speedbar-frame-parameters (quote ((minibuffer) (width . 33) (border-width . 0) (menu-bar-lines . 0) (unsplittable . t))))
 '(speedbar-mode-specific-contents-flag t)
 '(speedbar-sort-tags t)
 '(tab-width 2)
 '(tempo-interactive t))
(custom-set-faces
  ;; custom-set-faces was added by Custom.
  ;; If you edit it by hand, you could mess it up, so be careful.
  ;; Your init file should contain only one such instance.
  ;; If there is more than one, they won't work right.
 '(mode-line ((t (:inverse-video nil :foreground "gray10" :background "gainsboro")))))

;;--------------------------------------------------------------------
;; Lines added for using emacs as ide for android
;;
;; source: http://code.google.com/p/android-emacs-toolkit/
;;

;; Do not use '\' instead '/'
; (if (eq system-type 'windows-nt)
;     (progn (setq android-ndk-root-path "e:/zxy/home/program/android-ndk-r7-windows")
;            (setq android-sdk-root-path "e:/zxy/home/program/android-sdk-windows"))
;   ;; Ubuntu do not understanding '~' instead of 'home'
;   (progn (setq android-ndk-root-path "$HOME/android/android-ndk-r8d")
;          (setq android-sdk-root-path "$HOME/android/android-sdk-linux")))
; (setq android-default-package "com.zxy")
;
;(add-to-list 'load-path "~/android/android-emacs-toolkit")
; (require 'androidmk-mode)
; (add-hook 'androidmk-mode-hook
;           (lambda ()
;             (progn (local-set-key [M-f5] 'androidndk-build)
;                    (local-set-key [M-S-f5] 'androidndk-rebuild)
;                    (local-set-key [C-f5] 'androidsdk-build)
;                    (local-set-key [C-S-f5] 'androidsdk-rebuild)
;                    )))

;; 4 RUNNING EXAMPLES
;;  1.  Follow usage.
;;  2.  Create new android avd.
;;        M-x android-create-avd
;;            Input avd name. 
;;  3.  Launch android avd.
;;        M-x android-launch-avd
;;            Input avd name. 
;;  4.  Output log.
;;        M-x android-start-log
;;  5.  Create new android project.
;;        M-x android-new-project
;;            Input project path, project name, project target, project pachage, project activity and whether create jni folder. Then java file is open. 
;;  6.  Build project with ant and install.
;;        M-x androidsdk-build
;;            Hello world will print on emulator. 
;;  7.  Build ndk project with ndk-build and run.
;;        M-x androidndk-build
;;            Hello world will print on android-log buffer. 

;;
;; end of lines for android
;;--------------------------------------------------------------------
