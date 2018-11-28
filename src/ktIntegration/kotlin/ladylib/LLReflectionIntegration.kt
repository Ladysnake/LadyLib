package ladylib

import ladylib.misc.PublicApi
import ladylib.reflection.TypedReflection
import ladylib.reflection.TypedReflection.findGetter
import ladylib.reflection.TypedReflection.findSetter
import ladylib.reflection.typed.TypedGetter
import ladylib.reflection.typed.TypedSetter
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Reflects a field of type [T] from a class [R]
 *
 * Example:
 * ```
 * val String.hash : Int by reflected("hash")
 * ```
 */
@PublicApi
inline fun <reified R, reified T> reflected(obfName: String): ReflectionDelegate<R, T> {
    return ReflectionDelegate(findGetter(R::class.java, obfName, T::class.java), findSetter(R::class.java, obfName, T::class.java))
}

/**
 * Example:
 * ```
 * val hash : String.() -> Int = findMethod("hashCode")
 * print("a".hash())
 * ```
 */
@PublicApi
inline fun <reified T, reified R> findMethod0(methodObfName: String): T.() -> R {
    return TypedReflection.findMethod(T::class.java, methodObfName, R::class.java)::invoke
}

@PublicApi
inline fun <reified T, reified R, reified P1> findMethod1(methodObfName: String): T.(P1) -> R {
    return TypedReflection.findMethod(T::class.java, methodObfName, R::class.java, P1::class.java)::invoke
}

@PublicApi
inline fun <reified T, reified R, reified P1, reified P2> findMethod2(methodObfName: String): T.(P1, P2) -> R {
    return TypedReflection.findMethod(T::class.java, methodObfName, R::class.java, P1::class.java, P2::class.java)::invoke
}

@PublicApi
inline fun <reified T, reified R, reified P1, reified P2, reified P3> findMethod3(methodObfName: String): T.(P1, P2, P3) -> R {
    return TypedReflection.findMethod(T::class.java, methodObfName, R::class.java, P1::class.java, P2::class.java, P3::class.java)::invoke
}

@PublicApi
inline fun <reified T, reified R, reified P1, reified P2, reified P3, reified P4> findMethod4(methodObfName: String): T.(P1, P2, P3, P4) -> R {
    return TypedReflection.findMethod(T::class.java, methodObfName, R::class.java, P1::class.java, P2::class.java, P3::class.java, P4::class.java)::invoke
}

class ReflectionDelegate<in T, V>(private val getter: TypedGetter<T, V>, private val setter: TypedSetter<T, V>) : ReadWriteProperty<T, V> {
    override operator fun getValue(thisRef: T, property: KProperty<*>): V {
        return getter(thisRef)
    }

    override operator fun setValue(thisRef: T, property: KProperty<*>, value: V) {
        setter(thisRef, value)
    }
}