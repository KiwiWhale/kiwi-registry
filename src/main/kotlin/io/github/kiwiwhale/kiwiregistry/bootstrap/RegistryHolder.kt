package io.github.kiwiwhale.kiwiregistry.bootstrap

interface RegistryHolder {

    fun registryBootstrap(
        bootstrap: BootstrapHolder,
        builder: ContributionBuilder.() -> Unit
    ) {
        val contributionBuilder = ContributionBuilder().apply(builder)

        BootstrapHolderState
            .get(bootstrap)
            .submit(contributionBuilder.build(this))
    }
}
