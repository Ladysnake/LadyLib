package ladylib.reflection.typed;

import java.lang.invoke.MethodHandle;

public class TypedGetter<T, R> extends TypedMethodHandle0<T, R> {
    TypedGetter(MethodHandle methodHandle, String name) {
        super(methodHandle, name);
    }

    public R get(T thisRef) {
        return this.invoke(thisRef);
    }
}
