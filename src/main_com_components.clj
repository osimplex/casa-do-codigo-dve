(ns main-com-components
  (:require
   [com.stuartsierra.component :as component]
   [com.stuartsierra.component.repl
    :refer [set-init]]
   [io.pedestal.http :as http]))

(comment
  (defn test?
    [service-map]
    (= :test (:env service-map))))

(defrecord Pedestal [service-map service]
  component/Lifecycle
  ;acho que é aqui que eu vou precisa configurar a rota usando os componentes gerenciados
  ; o serice-map tem as rotas e eu posso comportar essas rotas agora. 
  (start [this]
    (if service
      this
      (assoc this :service (http/start (http/create-server service-map)))))
  (stop [this]
    (http/stop service)
    (assoc this :service nil)))

(defn new-pedestal []
  (map->Pedestal {}))

(defn respond-hello [dependencias]
  (fn []
    (println dependencias)
    (println "=====33333")
    {:status 200 :body "Hello, world!"}))

(defn routes [dependencias]
  #{["/greet" :get (respond-hello dependencias) :route-name :greet]})

(defrecord Rotas []
  component/Lifecycle
  (start [this]
    (println "Subindo componente de rota")
    this)
  (stop [this]
    (println "Destruindo componente de rota")
    this))

(defrecord ServiceMap [env rotas]
  component/Lifecycle
  (start [this]
    (println "Subbindo service map")
    (println (str "Dependencias" this " e " rotas))
    (assoc this  :env env
           ::http/routes (routes {:rotas rotas})
           ::http/type :jetty
           ::http/port 8890
           ::http/join? false))
  (stop [this]
    (println "Destruindo service map")
    this))

(defn new-system [env]
  (component/system-map
   :rotas (->Rotas)
   :service-map
   (component/using
    (map->ServiceMap {:env env})
    [:rotas])
   :pedestal
   (component/using
    (new-pedestal)
    [:service-map])))

(set-init (fn [] (new-system :prod)))
