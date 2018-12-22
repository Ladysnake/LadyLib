package ladylib

import ladylib.reflection.TypedReflection
import ladylib.reflection.TypedReflection.createFieldRef
import ladylib.reflection.typed.RWTypedField
import org.apiguardian.api.API
import org.apiguardian.api.API.Status.EXPERIMENTAL
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
@API(status = EXPERIMENTAL, since = "2.6.2")
inline fun <reified R, reified T> reflected(obfName: String): ReflectionDelegate<R, T> {
    return ReflectionDelegate(createFieldRef(R::class.java, obfName, T::class.java))
}

/**
 * Example:
 * ```
 * val hash : String.() -> Int = findMethod("hashCode")
 * print("a".hash())
 * ```
 */
@API(status = EXPERIMENTAL, since = "2.6.2")
inline fun <reified T, reified R> findMethod0(methodObfName: String): T.() -> R {
    return TypedReflection.findMethod(T::class.java, methodObfName, R::class.java)::invoke
}

@API(status = EXPERIMENTAL, since = "2.6.2")
inline fun <reified T, reified R, reified P1> findMethod1(methodObfName: String): T.(P1) -> R {
    return TypedReflection.findMethod(T::class.java, methodObfName, R::class.java, P1::class.java)::invoke
}

@API(status = EXPERIMENTAL, since = "2.6.2")
inline fun <reified T, reified R, reified P1, reified P2> findMethod2(methodObfName: String): T.(P1, P2) -> R {
    return TypedReflection.findMethod(T::class.java, methodObfName, R::class.java, P1::class.java, P2::class.java)::invoke
}

@API(status = EXPERIMENTAL, since = "2.6.2")
inline fun <reified T, reified R, reified P1, reified P2, reified P3> findMethod3(methodObfName: String): T.(P1, P2, P3) -> R {
    return TypedReflection.findMethod(T::class.java, methodObfName, R::class.java, P1::class.java, P2::class.java, P3::class.java)::invoke
}

@API(status = EXPERIMENTAL, since = "2.6.2")
inline fun <reified T, reified R, reified P1, reified P2, reified P3, reified P4> findMethod4(methodObfName: String): T.(P1, P2, P3, P4) -> R {
    return TypedReflection.findMethod(T::class.java, methodObfName, R::class.java, P1::class.java, P2::class.java, P3::class.java, P4::class.java)::invoke
}

class ReflectionDelegate<in T, V>(private val field: RWTypedField<T, V>) : ReadWriteProperty<T, V> {
    override operator fun getValue(thisRef: T, property: KProperty<*>): V {
        return field[thisRef]
    }

    override operator fun setValue(thisRef: T, property: KProperty<*>, value: V) {
        field[thisRef] = value
    }
}