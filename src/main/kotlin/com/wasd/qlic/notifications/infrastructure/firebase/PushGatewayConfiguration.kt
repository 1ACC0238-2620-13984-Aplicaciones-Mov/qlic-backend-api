package com.wasd.qlic.notifications.infrastructure.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.Notification
import com.wasd.qlic.notifications.domain.model.PushDeliveryStatus
import com.wasd.qlic.notifications.domain.model.PushMessage
import com.wasd.qlic.notifications.domain.services.GatewayResult
import com.wasd.qlic.notifications.domain.services.PushNotificationGateway
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.ByteArrayInputStream
import java.util.Base64

/**
 * Selects the push provider: Firebase Cloud Messaging when the service account of the Firebase project is
 * configured (FIREBASE_CREDENTIALS_BASE64), otherwise a gateway that records the attempt as DISABLED so the
 * rest of the flow works in development and in the tests.
 */
@Configuration
class PushGatewayConfiguration {
    private val log = LoggerFactory.getLogger(PushGatewayConfiguration::class.java)

    @Bean
    fun pushNotificationGateway(
        @Value("\${qlic.push.firebase.credentials-base64:}") credentialsBase64: String
    ): PushNotificationGateway {
        if (credentialsBase64.isBlank()) {
            log.warn("Firebase credentials are not configured: push notifications are recorded as DISABLED")
            return DisabledPushNotificationGateway()
        }
        val credentials = GoogleCredentials.fromStream(ByteArrayInputStream(Base64.getDecoder().decode(credentialsBase64.trim())))
        val app = FirebaseApp.getApps().firstOrNull { it.name == APP_NAME }
            ?: FirebaseApp.initializeApp(FirebaseOptions.builder().setCredentials(credentials).build(), APP_NAME)
        log.info("Push notifications enabled with Firebase Cloud Messaging")
        return FirebasePushNotificationGateway(FirebaseMessaging.getInstance(app))
    }

    private companion object {
        const val APP_NAME = "qlic"
    }
}

/** TS06: sends the message through Firebase Cloud Messaging and translates the answer of the provider. */
class FirebasePushNotificationGateway(private val messaging: FirebaseMessaging) : PushNotificationGateway {

    override fun send(token: String, message: PushMessage): GatewayResult {
        val firebaseMessage = Message.builder()
            .setToken(token)
            .setNotification(Notification.builder().setTitle(message.title).setBody(message.body).build())
            .putAllData(message.data + ("category" to message.category.name))
            .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
            .build()
        return try {
            GatewayResult(PushDeliveryStatus.SENT, providerMessageId = messaging.send(firebaseMessage))
        } catch (exception: FirebaseMessagingException) {
            val code = exception.messagingErrorCode
            val status = if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT ||
                code == MessagingErrorCode.SENDER_ID_MISMATCH
            ) PushDeliveryStatus.INVALID_TOKEN else PushDeliveryStatus.FAILED
            GatewayResult(status, errorCode = code?.name ?: exception.errorCode?.name)
        }
    }
}

/** Used when Firebase is not configured: nothing leaves the server and the attempt is recorded as DISABLED. */
class DisabledPushNotificationGateway : PushNotificationGateway {
    override fun send(token: String, message: PushMessage): GatewayResult = GatewayResult(PushDeliveryStatus.DISABLED)
}
