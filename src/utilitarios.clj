(ns utilitarios
  (:require
   [io.pedestal.http :as http])
  (:import
   [java.time ZoneId]
   [java.util Date]))

(defn local-date->date [local-date]
  (Date/from (.toInstant (.atStartOfDay local-date (ZoneId/systemDefault)))))

;;paraTreinar aqui eu posso usar o lance das specs, para definir bem a entrada. Brincar de pre e pos condicoes
(defn parse-json-body [context]
  (get-in context [:request :json-params]))

;;aqui pode ser um multimetodo
(defn respond-with-json [context payload]
  (http/respond-with context 200 payload))

(defn respond-validation-error-with-json [context errors]
  ;aqui antes eu tava setando o content-type da resposta, não pode mais. da problema lá no
  ;interceptor. acho que se for setar, tem que definir a chave como string e não como
  ;simbolo. 
  (http/respond-with context 400 errors))

(defn respond-with-status [context status]
  (http/respond-with context status))

(defn executa-transacao
  "Executa uma transacao supondo que o context passado tem uma chave 
  [:request :funcao-transacao] que retorna uma função que recebe um mapa como argumento."
  [context mapa]
  (let [funcao-transacao (get-in context [:request :funcao-transacao])]
    (funcao-transacao mapa)))
