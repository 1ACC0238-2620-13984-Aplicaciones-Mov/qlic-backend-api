package com.wasd.qlic.shared.infrastructure.documentation

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Completes the OpenAPI document so every request and response has a description and an example:
 * fills the example of the fields that do not declare one in their resource, and documents the
 * standard errors of each operation (400, 401, 403, 404 and 409) with a Problem Details example
 * (RFC 9457) as the API really returns them.
 */
@Component
class OpenApiExamplesCustomizer : OpenApiCustomizer {

    override fun customise(openApi: OpenAPI) {
        val schemas = openApi.components?.schemas.orEmpty()
        schemas.forEach { (schemaName, schema) ->
            schema.properties?.forEach { (property, propertySchema) ->
                if (propertySchema.example == null && propertySchema.`get$ref`() == null) {
                    PROPERTY_EXAMPLES["$schemaName.$property"]?.let { propertySchema.setExample(it) }
                }
            }
        }
        val problemSchema: Schema<*> =
            if (schemas.containsKey(PROBLEM_DETAIL)) Schema<Any>().apply { `set$ref`("#/components/schemas/$PROBLEM_DETAIL") } else ObjectSchema()
        openApi.paths?.forEach { (path, pathItem) ->
            pathItem.readOperationsMap().forEach { (method, operation) -> documentErrors(path, method, operation, problemSchema) }
        }
    }

    private fun documentErrors(path: String, method: PathItem.HttpMethod, operation: Operation, problemSchema: Schema<*>) {
        val responses = operation.responses ?: ApiResponses().also { operation.responses = it }
        val isPublic = PUBLIC_PREFIXES.any { path.startsWith(it) }
        val isWrite = method in WRITE_METHODS

        if (isWrite || operation.requestBody != null || operation.parameters.orEmpty().isNotEmpty()) {
            responses.putIfAbsent("400", ApiResponse().description("The request or one of its fields is not valid"))
        }
        if (!isPublic) {
            responses.putIfAbsent(
                "401",
                ApiResponse().description(
                    "Missing, invalid or expired access token. Send the token of POST /api/v1/authentication/sign-in " +
                        "in the header Authorization: Bearer <token>."
                )
            )
        }
        if (!isPublic && isWrite && WRITE_PROTECTED_PREFIXES.any { path.startsWith(it) }) {
            responses.putIfAbsent("403", ApiResponse().description("The token has VIEWER permission and cannot modify the account"))
        }
        if (!isPublic && path.contains("{")) {
            responses.putIfAbsent("404", ApiResponse().description("The resource does not exist or belongs to another account"))
        }

        responses.forEach { (code, response) ->
            val status = code.toIntOrNull() ?: return@forEach
            if (status !in 400..499 || status == 401) return@forEach
            val example = problemExample(status, path)
            val content = response.content
            if (content == null || content.isEmpty()) {
                response.content = Content().addMediaType(
                    PROBLEM_JSON,
                    MediaType().schema(problemSchema).example(example)
                )
            } else {
                content.values.filter { it.example == null && it.examples.isNullOrEmpty() }.forEach { it.example = example }
            }
        }
    }

    private fun problemExample(status: Int, path: String): Map<String, Any> {
        val (code, detail) = when (status) {
            400 -> "error.validation" to "One or more fields are not valid."
            403 -> "iam.permission.denied" to "You do not have the permission required for this operation."
            404 -> NOT_FOUND_BY_PREFIX.entries.firstOrNull { path.startsWith(it.key) }?.value
                ?: ("error.not-found" to "The resource does not exist in your account.")
            409 -> CONFLICT_BY_PREFIX.entries.firstOrNull { path.startsWith(it.key) }?.value
                ?: ("error.conflict" to "The operation conflicts with the current state of the resource.")
            else -> "error.invalid-request" to "The request is not valid."
        }
        val example = linkedMapOf<String, Any>(
            "type" to "about:blank",
            "title" to (REASON_PHRASES[status] ?: "Client Error"),
            "status" to status,
            "detail" to detail,
            "instance" to path.replace(Regex("\\{[^}]+}"), SAMPLE_ID),
            "code" to code
        )
        if (status == 400) example["errors"] = mapOf("name" to "The name is required.")
        return example
    }

    companion object {
        private const val PROBLEM_DETAIL = "ProblemDetail"
        private const val PROBLEM_JSON = "application/problem+json"
        private const val SAMPLE_ID = "3fa85f64-5717-4562-b3fc-2c963f66afa6"

        private val WRITE_METHODS = setOf(
            PathItem.HttpMethod.POST, PathItem.HttpMethod.PUT, PathItem.HttpMethod.PATCH, PathItem.HttpMethod.DELETE
        )

        private val PUBLIC_PREFIXES = listOf("/api/v1/authentication", "/api/v1/terms-of-service", "/api/v1/plans")

        private val WRITE_PROTECTED_PREFIXES = listOf(
            "/api/v1/devices", "/api/v1/locations", "/api/v1/water-points", "/api/v1/tanks", "/api/v1/alerts",
            "/api/v1/consumption", "/api/v1/incidents", "/api/v1/subscriptions"
        )

        private val REASON_PHRASES = mapOf(
            400 to "Bad Request", 402 to "Payment Required", 403 to "Forbidden", 404 to "Not Found", 409 to "Conflict"
        )

        private val NOT_FOUND_BY_PREFIX = linkedMapOf(
            "/api/v1/devices" to ("monitoring.device.not-found" to "The device does not exist in your account."),
            "/api/v1/locations" to ("monitoring.location.not-found" to "The location does not exist in your account."),
            "/api/v1/water-points" to ("monitoring.water-point.not-found" to "The Water Point does not exist in your account."),
            "/api/v1/tanks" to ("monitoring.tank.not-found" to "The tank does not exist in your account."),
            "/api/v1/alerts/recipients" to ("alerting.recipient.not-found" to "The alert recipient does not exist in your account."),
            "/api/v1/alerts" to ("alerting.alert.not-found" to "The alert does not exist in your account."),
            "/api/v1/incidents" to ("support.incident.not-found" to "The incident does not exist in your account."),
            "/api/v1/subscriptions" to ("subscriptions.subscription.not-found" to "The subscription does not exist in your account."),
            "/api/v1/invitations" to ("iam.invitation.not-found" to "The invitation does not exist."),
            "/api/v1/authorized-users" to ("iam.authorized-user.not-found" to "The authorized user does not exist in your account."),
            "/api/v1/consumption/goals" to ("consumption.goal.not-found" to "No sustainability goal was defined for that month."),
            "/api/v1/notifications" to ("notifications.device-token.not-found" to "The device is not registered in your account.")
        )

        private val CONFLICT_BY_PREFIX = linkedMapOf(
            "/api/v1/devices" to ("monitoring.device.already-registered" to
                "The device with serial number QS-2026-000123 is already registered in another account."),
            "/api/v1/water-points" to ("monitoring.water-point.limit-reached" to
                "Your plan allows up to 6 Water Points. Upgrade your plan to monitor more."),
            "/api/v1/locations" to ("monitoring.location.has-water-points" to
                "The location still has Water Points. Move or delete them before deleting the location."),
            "/api/v1/authentication" to ("iam.email.already-registered" to "The email claudia.morales@example.com is already registered."),
            "/api/v1/invitations" to ("iam.invitation.already-invited" to
                "The person jorge.quispe@example.com already has access or a pending invitation.")
        )

        /** Examples of the fields that do not declare one in their resource, by "Schema.property". */
        private val PROPERTY_EXAMPLES: Map<String, Any> = mapOf(
            "LocationResource.name" to "Restaurante Don Lucho - Miraflores",
            "LocationResource.address" to "Av. Larco 345, Miraflores",
            "LocationResource.waterPointsCount" to 3,
            "WaterPointResource.name" to "Cocina principal",
            "WaterPointResource.locationName" to "Restaurante Don Lucho - Miraflores",
            "WaterPointResource.devicesCount" to 1,
            "WaterTankResource.name" to "Tanque elevado",
            "WaterTankResource.locationName" to "Restaurante Don Lucho - Miraflores",
            "RegisterDeviceRequest.qrCodePayload" to "QLIC|QS-2026-000123|QlicSense",
            "RegisterDeviceRequest.serialNumber" to "QS-2026-000123",
            "RegisterDeviceRequest.model" to "QlicSense",
            "IoTDeviceResource.serialNumber" to "QS-2026-000123",
            "IoTDeviceResource.model" to "QlicSense",
            "IoTDeviceResource.batteryPercentage" to 87,
            "ConsumptionReadingResource.volumeLiters" to BigDecimal("12.50"),
            "ConsumptionReadingResource.flowRateLitersPerMinute" to BigDecimal("7.5"),
            "ConsumptionReadingResource.pressureBar" to BigDecimal("2.2"),
            "DeviceStatusSummaryResource.total" to 4,
            "DeviceStatusSummaryResource.online" to 3,
            "DeviceStatusSummaryResource.lowBattery" to 1,
            "DeviceStatusSummaryResource.offline" to 0,
            "DeviceStatusSummaryResource.withoutWaterPoint" to 0,
            "WaterPointTelemetryResource.name" to "Cocina principal",
            "WaterPointTelemetryResource.locationName" to "Restaurante Don Lucho - Miraflores",
            "ConfigureThresholdRequest.period" to "HOURLY",
            "ConfigureThresholdRequest.customLimitLitersPerHour" to BigDecimal("150.00"),
            "ConfigureThresholdRequest.baselineLitersPerHour" to BigDecimal("100.00"),
            "ConsumptionThresholdResource.period" to "HOURLY",
            "ConsumptionThresholdResource.customLimitLitersPerHour" to BigDecimal("150.00"),
            "ConsumptionThresholdResource.baselineLitersPerHour" to BigDecimal("100.00"),
            "ConsumptionThresholdResource.effectiveLimitLitersPerHour" to BigDecimal("150.00"),
            "ConfigureNotificationPreferenceRequest.groupingWindowMinutes" to 30,
            "NotificationPreferenceResource.groupingWindowMinutes" to 30,
            "AddAuthorizedRecipientRequest.fullName" to "Jorge Quispe",
            "AddAuthorizedRecipientRequest.email" to "jorge.quispe@example.com",
            "AddAuthorizedRecipientRequest.devicePushToken" to "fcm-registration-token-of-the-device",
            "AuthorizedRecipientResource.fullName" to "Jorge Quispe",
            "AuthorizedRecipientResource.email" to "jorge.quispe@example.com",
            "AuthorizedRecipientResource.devicePushToken" to "fcm-registration-token-of-the-device",
            "EvaluateReadingRequest.currentFlowLitersPerHour" to BigDecimal("180.00"),
            "EvaluateReadingRequest.thresholdLimit" to BigDecimal("125.00"),
            "LeakAlertResource.estimatedVolumeLiters" to BigDecimal("45.00"),
            "LeakAlertResource.estimatedCostSoles" to BigDecimal("0.23"),
            "LeakAlertResource.recommendedAction" to "Close the stopcock of the Water Point and check for visible leaks.",
            "LeakAlertResource.acknowledgedAction" to "Se cerró la llave de paso y se llamó al gasfitero",
            "LeakAlertResource.notifiedRecipientsCount" to 2,
            "AcknowledgeAlertRequest.actionTaken" to "Se cerró la llave de paso y se llamó al gasfitero",
            "PurchaseSubscriptionRequest.paymentMethod" to "YAPE_PLIN_OR_CARD",
            "PurchaseSubscriptionResponse.message" to "Suscripción activada y funcionalidades del plan habilitadas.",
            "PaymentRecordResource.amountSoles" to BigDecimal("49.90"),
            "PaymentRecordResource.paymentMethod" to "YAPE_PLIN_OR_CARD",
            "SubscriptionResource.maxWaterPointsAllowed" to 6,
            "PushDeliveryResource.title" to "Possible leak detected",
            "PushDeliveryResource.providerMessageId" to "projects/qlic/messages/0:1732550400000000",
            "IncidentResource.description" to "Fuga debajo del lavadero de la cocina",
            "IncidentResource.resolution" to "Se reemplazó la llave de paso de la cocina",
            "TechnicalVisitResource.notes" to "Tocar el timbre del segundo piso",
            "AuthenticationResource.token" to "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJxbGljLWJhY2tlbmQtYXBpIn0.c2lnbmF0dXJl",
            "PasswordResetRequestedResource.message" to "If the email is registered, we sent a link to reset the password.",
            "PasswordResetRequestedResource.resetToken" to "7d2c4e1a9b0f4c3e8a6d5b2f1e0c9a8b",
            "PasswordResetConfirmationResource.token" to "7d2c4e1a9b0f4c3e8a6d5b2f1e0c9a8b",
            "TermsOfServiceResource.title" to "Términos y condiciones del servicio Qlic",
            "TermsOfServiceResource.content" to "1. Objeto del servicio. Qlic brinda el monitoreo del consumo de agua...",
            "SustainabilityGoalResource.message" to "You are on track: you used 57% of your goal with 80% of the month elapsed.",
            "SavingRecommendationResource.waterPointName" to "Cocina principal",
            "SavingRecommendationResource.message" to
                "Cocina principal consumed 19% more than its usual daily average. Check the taps and the dishwasher.",
            "WaterPointConsumptionResource.waterPointName" to "Cocina principal",
            "PeriodConsumptionReportResource.message" to "There are no readings in the selected period.",
            "PeriodComparisonResource.message" to "Your consumption increased 13.16% compared with the previous period.",
            "ShareableReportResource.title" to "Qlic - Consumption report 2026-11-01 to 2026-11-30",
            "ShareableReportResource.text" to
                "Total: 1946.30 L (1.95 m3). Estimated cost: S/ 9.73. Highest consumption: Cocina principal (42.15%).",
            "ActivityScheduleResource.activeDays" to listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")
        )
    }
}
