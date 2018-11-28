package ladylib.reflection.typed;

import ladylib.misc.PublicApi;
import ladylib.misc.ReflectionFailedException;

import java.lang.invoke.MethodHandle;

@PublicApi
public class TypedMethod2<T, P1, P2, R> extends TypedMethod {
    public TypedMethod2(MethodHandle methodHandle, String name) {
        super(methodHandle, name);
    }

    @SuppressWarnings("unchecked")
    public R invoke(T thisRef, P1 arg1, P2 arg2) {
        try {
            return (R) methodHandle.invoke(thisRef, arg1, arg2);
        } catch (Throwable throwable) {
            throw new ReflectionFailedException(String.format("Could not invoke %s [%s] on %s with args %s, and %s", name, methodHandle, thisRef, arg1, arg2), throwable);
        }
    }
}
