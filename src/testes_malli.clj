(ns testes-malli
  (:require
   [malli.core :as m]))

(comment
  (def Age
    [:and
     {:title "Age"
      :description "It's an age"
      :json-schema/example 20}
     :int [:> 18]]))

(def Autor
  [:map
   [:nome [:string {:min 1 :max 10}]]])

(comment (def Address
   [:map
    [:street :string]
    [:country [:enum "FI" "UA"]]]))

(comment
  (defn printa-exemplo []
    ;(m/properties Autor)
    (m/validate Autor {:nome "Albertooooooooo"})
    ;(me/humanize (m/explain Autor {:nome "Alberto"}))
    ))

