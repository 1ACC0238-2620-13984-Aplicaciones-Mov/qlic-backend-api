package com.wasd.qlic.support.infrastructure.seeding

import com.wasd.qlic.support.domain.model.Technician
import com.wasd.qlic.support.domain.model.TermsOfService
import com.wasd.qlic.support.domain.repositories.TechnicianRepository
import com.wasd.qlic.support.domain.repositories.TermsOfServiceRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Loads the reference data of Support when the database is empty: the support technicians
 * (US36) and the current terms of service in English and Latin American Spanish (US38).
 */
@Component
class SupportDataSeeder(
    private val technicianRepository: TechnicianRepository,
    private val termsOfServiceRepository: TermsOfServiceRepository
) : ApplicationRunner {

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (technicianRepository.count() == 0L) {
            technicianRepository.save(Technician(fullName = "Miguel Torres", phone = "+51987000111", zone = "Lima Centro"))
            technicianRepository.save(Technician(fullName = "Ana Paredes", phone = "+51987000222", zone = "Lima Sur"))
            technicianRepository.save(Technician(fullName = "Diego Salazar", phone = "+51987000333", zone = "Lima Norte"))
        }
        if (termsOfServiceRepository.count() == 0L) {
            termsOfServiceRepository.save(
                TermsOfService(version = VERSION, language = "en", title = EN_TITLE, content = EN_CONTENT, updatedOn = UPDATED_ON)
            )
            termsOfServiceRepository.save(
                TermsOfService(version = VERSION, language = "es", title = ES_TITLE, content = ES_CONTENT, updatedOn = UPDATED_ON)
            )
        }
    }

    companion object {
        private const val VERSION = "1.0"
        private val UPDATED_ON: LocalDate = LocalDate.of(2026, 10, 6)

        private const val EN_TITLE = "Qlic Terms and Conditions"
        private val EN_CONTENT = """
            1. Acceptance of the terms. By creating a Qlic account you accept these Terms and Conditions. If you do not agree with them, you must not use the service. Qlic is provided by WASD, a startup formed by students of the Universidad Peruana de Ciencias Aplicadas.
            2. The service. Qlic is a water monitoring service for homes and small businesses. It combines IoT measurement devices installed at Water Points, a mobile application and a web platform to show water consumption, detect unusual consumption and possible leaks, and send alerts and reports.
            3. Account and security. You must provide accurate information when you register and keep it up to date. You are responsible for keeping your password confidential and for all activity in your account. Passwords are stored encrypted and are never shown to Qlic staff. Notify us immediately if you suspect unauthorized access.
            4. Authorized users. The account holder may invite other people to access the account with the permission level they choose. The account holder is responsible for the people they invite and may revoke their access at any time. The number of authorized users depends on the subscription plan.
            5. Devices and installation. Measurements depend on the correct installation, power supply and connectivity of the devices. You must not tamper with the devices or use them for purposes other than water monitoring.
            6. Alerts and limitations. Alerts, estimates of volume and cost, and recommendations are informative and are based on the readings received from your devices. Qlic does not close valves or act on your installation, and it does not guarantee that every leak will be detected or that damage or costs will be avoided. In an emergency, act according to the recommended action and contact a qualified technician.
            7. Personal data. Qlic collects your name, email, phone number, the data of your devices and Water Points, and your consumption readings. This data is used only to provide the service: showing consumption, generating alerts and reports, and providing support. Qlic does not sell your data and does not share it with third parties, except with the providers needed to operate the service, such as push notification and payment providers, which receive only the minimum data required. Push notifications do not include sensitive account data. You may request access to, correction of or deletion of your data at any time, in accordance with Peruvian Law N.° 29733 on Personal Data Protection.
            8. Plans and payments. Paid plans are billed according to the conditions shown before contracting them. You may change or cancel your plan from the application; the cancellation takes effect at the end of the current billing period.
            9. Availability. We work to keep the service available, but it may be interrupted for maintenance, failures of third-party providers or causes beyond our control. Planned maintenance will be announced in advance whenever possible.
            10. Intellectual property. The Qlic brand, the application and its content belong to WASD. Your consumption data belongs to you.
            11. Termination. You may delete your account at any time. We may suspend accounts that breach these terms or use the service fraudulently, after notifying the account holder whenever possible.
            12. Changes to these terms. We will notify you of relevant changes through the application or by email before they take effect. Continuing to use the service after that date means that you accept the new terms.
            13. Applicable law and contact. These terms are governed by the laws of Peru. For questions or requests about these terms or your personal data, contact us through the Support section of the application.
        """.trimIndent()

        private const val ES_TITLE = "Términos y condiciones de Qlic"
        private val ES_CONTENT = """
            1. Aceptación de los términos. Al crear una cuenta de Qlic usted acepta estos Términos y Condiciones. Si no está de acuerdo con ellos, no debe utilizar el servicio. Qlic es provisto por WASD, una startup conformada por estudiantes de la Universidad Peruana de Ciencias Aplicadas.
            2. El servicio. Qlic es un servicio de monitoreo de agua para hogares y pequeños negocios. Combina dispositivos IoT de medición instalados en los Water Points, una aplicación móvil y una plataforma web para mostrar el consumo de agua, detectar consumos inusuales y posibles fugas, y enviar alertas y reportes.
            3. Cuenta y seguridad. Usted debe proporcionar información veraz al registrarse y mantenerla actualizada. Es responsable de mantener la confidencialidad de su contraseña y de toda la actividad de su cuenta. Las contraseñas se almacenan cifradas y nunca se muestran al personal de Qlic. Comuníquenos de inmediato si sospecha de un acceso no autorizado.
            4. Usuarios autorizados. El titular de la cuenta puede invitar a otras personas a acceder a la cuenta con el nivel de permiso que elija. El titular es responsable de las personas que invita y puede revocar su acceso en cualquier momento. La cantidad de usuarios autorizados depende del plan de suscripción.
            5. Dispositivos e instalación. Las mediciones dependen de la correcta instalación, alimentación eléctrica y conectividad de los dispositivos. Usted no debe manipular los dispositivos ni utilizarlos con fines distintos al monitoreo de agua.
            6. Alertas y limitaciones. Las alertas, las estimaciones de volumen y costo, y las recomendaciones son informativas y se basan en las lecturas recibidas de sus dispositivos. Qlic no cierra válvulas ni actúa sobre su instalación, y no garantiza que se detecten todas las fugas ni que se eviten daños o costos. En caso de emergencia, siga la acción recomendada y contacte a un técnico calificado.
            7. Datos personales. Qlic recopila su nombre, correo, número de teléfono, los datos de sus dispositivos y Water Points, y sus lecturas de consumo. Estos datos se utilizan únicamente para prestar el servicio: mostrar el consumo, generar alertas y reportes, y brindar soporte. Qlic no vende sus datos ni los comparte con terceros, salvo con los proveedores necesarios para operar el servicio, como los de notificaciones push y pagos, que reciben solo los datos mínimos requeridos. Las notificaciones push no incluyen datos sensibles de la cuenta. Usted puede solicitar el acceso, la rectificación o la eliminación de sus datos en cualquier momento, conforme a la Ley N.° 29733, Ley de Protección de Datos Personales.
            8. Planes y pagos. Los planes de pago se facturan según las condiciones mostradas antes de contratarlos. Usted puede cambiar o cancelar su plan desde la aplicación; la cancelación surte efecto al término del periodo de facturación vigente.
            9. Disponibilidad. Trabajamos para mantener el servicio disponible, pero este puede interrumpirse por mantenimiento, fallas de proveedores externos o causas fuera de nuestro control. El mantenimiento programado se anunciará con anticipación siempre que sea posible.
            10. Propiedad intelectual. La marca Qlic, la aplicación y su contenido pertenecen a WASD. Sus datos de consumo le pertenecen a usted.
            11. Terminación. Usted puede eliminar su cuenta en cualquier momento. Podemos suspender las cuentas que incumplan estos términos o utilicen el servicio de forma fraudulenta, notificando previamente al titular siempre que sea posible.
            12. Cambios en estos términos. Le notificaremos los cambios relevantes mediante la aplicación o por correo antes de que entren en vigencia. Continuar utilizando el servicio después de esa fecha implica la aceptación de los nuevos términos.
            13. Ley aplicable y contacto. Estos términos se rigen por las leyes del Perú. Para consultas o solicitudes sobre estos términos o sobre sus datos personales, contáctenos mediante la sección Soporte de la aplicación.
        """.trimIndent()
    }
}
