package ladylib.reflection;

import java.lang.invoke.MethodHandle;

public class Setter<T, P1> extends LLMethodHandle.LLMethodHandle1<T, P1, Void> {
    Setter(MethodHandle methodHandle, String name) {
        super(methodHandle, name);
    }

    public void set(T thisRef, P1 value) {
        this.invoke(thisRef, value);
    }
}
