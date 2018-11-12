package ladylib.reflection;

import java.lang.invoke.MethodHandle;

public class Getter<T, R> extends LLMethodHandle.LLMethodHandle0<T, R> {
    Getter(MethodHandle methodHandle, String name) {
        super(methodHandle, name);
    }
}
