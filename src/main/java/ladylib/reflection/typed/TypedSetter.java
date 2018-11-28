package ladylib.reflection.typed;

import java.lang.invoke.MethodHandle;

public class TypedSetter<T, P1> extends TypedMethodHandle1<T, P1, Void> {
    TypedSetter(MethodHandle methodHandle, String name) {
        super(methodHandle, name);
    }

    public void set(T thisRef, P1 value) {
        this.invoke(thisRef, value);
    }
}
