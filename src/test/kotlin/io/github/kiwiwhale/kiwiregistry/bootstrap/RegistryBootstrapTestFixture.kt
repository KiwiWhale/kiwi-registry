package io.github.kiwiwhale.kiwiregistry.bootstrap

import io.github.kiwiwhale.kiwiregistry.core.RegistryId
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey

internal object RegistryBootstrapTestFixture {

    val registryKey = RegistryKey.simple<String, String>(
        RegistryId("test", "items")
    )

    class TestHolder : RegistryHolder

    fun contribution(
        holder: RegistryHolder,
        builder: ContributionBuilder.() -> Unit
    ): Contribution {
        val contributionBuilder = ContributionBuilder()
        contributionBuilder.builder()
        return contributionBuilder.build(holder)
    }
}
