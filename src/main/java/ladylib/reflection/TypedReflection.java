package ladylib.reflection;

import ladylib.misc.PublicApi;
import ladylib.reflection.typed.*;

import static ladylib.misc.ReflectionUtil.*;

public final class TypedReflection {
    private TypedReflection() { throw new AssertionError(); }

    @PublicApi
    public static <T, R> TypedGetter<T, R> findGetter(Class<T> clazz, String fieldObfName, Class<R> type) {
        return new TypedGetter<>(findGetterFromObfName(clazz, fieldObfName, type), fieldObfName);
    }

    @PublicApi
    public static <T, R> TypedSetter<T, R> findSetter(Class<T> clazz, String fieldObfName, Class<R> type) {
        return new TypedSetter<>(findSetterFromObfName(clazz, fieldObfName, type), fieldObfName);
    }

    @PublicApi
    public static <T, R> RWTypedField<T, R> createFieldRef(Class<T> clazz, String fieldObfName, Class<R> type) {
        TypedGetter<T, R> getter = findGetter(clazz, fieldObfName, type);
        TypedSetter<T, R> setter = findSetter(clazz, fieldObfName, type);
        return new RWTypedField<>(getter, setter);
    }

    @PublicApi
    public static <T, R> TypedMethod0<T, R> findMethod(Class<T> clazz, String methodObfName, Class<R> returnType) {
        return new TypedMethod0<>(findMethodHandleFromObfName(clazz, methodObfName, returnType), methodObfName);
    }

    @PublicApi
    public static <T, P1, R> TypedMethod1<T, P1, R> findMethod(Class<T> clazz, String methodObfName, Class<R> returnType, Class<P1> p1) {
        return new TypedMethod1<>(findMethodHandleFromObfName(clazz, methodObfName, returnType, p1), methodObfName);
    }

    @PublicApi
    public static <T, P1, P2, R> TypedMethod2<T, P1, P2, R> findMethod(Class<T> clazz, String methodObfName, Class<R> returnType, Class<P1> p1, Class<P2> p2) {
        return new TypedMethod2<>(findMethodHandleFromObfName(clazz, methodObfName, returnType, p1, p2), methodObfName);
    }

    @PublicApi
    public static <T, P1, P2, P3, R> TypedMethod3<T, P1, P2, P3, R> findMethod(Class<T> clazz, String methodObfName, Class<R> returnType, Class<P1> p1, Class<P2> p2, Class<P3> p3) {
        return new TypedMethod3<>(findMethodHandleFromObfName(clazz, methodObfName, returnType, p1, p2, p3), methodObfName);
    }

    @PublicApi
    public static <T, P1, P2, P3, P4, R> TypedMethod4<T, P1, P2, P3, P4, R> findMethod(Class<T> clazz, String methodObfName, Class<R> returnType, Class<P1> p1, Class<P2> p2, Class<P3> p3, Class<P4> p4) {
        return new TypedMethod4<>(findMethodHandleFromObfName(clazz, methodObfName, returnType, p1, p2, p3, p4), methodObfName);
    }

}
