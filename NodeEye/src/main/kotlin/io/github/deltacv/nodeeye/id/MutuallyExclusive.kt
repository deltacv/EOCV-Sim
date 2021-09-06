package io.github.deltacv.nodeeye.id

fun mutuallyExclude(a: IdElementContainer<*>, b: IdElementContainer<*>) {
    a.nextIdCallback = {
        b.nextIdDontTrigger()
    }

    b.nextIdCallback = {
        b.nextIdDontTrigger()
    }
}