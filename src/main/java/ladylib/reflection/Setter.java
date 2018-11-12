package ladylib.reflection;

import java.lang.invoke.MethodHandle;

public class Setter<T> extends LLMethodHandle.LLMethodHandle1<T, Void> {
    public Setter(MethodHandle methodHandle, String name) {
        super(methodHandle, name);
    }

    public void set(T value) {
        this.invoke(value);
    }
}
