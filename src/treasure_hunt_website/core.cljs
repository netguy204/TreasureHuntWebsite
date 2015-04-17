(ns ^:figwheel-always treasure-hunt-website.core
    (:require [om.core :as om]
              [om.dom :as dom]
              [domina :refer [by-id]]
              [core.async :refer [chan put! <! go]]
              ))

(enable-console-print!)

(println "Edits to this text should show up in your developer console.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:users []
                          :wishes []}))

(defn display-user [selected]
  (if selected
    "selected-user"
    ""))

Flagrant ERROR

(defn get-wishes-for-user [id]
  (println "Attempting to get wishes for user: " id))

(defn user-view [crsr owner]
  (reify
    om/IRenderState
    (render [_ state]
      (dom/li #js {:onClick #((get-wishes-for-user (:id crsr)) (put! (:click-chan state) owner))
                   :className (display-user (:selected state))}
              ))))

(defn users-view [crsr owner]
  (reify
    om/IInitState
    (init-state [_]
      {:click-chan (chan)})

    om/IWillMount
    (will-mount [_]
      (let [click-chan (om/get-state owner :click-chan)]
        (go (loop []
              (let [clicked-user (<! click-chan)]
                (om/set-state! (om/get-state! owner :selected-user) :selected false)
                (om/set-state! owner :selected-user clicked-user)
                (om/set-state! clicked-user :selected true))))))
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:id "users"}
               (dom/h2 nil "Wishers"
                       (apply dom/ul nil
                              (om/build-all user-view (:users crsr) {:init-state state})))))))

(om/root users-view app-state
         {:target (by-id "users")})


