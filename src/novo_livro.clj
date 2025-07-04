(ns novo-livro
  (:require
   [datomic-lib]
   [datomic-schema-livro]
   [malli.core :as m]
   [malli.error :as me]
   [utilitarios]
   [validacoes-conversoes]))

(def schema-basico-novo-livro
  [:map
   [:titulo [:string {:min 1 :max 20 :error/message "Titulo é obrigatório"}]]
   [:resumo [:string {:min 1 :max 500 :error/message "Resumo é obrigatório"}]]
   [:preco [:and
             [string? {:min 1 :error/message "Preço é obrigatório"}]
             [:fn {:error/message "Preco não está bem formatado"}
              #(validacoes-conversoes/decimal-string? %)]
             [:fn {:error/message "Preço precisa ser maior que zerp"}
              #(validacoes-conversoes/decimal-greater-than? 0 %)]]]
   [:isbn [:string {:min 1 :max 500 :error/message "ISBN é obrigatório"}]]
   ;pq eu não posso chamar a função que retorna o array
   [:data-lancamento [:and
                      [:string {:min 1 :error/message "Data é obrigatória"}]
                      ;na documentacao explica que quando usa funcao, as propriedades
                      ;tem que ser passadas primeiro
                      [:fn {:error/message "Data mal formatada"}
                       #(validacoes-conversoes/valid-date? "yyyy-MM-dd" %)]
                      [:fn {:error/message "Data não está no futuro"}
                       #(validacoes-conversoes/future-date? "yyyy-MM-dd" %)]]]
   [:id-categoria [number? {:min 1 :error/message "Categoria é obrigatória"}]]
   [:id-autor [number? {:min 1 :error/message "Autor é obrigatório"}]]])

(def handler
  {:name :novo-livro
   :enter (fn [context]
            (let [payload (utilitarios/parse-json-body context)
                  ;aqui eu estou validando duas vezes?
                  dados-basicos-estao-validos? (m/validate schema-basico-novo-livro payload)
                  errors (me/humanize (m/explain schema-basico-novo-livro payload))
                  dados (get-in context [:request :db])]
              (cond
                (not dados-basicos-estao-validos?)
                (utilitarios/respond-validation-error-with-json context errors)

                (datomic-lib/busca-entidades-por-unico-atributo
                 dados :livro/titulo (:titulo payload))
                (utilitarios/respond-validation-error-with-json
                 context
                 {:global-errors ["Ja existe um livro com o mesmo titulo"]})

                (not (datomic-lib/busca-entidade dados (:id-categoria payload)))
                (utilitarios/respond-validation-error-with-json
                 context {:global-errors ["Nao existe a categoria referenciada"]})

                (not (datomic-lib/busca-entidade dados (:id-autor payload)))
                (utilitarios/respond-validation-error-with-json
                 context {:global-errors ["Nao existe o autor referenciado"]})

                :else (let [data-lancamento-convertida
                            (validacoes-conversoes/valid-date?
                             "yyyy-MM-dd" (:data-lancamento payload))
                            data-lancamento-como-date-antigo
                            (utilitarios/local-date->date data-lancamento-convertida)
                            preco-convertido
                            (validacoes-conversoes/decimal-string? (:preco payload))
                            novo-livro
                            (assoc payload
                                   :preco preco-convertido
                                   :data-lancamento data-lancamento-como-date-antigo)
                            novos-ids
                            (utilitarios/executa-transacao
                             context [(datomic-schema-livro/to-schema novo-livro)])]
                        (utilitarios/respond-with-json context {:id novos-ids})))))})
