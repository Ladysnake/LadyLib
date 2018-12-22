package ladylib.reflection.typed;

import ladylib.misc.ReflectionFailedException;
import org.apiguardian.api.API;

import java.lang.invoke.MethodHandle;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL, since = "2.6.2")
public class TypedMethod3<T, P1, P2, P3, R> extends TypedMethod {
    public TypedMethod3(MethodHandle methodHandle, String name) {
        super(methodHandle, name);
    }

    @SuppressWarnings("unchecked")
    public R invoke(T thisRef, P1 arg1, P2 arg2, P3 arg3) {
        try {
            return (R) methodHandle.invoke(thisRef, arg1, arg2, arg3);
        } catch (Throwable throwable) {
            throw new ReflectionFailedException(String.format("Could not invoke %s [%s] on %s with args %s, %s and %s", name, methodHandle, thisRef, arg1, arg2, arg3), throwable);
        }
    }
}
