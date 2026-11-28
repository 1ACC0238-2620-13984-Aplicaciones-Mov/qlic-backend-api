# Guía de integración con las aplicaciones móviles

Esta guía resume lo que necesitan las aplicaciones Android (Kotlin) y Flutter para consumir el RESTful API de Qlic. La especificación completa, con todos los campos y ejemplos, está en Swagger UI.

| Recurso | URL |
|---|---|
| API desplegada | `https://qlic-backend-api.onrender.com` |
| Swagger UI | `https://qlic-backend-api.onrender.com/swagger-ui/index.html` |
| OpenAPI JSON | `https://qlic-backend-api.onrender.com/v3/api-docs` |
| API local | `http://10.0.2.2:8080` desde el emulador de Android (`./gradlew bootRun`) |

## 1. Convenciones generales

- **Formato:** JSON en UTF-8. Envíe `Content-Type: application/json` en las solicitudes con cuerpo.
- **Autenticación:** todas las rutas, salvo `/api/v1/authentication/**`, `/api/v1/plans/**` y `/api/v1/terms-of-service/**`, exigen `Authorization: Bearer <token>`.
- **Idioma de los mensajes:** envíe `Accept-Language: es-419` para recibir los mensajes de error y de las notificaciones en español. Sin el encabezado se responden en inglés.
- **Fechas:**
  - Los campos de tipo instante (`expiresAt`, `createdAt` de IAM) llegan en UTC con `Z`, por ejemplo `2026-11-27T01:30:00Z`.
  - Los campos de fecha y hora local (`recordedAt`, `lastReadingAt`, `detectedAt`, `generatedAt`) llegan sin zona, por ejemplo `2026-11-26T20:30:00`, y están en hora de Lima.
  - Los meses se envían como `yyyy-MM`.
- **Identificadores:** todos los `id` son UUID en texto.
- **Cuenta de la sesión:** ningún endpoint recibe el `accountId` en el cuerpo ni en la URL de los recursos. El API lo toma del token, y un recurso de otra cuenta responde **404**.
- **Primer acceso del día:** el servicio en Render puede tardar hasta un minuto en responder la primera solicitud después de estar inactivo. Use un timeout de lectura de al menos 60 segundos y muestre un indicador de carga.

## 2. Tipos de usuario y permisos

El "grupo" en Qlic es la **cuenta**: el hogar o el negocio. Cada cuenta tiene un **titular** y puede tener **miembros invitados**. El permiso de la sesión llega en el campo `permission` del inicio de sesión y de `GET /api/v1/accounts/me`.

| Operación | Titular `OWNER` | Miembro `OPERATOR` | Miembro `VIEWER` |
|---|---|---|---|
| Consultar dashboard, consumo, reportes, alertas, tanques, dispositivos e incidencias | ✅ | ✅ | ✅ |
| Editar su perfil (nombre y teléfono) y registrar su celular para notificaciones push | ✅ | ✅ | ✅ |
| Recibir las notificaciones push de la cuenta | ✅ | ✅ | ✅ |
| Atender alertas y registrar incidencias | ✅ | ✅ | 403 |
| Configurar locales, puntos de agua, dispositivos, simulación, tanques, umbrales y preferencias | ✅ | ✅ | 403 |
| Contratar o cambiar el plan | ✅ | ✅ | 403 |
| Invitar y revocar miembros, y cambiar el nombre de la cuenta | ✅ | 403 | 403 |

La aplicación oculta lo que el permiso no permite; el API responde **403** con el código `iam.permission.denied` aunque la aplicación muestre el botón por error.

Los planes pertenecen a un segmento (`HOME` o `BUSINESS`). Una aplicación dedicada a un segmento lista sus planes con `GET /api/v1/plans?segment=HOME` o `GET /api/v1/plans?segment=BUSINESS`.

## 3. Flujos de sesión

### 3.1. Titular: registro e inicio de sesión

1. `POST /api/v1/authentication/sign-up`
   ```json
   { "fullName": "Patricia Salas", "email": "patricia@example.com", "password": "Qlic2026!", "phone": "+51987654321", "acceptTerms": true }
   ```
   Responde `201` con `subscriberId` y `accountId`. Un correo ya registrado responde `409` (`iam.email.already-registered`).
2. `POST /api/v1/authentication/sign-in`
   ```json
   { "email": "patricia@example.com", "password": "Qlic2026!" }
   ```
   Responde `200`:
   ```json
   {
     "subscriberId": "6f1c…",
     "accountId": "a1b2…",
     "email": "patricia@example.com",
     "fullName": "Patricia Salas",
     "token": "eyJhbGciOiJIUzI1NiJ9…",
     "tokenType": "Bearer",
     "expiresAt": "2026-11-28T01:30:00Z",
     "permission": "OWNER",
     "sharedAccounts": []
   }
   ```
3. Guarde `token` y `expiresAt` en almacenamiento cifrado (Android Keystore o `flutter_secure_storage`).
4. Active la prueba gratuita una sola vez con `POST /api/v1/subscriptions/trial` (`201`). Una segunda vez responde `409` (`subscriptions.trial.already-used`).

### 3.2. Miembro: aceptar la invitación y entrar a la cuenta compartida

1. El titular invita con `POST /api/v1/invitations`:
   ```json
   { "email": "diego@example.com", "permission": "OPERATOR" }
   ```
   El plan limita el número de miembros (`409` `iam.invitation.limit-reached`).
2. El miembro se registra con **el mismo correo** e inicia sesión. Su token apunta a su propia cuenta, vacía.
3. Lista sus invitaciones pendientes con `GET /api/v1/invitations/received` y acepta una con `POST /api/v1/invitations/{invitationId}/acceptance` (`201`).
4. Desde ese momento, el inicio de sesión trae la cuenta en `sharedAccounts`:
   ```json
   "sharedAccounts": [
     { "accountId": "a1b2…", "accountName": "Bodega Salas", "permission": "OPERATOR", "authorizedAt": "2026-11-26T22:10:00Z" }
   ]
   ```
5. La aplicación abre la sesión sobre esa cuenta, sin volver a pedir la contraseña:
   `POST /api/v1/accounts/{accountId}/sessions` → `200` con un **token nuevo** cuyo `accountId` y `permission` son los de la cuenta compartida. Reemplace el token guardado por este.

Regla sugerida para la pantalla posterior al inicio de sesión:

| `sharedAccounts` | Acción |
|---|---|
| Vacío | Mostrar las invitaciones recibidas para aceptar una |
| Una cuenta | Abrir la sesión sobre ella automáticamente |
| Varias cuentas | Mostrar un selector con `accountName` y `permission` |

### 3.3. Mostrar a qué cuenta pertenece el usuario

`GET /api/v1/accounts/me` devuelve `accountName` y `permission`. Muestre, por ejemplo, **"Bodega Salas · Operador"** o **"Casa Salas · Titular"** en el encabezado o en el perfil.

### 3.4. Vencimiento de la sesión y cierre

- El token dura 24 horas. Ante cualquier **401** (`iam.token.required` o `iam.token.invalid`), borre la sesión y vuelva al inicio de sesión.
- Al cerrar sesión, dé de baja el celular con `DELETE /api/v1/notifications/device-tokens?token=<token FCM>` antes de borrar el token de acceso.

## 4. Formato de los errores

Todos los errores usan Problem Details (RFC 9457) con `Content-Type: application/problem+json` y un campo `code` estable:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Su plan permite hasta 2 puntos de agua. Mejore su plan para monitorear más.",
  "instance": "/api/v1/water-points",
  "code": "monitoring.water-point.limit-reached"
}
```

- Muestre `detail` al usuario: ya viene traducido según `Accept-Language`.
- Use `code` para decidir la navegación. Por ejemplo, ante `monitoring.water-point.limit-reached`, ofrezca mejorar el plan.
- Los errores de validación (`code` = `error.validation`) traen además `errors`, con el mensaje de cada campo para mostrarlo debajo del input:
  ```json
  { "status": 400, "code": "error.validation", "errors": { "email": "El correo electrónico no tiene un formato válido." } }
  ```

### Códigos de error

| `code` | HTTP | Mensaje (`es-419`) |
|---|---:|---|
| `alerting.alert.not-found` | 404 | La alerta no existe en su cuenta. |
| `alerting.recipient.not-found` | 404 | El destinatario de alertas no existe en su cuenta. |
| `consumption.goal.baseline-unavailable` | 409 | El mes anterior no tiene lecturas, por lo que aún no se puede medir la reducción. |
| `consumption.goal.invalid` | 400 | La reducción debe ser mayor que 0% y menor que 100%. |
| `consumption.goal.not-found` | 404 | No se definió una meta de sostenibilidad para ese mes. |
| `consumption.period.invalid` | 400 | El periodo no es válido: el inicio no puede ser posterior al término y no puede superar los 366 días. |
| `consumption.schedule.invalid` | 400 | El horario no es válido: la hora de apertura y de cierre deben ser distintas y debe haber al menos un día activo. |
| `consumption.schedule.not-defined` | 404 | Aún no se ha declarado el horario de actividad del local. |
| `consumption.tariff.invalid` | 400 | El precio por metro cúbico debe ser positivo. |
| `error.invalid-request` | 400 | La solicitud no es válida. |
| `error.validation` | 400 | Uno o más campos no son válidos. |
| `iam.account.access-denied` | 403 | No tiene acceso a la cuenta solicitada. |
| `iam.authorized-user.not-found` | 404 | El usuario autorizado no existe en su cuenta. |
| `iam.credentials.invalid` | 401 | Las credenciales no son válidas. |
| `iam.email.already-registered` | 409 | El correo {0} ya se encuentra registrado. |
| `iam.email.invalid` | 400 | El correo electrónico no tiene un formato válido. |
| `iam.invitation.already-invited` | 409 | La persona {0} ya tiene acceso o una invitación pendiente. |
| `iam.invitation.expired` | 409 | La invitación venció. Solicite una nueva al titular de la cuenta. |
| `iam.invitation.invalid` | 400 | La invitación no es válida. Invite a otro correo con permiso VIEWER u OPERATOR. |
| `iam.invitation.limit-reached` | 409 | Su plan permite hasta {0} usuarios adicionales. Mejore su plan para invitar a más personas. |
| `iam.invitation.not-addressed` | 403 | La invitación está dirigida a otro correo. |
| `iam.invitation.not-found` | 404 | La invitación no existe. |
| `iam.invitation.not-pending` | 409 | La invitación ya fue utilizada. |
| `iam.permission.denied` | 403 | No cuenta con el permiso requerido para esta operación. |
| `iam.reset-token.expired` | 400 | El enlace de restablecimiento venció. Solicite un nuevo enlace para definir su contraseña. |
| `iam.reset-token.invalid` | 400 | El enlace de restablecimiento no es válido o ya fue utilizado. Solicite un nuevo enlace. |
| `iam.subscriber.not-found` | 404 | El suscriptor no existe. |
| `iam.terms.not-accepted` | 400 | Debe aceptar los términos y condiciones del servicio para registrarse. |
| `iam.token.invalid` | 401 | Su sesión venció o no es válida. Inicie sesión nuevamente. |
| `iam.token.required` | 401 | Inicie sesión para continuar: la solicitud no tiene token de acceso. |
| `monitoring.device.already-registered` | 409 | El dispositivo con número de serie {0} ya está registrado en otra cuenta. |
| `monitoring.device.not-found` | 404 | El dispositivo no existe en su cuenta. |
| `monitoring.device.without-water-point` | 409 | Asigne el dispositivo a un Water Point antes de enviar lecturas. |
| `monitoring.location.has-tanks` | 409 | El local todavía tiene tanques. Elimínelos antes de eliminar el local. |
| `monitoring.location.has-water-points` | 409 | El local todavía tiene puntos de agua. Muévalos o elimínelos antes de eliminar el local. |
| `monitoring.location.invalid` | 400 | El nombre es obligatorio y no puede estar vacío. |
| `monitoring.location.not-found` | 404 | El local no existe en su cuenta. |
| `monitoring.reading.period.invalid` | 400 | El periodo no es válido: el inicio no puede ser posterior al fin y no puede superar los 92 días. |
| `monitoring.tank.invalid` | 400 | Los datos del tanque no son válidos: la capacidad debe ser positiva y el nivel crítico y el nivel reportado deben estar entre 0 y 100. |
| `monitoring.tank.not-found` | 404 | El tanque no existe en su cuenta. |
| `monitoring.water-point.has-devices` | 409 | El punto de agua todavía tiene un dispositivo asignado. Asígnelo a otro punto de agua o desvincúlelo primero. |
| `monitoring.water-point.limit-reached` | 409 | Su plan permite hasta {0} puntos de agua. Mejore su plan para monitorear más. |
| `monitoring.water-point.not-found` | 404 | El punto de agua no existe en su cuenta. |
| `notifications.device-token.not-found` | 404 | El dispositivo no está registrado en su cuenta. |
| `subscriptions.subscription.not-found` | 404 | La suscripción no existe en su cuenta. |
| `subscriptions.trial.already-used` | 409 | El periodo de prueba ya fue utilizado por esta cuenta. |
| `support.alert.not-active` | 409 | La alerta ya no está activa. Registre la incidencia seleccionando el dispositivo o Water Point. |
| `support.alert.not-found` | 404 | La alerta no existe en su cuenta. |
| `support.device.not-found` | 404 | El dispositivo no existe en su cuenta. |
| `support.incident.asset-required` | 400 | Seleccione el dispositivo o Water Point afectado, o la alerta que origina la incidencia. |
| `support.incident.invalid-transition` | 409 | La incidencia no puede pasar de {0} a {1}. |
| `support.incident.not-found` | 404 | La incidencia no existe en su cuenta. |
| `support.technician.not-found` | 404 | El técnico no existe o no se encuentra activo. |
| `support.technician.unavailable` | 409 | No hay técnicos disponibles en este momento. |
| `support.terms.not-found` | 404 | Los términos y condiciones no están disponibles. |
| `support.visit.not-covered` | 403 | Su plan vigente no cubre visitas técnicas. |

## 5. Notificaciones push

1. Obtenga el token de Firebase Cloud Messaging del celular y regístrelo **después de abrir la sesión de la cuenta** que debe recibir las alertas:
   `POST /api/v1/notifications/device-tokens`
   ```json
   { "token": "<token FCM>", "platform": "ANDROID" }
   ```
   Si el token ya estaba registrado en otra cuenta, pasa a la cuenta de la sesión. Vuelva a registrarlo cuando Firebase lo renueve.
2. Pruebe la recepción con `POST /api/v1/notifications/test` y consulte los intentos en `GET /api/v1/notifications/deliveries`.
3. Cada push trae un título y un texto visibles, más un bloque `data` para la navegación:

| `data.category` | Cuándo se envía | Claves de `data` | Pantalla a abrir |
|---|---|---|---|
| `LEAK_ALERT` | Se detecta o escala una fuga: dos lecturas seguidas sobre el umbral en 30 minutos | `alertId`, `waterPointId` | Detalle de la alerta: `GET /api/v1/alerts/{alertId}` |
| `TANK_LEVEL_LOW` | El nivel del tanque baja del nivel crítico | `tankId`, `levelPercentage` | Detalle del tanque: `GET /api/v1/tanks/{tankId}` |
| `DEVICE_OFFLINE` | Un medidor pasa 60 minutos sin reportar | `deviceId` | Detalle del medidor: `GET /api/v1/devices/{deviceId}` |
| `TEST` | Prueba manual | — | Ninguna |

Las alertas solo se notifican si su urgencia alcanza la mínima configurada en `PUT /api/v1/alerts/preferences`. Una alerta activa no se vuelve a notificar mientras su urgencia no suba.

## 6. Dashboard sin conexión (US14)

`GET /api/v1/monitoring/dashboard` responde con los encabezados `ETag` y `Last-Modified`.

1. Guarde la respuesta y el `ETag` en el almacenamiento local.
2. En la siguiente consulta, envíe `If-None-Match: <ETag guardado>`.
3. Si la respuesta es **304** (sin cuerpo), muestre la copia guardada. Si es **200**, reemplácela.
4. Sin conexión, muestre la copia con el texto "Actualizado hace X min", calculado con `generatedAt`.

Campos principales: `todayLiters`, `monthLiters`, `currentFlowLitersPerMinute`, `averagePressureBar`, `lastReadingAt`, `devices` (`total`, `online`, `lowBattery`, `offline`, `withoutWaterPoint`) y `waterPoints[]`, con la telemetría de cada punto de agua.

## 7. Flujos principales

### 7.1. Instalación de un medidor (titular)

| Paso | Endpoint |
|---|---|
| 1. Registrar el local | `POST /api/v1/locations` `{ "name": "Casa Salas", "address": "Av. Primavera 123" }` |
| 2. Registrar el punto de agua | `POST /api/v1/water-points` `{ "locationId": "…", "name": "Baño", "type": "BATHROOM" }` |
| 3. Registrar el medidor leído del QR | `POST /api/v1/devices` `{ "qrCodePayload": "…", "serialNumber": "QLIC-0001", "model": "QlicSense" }` |
| 4. Asignarlo al punto de agua | `PATCH /api/v1/devices/{id}/water-point` `{ "waterPointId": "…" }` |
| 5. Ponerle un nombre (opcional) | `PUT /api/v1/devices/{id}` `{ "name": "Medidor baño", "waterPointId": "…" }` |

### 7.2. Atención de una alerta

1. El usuario toca el push `LEAK_ALERT` y la aplicación abre `GET /api/v1/alerts/{alertId}`. La respuesta trae la urgencia, el volumen y el costo estimados y la acción recomendada.
2. El usuario marca la alerta como atendida: `PATCH /api/v1/alerts/{alertId}/acknowledge` con `{ "actionTaken": "Se cambió el empaque del inodoro" }`. Requiere `OWNER` u `OPERATOR`.
3. Si necesita ayuda, registra una incidencia desde la alerta con `POST /api/v1/incidents` y sigue su estado en `GET /api/v1/incidents/{id}/history`.

### 7.3. Reportes de consumo

| Pantalla | Endpoint |
|---|---|
| Consumo del periodo, costo y recomendaciones | `GET /api/v1/consumption/reports/period?from=2026-11-01&to=2026-11-26` |
| Comparación con el periodo anterior | `GET /api/v1/consumption/reports/comparison?from=…&to=…` |
| Texto para compartir | `GET /api/v1/consumption/reports/period/share?from=…&to=…` |
| Meta del mes | `PUT` / `GET /api/v1/consumption/goals/2026-11` |
| Tanques y predicción | `GET /api/v1/tanks`, `GET /api/v1/tanks/{id}`, `GET /api/v1/tanks/{id}/levels` |

## 8. Modo demostración (sin medidor físico)

Para las pruebas con usuarios y la exposición, un medidor asignado a un punto de agua puede generar lecturas simuladas:

```http
PUT /api/v1/devices/{id}/simulation
{ "enabled": true, "scenario": "LEAK" }
```

- `NORMAL` sigue un perfil de consumo diario realista y nunca genera alertas con el umbral por defecto.
- `LEAK` agrega un caudal continuo sobre el umbral. Con la segunda lectura simulada (unos 4 minutos) el API crea la alerta **CRITICAL** y envía el push `LEAK_ALERT` a todos los celulares registrados en la cuenta.
- `{ "enabled": false }` detiene la simulación.
- Un medidor sin punto de agua responde `409` (`monitoring.device.without-water-point`).
- El campo `simulationScenario` del dispositivo indica si está en simulación, para mostrar una etiqueta "Demo" en la aplicación.

Flujo de demostración sugerido:

1. El titular invita al miembro.
2. El miembro acepta y registra su celular.
3. El titular activa `LEAK`.
4. Llega el push a los dos celulares.
5. El miembro atiende la alerta.
6. El titular ve la alerta atendida y desactiva la simulación.

## 9. Valores de los enumerados

| Enumerado | Valores |
|---|---|
| `permission` | `OWNER`, `OPERATOR`, `VIEWER` |
| Estado del dispositivo | `ONLINE`, `OFFLINE`, `LOW_BATTERY`, `CALIBRATING` |
| `simulationScenario` | `NORMAL`, `LEAK`, `null` |
| Tipo de punto de agua | `KITCHEN`, `BATHROOM`, `LAUNDRY`, `GARDEN`, `TANK_INLET`, `MAIN_LINE`, `PRODUCTION`, `OTHER` |
| Urgencia de la alerta | `INFORMATIONAL`, `MODERATE`, `CRITICAL` |
| Estado de la alerta | `ACTIVE`, `ACKNOWLEDGED`, `RESOLVED` |
| Estado de la incidencia | `PENDING`, `VISIT_REQUESTED`, `IN_PROGRESS`, `RESOLVED` |
| Estado de la invitación | `PENDING`, `ACCEPTED`, `EXPIRED` |
| Plan | `TRIAL`, `HOME_BASIC`, `HOME_PRO`, `PYME_BUSINESS`, `PYME_ENTERPRISE` |
| Segmento del plan | `TRIAL`, `HOME`, `BUSINESS` |
| Estado de la suscripción | `TRIAL_ACTIVE`, `TRIAL_EXPIRED`, `ACTIVE`, `CANCELLED_PENDING_EXPIRATION`, `PAYMENT_REJECTED` |
| Plataforma del celular | `ANDROID`, `IOS`, `WEB` |

## 10. Probar sin la aplicación

1. Abra Swagger UI y ejecute `POST /api/v1/authentication/sign-in`.
2. Copie el `token`, pulse **Authorize** y péguelo (sin la palabra `Bearer`).
3. Todas las operaciones protegidas quedan disponibles con esa sesión.
