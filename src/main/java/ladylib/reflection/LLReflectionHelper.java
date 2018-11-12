package ladylib.reflection;

import ladylib.misc.PublicApi;
import ladylib.reflection.LLMethodHandle.*;

import static ladylib.misc.ReflectionUtil.*;

public final class LLReflectionHelper {
    @PublicApi
    public static <T, R> Getter<T, R> findGetter(Class<T> clazz, String fieldObfName, Class<R> type) {
        return new Getter<>(findGetterFromObfName(clazz, fieldObfName, type), fieldObfName);
    }

    @PublicApi
    public static <T, R> Setter<T, R> findSetter(Class<T> clazz, String fieldObfName, Class<R> type) {
        return new Setter<>(findSetterFromObfName(clazz, fieldObfName, type), fieldObfName);
    }

    @PublicApi
    public static <T, R> LLMethodHandle0<T, R> findMethod(Class<T> clazz, String methodObfName, Class<R> returnType) {
        return new LLMethodHandle0<>(findMethodHandleFromObfName(clazz, methodObfName, returnType), methodObfName);
    }

    @PublicApi
    public static <T, P1, R> LLMethodHandle1<T, P1, R> findMethod(Class<T> clazz, String methodObfName, Class<R> returnType, Class<P1> p1) {
        return new LLMethodHandle1<>(findMethodHandleFromObfName(clazz, methodObfName, returnType, p1), methodObfName);
    }

    @PublicApi
    public static <T, P1, P2, R> LLMethodHandle2<T, P1, P2, R> findMethod(Class<T> clazz, String methodObfName, Class<R> returnType, Class<P1> p1, Class<P2> p2) {
        return new LLMethodHandle2<>(findMethodHandleFromObfName(clazz, methodObfName, returnType, p1, p2), methodObfName);
    }

    @PublicApi
    public static <T, P1, P2, P3, R> LLMethodHandle3<T, P1, P2, P3, R> findMethod(Class<T> clazz, String methodObfName, Class<R> returnType, Class<P1> p1, Class<P2> p2, Class<P3> p3) {
        return new LLMethodHandle3<>(findMethodHandleFromObfName(clazz, methodObfName, returnType, p1, p2, p3), methodObfName);
    }

    @PublicApi
    public static <T, P1, P2, P3, P4, R> LLMethodHandle4<T, P1, P2, P3, P4, R> findMethod(Class<T> clazz, String methodObfName, Class<R> returnType, Class<P1> p1, Class<P2> p2, Class<P3> p3, Class<P4> p4) {
        return new LLMethodHandle4<>(findMethodHandleFromObfName(clazz, methodObfName, returnType, p1, p2, p3, p4), methodObfName);
    }

}
