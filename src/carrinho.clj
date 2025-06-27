(ns carrinho
  (:require
   [common-schema]
   [datomic-lib]
   [datomic-schema-carrinho]
   [datomic.api :as d]
   [schema-refined.core :as r]
   [schema.core :as s]
   [utilitarios]))

;validar que chegou um array de itens
;https://github.com/metosin/malli?tab=readme-ov-file#vector-schemas
  ;tem que ter no mínimo um item no array
  ;cada item do array precisa ter uma quantidade maior que zero
  ;cada id referenciado precisa de fato existir
  ;nao pode ter id livro repetido no array

;construção do mapa do carrinho em si
  ;para cada item do array, transforma para um item com o id do livro, preco original, titulo e valor calculado
  ;adiciona o item copiando as informacoes do livro e o montante

(s/defschema NovoCarrinho
  {:items [{:id-livro (r/refined s/Int (r/Greater  0))
            :quantidade (r/refined s/Int (r/Greater  0))}]})

(s/validate NovoCarrinho
            {:items [{:id-livro 17592186045459, :quantidade 3}
                     {:id-livro 17592186045461, :quantidade 4}]})

(defn- mapeia-item-para-id-livro [item]
  (:id-livro item))

(defn- todos-ids-livros-existem [banco-dados payload]
  (let [ids-livros-payload (map mapeia-item-para-id-livro payload)
        ids-encontrados (d/pull-many banco-dados '[:db/id] ids-livros-payload)]
    (= (count ids-encontrados) (count ids-livros-payload))))

(defn- todos-ids-livros-diferentes? [payload]
  (let [ids (map
             (fn [item] (:id-livro item))
             payload)]
    (= (count ids) (count (set ids)))))

(defn- carrega-livro-por-id [dados]
  ;vai retornar a funcao faltando o argumento do id
  (partial datomic-lib/busca-todos-atributos-entidade dados))

(s/defn ^:always-validate logica-novo-carrinho [context
                                                payload :- NovoCarrinho
                                                db
                                                executa-transacao]
  {:pre [(seq (:items payload))]}
  (cond
    (not (todos-ids-livros-diferentes? payload))
    (utilitarios/respond-validation-error-with-json
     context {:global-erros ["Tem livro igual"]})

    (not (todos-ids-livros-existem db payload))
    (utilitarios/respond-validation-error-with-json
     context {:global-erros ["Tem livro referenciado que nao existe"]})

    :else (let [id-carrinho (java.util.UUID/randomUUID)
                carrinho (datomic-schema-carrinho/to-schema
                          id-carrinho (:items payload) (carrega-livro-por-id db))
                id-carrinho-salvo (executa-transacao [carrinho])]
            (utilitarios/respond-with-json context {:id id-carrinho-salvo}))))

(defn handler-passo-1 [{:keys [:datomic]}]
  {:name :carrinho-passo-1
   :enter (fn [context]
            (let [payload (get-in context [:request :json-params])
                  coerced-payload (common-schema/coerce NovoCarrinho payload)]
              (println payload)
              (println coerced-payload)
              (logica-novo-carrinho
               context coerced-payload (:db datomic) (:funcao-transacao datomic))))})
