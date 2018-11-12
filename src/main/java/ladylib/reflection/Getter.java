package ladylib.reflection;

import java.lang.invoke.MethodHandle;
import java.util.function.Supplier;

public class Getter<T> extends LLMethodHandle.LLMethodHandle0<T> implements Supplier<T> {
    public Getter(MethodHandle methodHandle, String name) {
        super(methodHandle, name);
    }

    @Override
    public T get() {
        return this.invoke();
    }
}
