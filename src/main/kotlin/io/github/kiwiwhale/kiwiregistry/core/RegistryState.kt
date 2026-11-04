package io.github.kiwiwhale.kiwiregistry.core

import java.util.concurrent.atomic.AtomicReference

internal class RegistryState<T : Any>(
    initial: Map<String, T> = emptyMap()
) {
    private val reference =
        AtomicReference(
            RegistrySnapshot(initial)
        )

    fun get(): RegistrySnapshot<T> =
        reference.get()

    fun compareAndSet(
        expected: RegistrySnapshot<T>,
        updated: RegistrySnapshot<T>
    ): Boolean =
        reference.compareAndSet(expected, updated)

    fun update(
        transform: (RegistrySnapshot<T>) -> RegistrySnapshot<T>
    ) {
        while (true) {
            val current = reference.get()
            val updated = transform(current)

            if (reference.compareAndSet(current, updated)) {
                return
            }
        }
    }
}
