package site.neworld.observer

import java.util.*

private data class ClassHierarchyNode(
        var self: Int,
        var right: Int
)

class TypeFilterableList<T : Any>(private val elementType: Class<T>) : AbstractCollection<T>() {
    private val classMap = hashMapOf(Pair(elementType as Class<*>, ClassHierarchyNode(0, 1)))
    private val entityMap = mutableListOf(HashSet<T>())
    override var size = 0
        private set

    private fun classQuery(c: Class<*>) = classMap[c].let { it?.self ?: -1 }

    private fun classQuery(o: T) = classQuery(o.javaClass)

    private fun putClass(o: T): Int {
        var klass = o.javaClass as Class<*>
        val stack = ArrayList<Class<*>>()
        while (classQuery(klass) < 0) {
            stack.add(klass)
            klass = klass.superclass
        }
        // prepare the insert
        stack.reverse()
        val father = classMap[klass]!!
        val start = father.self
        // add in the missing slots after its parent node
        entityMap.addAll(start + 1, stack.map { HashSet<T>() })
        // make space for the new class id
        val required = stack.size
        for ((_, v) in classMap) v.apply {
            if (self > start) self += required
            if (right > start) right += required
        }
        // execute the class insert
        var baseId = start
        for (v in stack) classMap[v] = ClassHierarchyNode(++baseId, father.right)
        return baseId
    }

    override fun add(element: T) =
            if (elementType.isInstance(element)) classQuery(element)
                    .let { if (it < 0) putClass(element) else it }
                    .let { entityMap[it].add(element) }.also { if (it) ++size }
            else false

    override fun remove(element: T) = classQuery(element)
            .let { if (it > 0) entityMap[it].remove(element) else false }
            .also { if (it) --size }

    override fun contains(element: T) = classQuery(element)
            .let { if (it > 0) entityMap[it].contains(element) else false }

    @Suppress("UNCHECKED_CAST")
    fun <S> getAllOfType(type: Class<S>): Collection<S> {
        val node = classMap[type] ?: return emptyList()
        return (node.self until node.right).flatMap { entityMap[it] } as List<S>
    }

    override fun iterator() = object: MutableIterator<T> {
        private val iterBase = entityMap.iterator()
        private var iterSub = iterBase.next().iterator()

        init {
            tryForward()
        }

        private tailrec fun tryForward() {
            if (iterSub.hasNext()) return
            if (!iterBase.hasNext()) return
            iterSub = iterBase.next().iterator()
            if (!iterSub.hasNext()) tryForward()
        }

        override fun hasNext(): Boolean {
            if (iterSub.hasNext()) return true
            tryForward()
            return iterSub.hasNext()
        }

        override fun next(): T {
            val res = iterSub.next()
            tryForward()
            return res
        }

        override fun remove() = iterSub.remove()
    }

    fun all() = entityMap.flatten()
}
