# language: es
Característica: Aviso de medidor desconectado
  Como titular de un hogar o de un negocio
  Quiero saber cuando un medidor deja de reportar
  Para no quedarme sin detección de fugas sin darme cuenta

  Escenario: Un medidor sin reportar por más de 60 minutos se marca desconectado y se avisa una sola vez
    Dado un medidor que reportó por última vez hace 90 minutos
    Cuando se revisa la conexión de los medidores
    Entonces el medidor queda en estado "OFFLINE"
    Y se avisó al titular 1 vez
    Cuando se revisa la conexión de los medidores
    Entonces se avisó al titular 1 vez

  Escenario: Un medidor que reportó hace poco sigue conectado
    Dado un medidor que reportó por última vez hace 10 minutos
    Cuando se revisa la conexión de los medidores
    Entonces el medidor queda en estado "ONLINE"
    Y se avisó al titular 0 veces
