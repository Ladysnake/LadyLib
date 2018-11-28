package ladylib.reflection.typed;

import java.lang.invoke.MethodHandle;

/**
 * A typed method handle, typically with an <code>invoke</code> method
 * that has a variable number of typed parameters
 *
 * @since 2.6
 */
public abstract class TypedMethod {
    protected MethodHandle methodHandle;
    protected String name;

    protected TypedMethod(MethodHandle methodHandle, String name) {
        this.methodHandle = methodHandle;
        this.name = name;
    }

}
