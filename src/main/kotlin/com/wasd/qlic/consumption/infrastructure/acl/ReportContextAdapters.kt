package com.wasd.qlic.consumption.infrastructure.acl

import com.wasd.qlic.consumption.domain.model.ServiceCategory
import com.wasd.qlic.consumption.domain.services.ServiceCategoryProvider
import com.wasd.qlic.consumption.domain.services.TariffCatalog
import com.wasd.qlic.consumption.domain.services.WaterPointCatalogProvider
import com.wasd.qlic.consumption.domain.services.WaterPointInfo
import com.wasd.qlic.monitoring.domain.repositories.WaterPointRepository
import com.wasd.qlic.subscriptions.domain.model.PlanType
import com.wasd.qlic.subscriptions.domain.repositories.SubscriptionRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

/** Anti-corruption layer with Device Monitoring: names and kinds of the Water Points of the account. */
@Component
class WaterPointCatalogAdapter(
    private val waterPointRepository: WaterPointRepository
) : WaterPointCatalogProvider {
    override fun waterPoints(accountId: UUID): Map<UUID, WaterPointInfo> =
        waterPointRepository.findByAccountId(accountId).associate { it.id to WaterPointInfo(it.name, it.type.name) }
}

/** Anti-corruption layer with Subscriptions: PYME plans are commercial users, the rest are domestic. */
@Component
class PlanServiceCategoryAdapter(
    private val subscriptionRepository: SubscriptionRepository
) : ServiceCategoryProvider {
    override fun defaultCategory(accountId: UUID): ServiceCategory =
        when (subscriptionRepository.findByAccountId(accountId)?.currentPlan) {
            PlanType.PYME_BUSINESS, PlanType.PYME_ENTERPRISE -> ServiceCategory.COMMERCIAL
            else -> ServiceCategory.DOMESTIC
        }
}

/**
 * Reference tariffs per cubic meter (water and sewerage) configured for the deployment. The real
 * structure of the provider is tiered by consumption range, so the result is always an estimate.
 */
@Component
class ConfiguredTariffCatalog(
    @Value("\${qlic.consumption.tariff.domestic-per-m3:5.00}") private val domestic: BigDecimal,
    @Value("\${qlic.consumption.tariff.commercial-per-m3:9.50}") private val commercial: BigDecimal
) : TariffCatalog {
    override fun pricePerCubicMeter(category: ServiceCategory): BigDecimal = when (category) {
        ServiceCategory.DOMESTIC -> domestic
        ServiceCategory.COMMERCIAL -> commercial
    }
}
