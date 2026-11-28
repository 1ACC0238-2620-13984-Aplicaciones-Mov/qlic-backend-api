# language: es
Característica: Detección automática de fugas (US15)
  Como titular de un hogar o de un negocio
  Quiero que Qlic evalúe cada lectura que envía mi medidor
  Para enterarme de una fuga sin tener que revisar la aplicación

  Antecedentes:
    Dado un punto de agua con un umbral de 125 litros por hora

  Escenario: Un pico aislado no genera alerta
    Cuando el medidor reporta un caudal de 360 litros por hora sin una lectura previa reciente
    Entonces no se genera ninguna alerta

  Escenario: Un caudal sostenido dentro del umbral no genera alerta
    Dado que la lectura anterior reportó 100 litros por hora
    Cuando el medidor reporta un caudal de 110 litros por hora
    Entonces no se genera ninguna alerta

  Esquema del escenario: Un caudal sostenido sobre el umbral genera una alerta según el exceso
    Dado que la lectura anterior reportó <caudal> litros por hora
    Cuando el medidor reporta un caudal de <caudal> litros por hora
    Entonces se genera una alerta con urgencia "<urgencia>"

    Ejemplos:
      | caudal | urgencia      |
      | 140    | INFORMATIONAL |
      | 170    | MODERATE      |
      | 360    | CRITICAL      |
