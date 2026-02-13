package org.woen.utils.atomic

import java.util.concurrent.atomic.AtomicIntegerArray
import java.util.concurrent.atomic.AtomicLongArray
import java.util.concurrent.atomic.AtomicReferenceArray



/**
 * Creates a new [AtomicIntegerArray] containing a copy of all elements
 * from this array. The copy is independent; changes to the original do
 * not affect the copy, and vice versa.
 *
 * @return a fresh [AtomicIntegerArray] with the same elements.
 */
fun AtomicIntegerArray.fullCopy(): AtomicIntegerArray {
    val values = IntArray(length()) { i -> this[i] }
    return AtomicIntegerArray(values)
}


/**
 * Creates a new [AtomicLongArray] containing a copy of all elements
 * from this array. The copy is independent; changes to the original do
 * not affect the copy, and vice versa.
 *
 * @return a fresh [AtomicLongArray] with the same elements.
 */
fun AtomicLongArray.fullCopy(): AtomicLongArray {
    val values = LongArray(length()) { i -> this[i] }
    return AtomicLongArray(values)
}


/**
 * Creates a new [AtomicReferenceArray] containing a **shallow copy** of
 * all elements from this array. The references themselves are copied,
 * meaning that both arrays refer to the same objects. If you need a deep
 * copy (copying the referenced objects), you must implement it separately.
 *
 * @return a fresh [AtomicReferenceArray] with the same object references.
 */
inline fun <reified V> AtomicReferenceArray<V>.fullCopy(): AtomicReferenceArray<V> {
    val array = Array<V?>(length()) { i -> this[i] }
    return AtomicReferenceArray(array)
}




/**
 * Returns `true` if this [AtomicIntegerArray] contains the specified [element].
 */
fun AtomicIntegerArray.contains(element: Int): Boolean {
    for (i in 0 until length()) {
        if (this[i] == element) return true
    }
    return false
}

/**
 * Returns `true` if this [AtomicLongArray] contains the specified [element].
 */
fun AtomicLongArray.contains(element: Long): Boolean {
    for (i in 0 until length()) {
        if (this[i] == element) return true
    }
    return false
}

/**
 * Returns `true` if this [AtomicReferenceArray] contains the specified [element].
 * The comparison uses structural equality (`==`), which translates to `equals()`
 * for non‑null values and handles `null` correctly.
 */
fun <V> AtomicReferenceArray<V>.contains(element: V): Boolean {
    for (i in 0 until length()) {
        if (this[i] == element) return true
    }
    return false
}



/**
 * Returns `true` if this [AtomicIntegerArray] has no elements.
 */
fun AtomicIntegerArray.isEmpty(): Boolean = length() == 0

/**
 * Returns `true` if this [AtomicLongArray] has no elements.
 */
fun AtomicLongArray.isEmpty(): Boolean = length() == 0

/**
 * Returns `true` if this [AtomicReferenceArray] has no elements.
 */
fun <V> AtomicReferenceArray<V>.isEmpty(): Boolean = length() == 0