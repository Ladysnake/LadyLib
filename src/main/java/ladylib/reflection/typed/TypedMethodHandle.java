package ladylib.reflection.typed;

import java.lang.invoke.MethodHandle;

public class TypedMethodHandle {
    protected MethodHandle methodHandle;
    protected String name;

    TypedMethodHandle(MethodHandle methodHandle, String name) {
        this.methodHandle = methodHandle;
        this.name = name;
    }

}
