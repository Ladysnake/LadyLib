package ladylib.reflection;

import ladylib.reflection.LLMethodHandle.*;

import static ladylib.misc.ReflectionUtil.*;

public final class LLReflectionHelper {
    public static <T> Getter<T> findGetter(Class<?> clazz, String fieldObfName, Class<T> type) {
        return new Getter<>(findGetterFromObfName(clazz, fieldObfName, type), fieldObfName);
    }

    public static <T> Setter<T> findSetter(Class<?> clazz, String fieldObfName, Class<T> type) {
        return new Setter<>(findSetterFromObfName(clazz, fieldObfName, type), fieldObfName);
    }

    public static <R> LLMethodHandle0<R> findMethod(Class<?> clazz, String methodObfName, Class<R> returnType) {
        return new LLMethodHandle0<>(findMethodHandleFromObfName(clazz, methodObfName, returnType), methodObfName);
    }

    public static <P1, R> LLMethodHandle1<P1, R> findMethod(Class<?> clazz, String methodObfName, Class<R> returnType, Class<P1> p1) {
        return new LLMethodHandle1<>(findMethodHandleFromObfName(clazz, methodObfName, returnType, p1), methodObfName);
    }

    public static <P1, P2, R> LLMethodHandle2<P1, P2, R> findMethod(Class<?> clazz, String methodObfName, Class<R> returnType, Class<P1> p1, Class<P2> p2) {
        return new LLMethodHandle2<>(findMethodHandleFromObfName(clazz, methodObfName, returnType, p1, p2), methodObfName);
    }

    public static <P1, P2, P3, R> LLMethodHandle3<P1, P2, P3, R> findMethod(Class<?> clazz, String methodObfName, Class<R> returnType, Class<P1> p1, Class<P2> p2, Class<P3> p3) {
        return new LLMethodHandle3<>(findMethodHandleFromObfName(clazz, methodObfName, returnType, p1, p2, p3), methodObfName);
    }

    public static <P1, P2, P3, P4, R> LLMethodHandle4<P1, P2, P3, P4, R> findMethod(Class<?> clazz, String methodObfName, Class<R> returnType, Class<P1> p1, Class<P2> p2, Class<P3> p3, Class<P4> p4) {
        return new LLMethodHandle4<>(findMethodHandleFromObfName(clazz, methodObfName, returnType, p1, p2, p3, p4), methodObfName);
    }

}
