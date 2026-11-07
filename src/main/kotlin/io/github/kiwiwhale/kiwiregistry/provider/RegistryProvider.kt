package io.github.kiwiwhale.kiwiregistry.provider

import io.github.kiwiwhale.kiwiregistry.core.RegistryGraph
import java.util.concurrent.atomic.AtomicReference

/**
 * Thread-safe provider for an immutable RegistryGraph.
 *
 * RegistryGraph instances are immutable snapshots.
 */
class RegistryProvider(
    initial: RegistryGraph = RegistryGraph.EMPTY
) {

    private val current =
        AtomicReference(initial)

    /**
     * Returns the current immutable graph.
     */
    val graph: RegistryGraph
        get() = current.get()

    /**
     * Atomically replaces the current graph.
     *
     * @return Previous graph.
     */
    fun replace(graph: RegistryGraph): RegistryGraph =
        current.getAndSet(graph)

    /**
     * Atomically updates the current graph.
     *
     * The transformation may execute multiple times
     * under concurrent contention.
     *
     * It must therefore be side-effect-free.
     *
     * @return Successfully installed graph.
     */
    fun update(
        transform: (RegistryGraph) -> RegistryGraph
    ): RegistryGraph {
        while (true) {
            val previous = current.get()
            val updated = transform(previous)

            if (current.compareAndSet(previous, updated)) {
                return updated
            }
        }
    }

    /**
     * Replaces the current graph only when the current
     * reference is identical to expected.
     */
    fun compareAndReplace(
        expected: RegistryGraph,
        replacement: RegistryGraph
    ): Boolean =
        current.compareAndSet(expected, replacement)
}
