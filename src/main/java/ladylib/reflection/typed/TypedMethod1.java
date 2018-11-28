package ladylib.reflection.typed;

import ladylib.misc.PublicApi;
import ladylib.misc.ReflectionFailedException;

import java.lang.invoke.MethodHandle;

@PublicApi
public class TypedMethod1<T, P1, R> extends TypedMethod {
    TypedMethod1(MethodHandle methodHandle, String name) {
        super(methodHandle, name);
    }

    @SuppressWarnings("unchecked")
    public R invoke(T thisRef, P1 arg) {
        try {
            return (R) methodHandle.invoke(thisRef, arg);
        } catch (Throwable throwable) {
            throw new ReflectionFailedException(String.format("Could not invoke %s [%s] on %s with arg %s", name, methodHandle, thisRef, arg), throwable);
        }
    }
}
