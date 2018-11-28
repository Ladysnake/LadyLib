package ladylib.reflection.typed;

import java.lang.invoke.MethodHandle;

public class TypedMethod {
    protected MethodHandle methodHandle;
    protected String name;

    TypedMethod(MethodHandle methodHandle, String name) {
        this.methodHandle = methodHandle;
        this.name = name;
    }

}
