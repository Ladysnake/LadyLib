package ladylib.reflection.typed;

import ladylib.misc.PublicApi;
import ladylib.misc.ReflectionFailedException;

import java.lang.invoke.MethodHandle;

@PublicApi
public class TypedMethodHandle0<T, R> extends TypedMethodHandle {
    TypedMethodHandle0(MethodHandle methodHandle, String name) {
        super(methodHandle, name);
    }

    @SuppressWarnings("unchecked")
    public R invoke(T thisRef) {
        try {
            return (R) methodHandle.invoke(thisRef);
        } catch (Throwable throwable) {
            throw new ReflectionFailedException(String.format("Could not invoke %s [%s] on %s", name, methodHandle, thisRef), throwable);
        }
    }
}
