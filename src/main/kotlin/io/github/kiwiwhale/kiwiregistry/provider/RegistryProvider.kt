package io.github.kiwiwhale.kiwiregistry.provider

import io.github.kiwiwhale.kiwiregistry.core.RegistryGraph
import java.util.concurrent.atomic.AtomicReference

/**
 * Thread-safe provider for an immutable [RegistryGraph].
 *
 * Graph replacement and updates are atomic.
 *
 * The provider does not modify installed graphs.
 *
 * @param initial Initial graph. Defaults to [RegistryGraph.EMPTY].
 */
class RegistryProvider(
    initial: RegistryGraph = RegistryGraph.EMPTY
) {

    private val current = AtomicReference(initial)

    /**
     * Returns the currently installed graph.
     *
     * The returned graph is an immutable snapshot.
     */
    val graph: RegistryGraph
        get() = current.get()

    /**
     * Atomically replaces the current graph.
     *
     * @param graph Replacement graph.
     * @return Previously installed graph.
     */
    fun replace(graph: RegistryGraph): RegistryGraph =
        current.getAndSet(graph)

    /**
     * Atomically updates the current graph.
     *
     * The transformation may execute multiple times
     * under concurrent contention.
     *
     * The transformation must be side-effect-free.
     *
     * If the transformation throws an exception,
     * the current graph remains unchanged by this operation.
     *
     * @param transform Transformation applied to the current graph.
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
     * Replaces the current graph only if its reference
     * is identical to [expected].
     *
     * @param expected Expected current graph.
     * @param replacement Replacement graph.
     * @return True if replacement succeeded.
     */
    fun compareAndReplace(
        expected: RegistryGraph,
        replacement: RegistryGraph
    ): Boolean =
        current.compareAndSet(expected, replacement)
}
