package io.github.deltacv.nodeeye.id

class IdElementContainer<T> : Iterable<T> {

    private val e = ArrayList<T?>()

    /**
     * Note that the element positions in this list won't necessarily match their ids
     */
    var elements = ArrayList<T>()
        private set

    fun nextId(element: () -> T) = lazy {
        nextId(element()).value
    }

    fun nextId(element: T) = lazy {
        e.add(element)
        elements.add(element)

        e.lastIndexOf(element)
    }

    fun nextId() = lazy {
        e.add(null)
        e.lastIndexOf(null)
    }

    fun removeId(id: Int) {
        elements.remove(e[id])
        e[id] = null
    }

    operator fun get(id: Int) = e[id]

    override fun iterator() = elements.listIterator()
}