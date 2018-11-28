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
    public static <T, R> TypedMethodHandle0<T, R> findMethod(Class<T> clazz, String methodObfName, Class<R> returnType) {
        return new TypedMethodHandle0<>(findMethodHandleFromObfName(clazz, methodObfName, returnType), methodObfName);
    }

    @PublicApi
    public static <T, P1, R> TypedMethodHandle1<T, P1, R> findMethod(Class<T> clazz, String methodObfName, Class<R> returnType, Class<P1> p1) {
        return new TypedMethodHandle1<>(findMethodHandleFromObfName(clazz, methodObfName, returnType, p1), methodObfName);
    }

    @PublicApi
    public static <T, P1, P2, R> TypedMethodHandle2<T, P1, P2, R> findMethod(Class<T> clazz, String methodObfName, Class<R> returnType, Class<P1> p1, Class<P2> p2) {
        return new TypedMethodHandle2<>(findMethodHandleFromObfName(clazz, methodObfName, returnType, p1, p2), methodObfName);
    }

    @PublicApi
    public static <T, P1, P2, P3, R> TypedMethodHandle3<T, P1, P2, P3, R> findMethod(Class<T> clazz, String methodObfName, Class<R> returnType, Class<P1> p1, Class<P2> p2, Class<P3> p3) {
        return new TypedMethodHandle3<>(findMethodHandleFromObfName(clazz, methodObfName, returnType, p1, p2, p3), methodObfName);
    }

    @PublicApi
    public static <T, P1, P2, P3, P4, R> TypedMethodHandle4<T, P1, P2, P3, P4, R> findMethod(Class<T> clazz, String methodObfName, Class<R> returnType, Class<P1> p1, Class<P2> p2, Class<P3> p3, Class<P4> p4) {
        return new TypedMethodHandle4<>(findMethodHandleFromObfName(clazz, methodObfName, returnType, p1, p2, p3, p4), methodObfName);
    }

}
