package io.github.kiwiwhale.kiwiregistry.provider

import io.github.kiwiwhale.kiwiregistry.core.RegistryGraph
import java.util.concurrent.atomic.AtomicReference

/**
 * Thread-safe provider for an immutable [RegistryGraph].
 *
 * The provider stores the current graph using an atomic reference.
 * Graph replacements and updates are performed atomically.
 *
 * RegistryGraph instances are immutable snapshots.
 *
 * @param initial Initial graph. Defaults to [RegistryGraph.EMPTY].
 */
class RegistryProvider(
    initial: RegistryGraph = RegistryGraph.EMPTY
) {

    private val current =
        AtomicReference(initial)

    /**
     * Returns the currently installed immutable graph.
     *
     * The returned reference represents a consistent snapshot
     * at the time of retrieval.
     */
    val graph: RegistryGraph
        get() = current.get()

    /**
     * Atomically replaces the current graph.
     *
     * @param graph New graph to install.
     * @return Previously installed graph.
     */
    fun replace(graph: RegistryGraph): RegistryGraph =
        current.getAndSet(graph)

    /**
     * Atomically updates the current graph.
     *
     * The transformation is executed using a compare-and-set loop.
     * Under concurrent contention, it may execute multiple times.
     *
     * The transformation must therefore be side-effect-free.
     *
     * If the transformation throws an exception, the current graph
     * remains unchanged by this operation.
     *
     * @param transform Function producing a replacement graph
     * from the currently installed graph.
     * @return Successfully installed graph.
     *
     * @throws Exception If the transformation fails.
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
     * Atomically replaces the current graph only if the current
     * reference is identical to [expected].
     *
     * Comparison uses reference identity rather than structural equality.
     *
     * @param expected Expected currently installed graph.
     * @param replacement Graph to install if the expectation matches.
     * @return True if the replacement succeeded.
     */
    fun compareAndReplace(
        expected: RegistryGraph,
        replacement: RegistryGraph
    ): Boolean =
        current.compareAndSet(expected, replacement)
}
