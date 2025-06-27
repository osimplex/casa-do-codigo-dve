(ns validacoes-conversoes
  (:import
   [java.time LocalDate]
   (java.time.format DateTimeFormatter)))

(defn valid-date? [pattern data-string]
  (try
    (let [formatter (DateTimeFormatter/ofPattern pattern)]
      (LocalDate/parse data-string formatter))
    ; Se der erro ao converter, retorna nulo
    (catch Exception _ nil)))

(defn future-date? [pattern data-string]
  (try
    (let [formatter (DateTimeFormatter/ofPattern pattern)
          parsed-date (LocalDate/parse data-string formatter)]
      (.isAfter parsed-date (LocalDate/now))) ;; Verifica se a data é depois da data atual
    ; Se der erro ao converter retorna true, seguindo a ideia da bean validation
    (catch Exception _ true)))

(comment
  (defn string-date [pattern date]
    (let [formatter (DateTimeFormatter/ofPattern pattern)
          stringfied-date (.format date formatter)]
      stringfied-date)))

(defn decimal-string? [valor-string]
  (try
    (BigDecimal. valor-string)
    ; Captura qualquer exceção lançada pela tentativa de conversão
    (catch Exception _ nil)))

(defn decimal-greater-than? [min valor-string]
  (if (and (decimal-string? min) (decimal-string? valor-string))
    (let [decimal-value (BigDecimal. valor-string)]
      (> (.compareTo decimal-value (BigDecimal. min)) 0));; Compara os decimais
    ; Se tiver algum erro de conversao, ignora. Alguém deveria ter tratado.
    true))
