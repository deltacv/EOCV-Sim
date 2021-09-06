package io.github.deltacv.nodeeye.id

interface IdElement {
    val id: Int
}

interface DrawableIdElement : IdElement {

    fun draw()

    fun delete()

    fun onEnable() { }

    fun enable(): DrawableIdElement {
        ::id.get()
        onEnable()
        return this
    }

}