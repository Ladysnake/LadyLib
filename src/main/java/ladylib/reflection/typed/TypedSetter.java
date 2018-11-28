package ladylib.reflection.typed;

import java.lang.invoke.MethodHandle;

/**
 * A typed setter using a method handle to access a field
 * @param <T> type of the field's owner
 * @param <P1> type of the field's value
 *
 * @since 2.6
 */
public class TypedSetter<T, P1> extends TypedMethod1<T, P1, Void> {
    public TypedSetter(MethodHandle methodHandle, String name) {
        super(methodHandle, name);
    }

    public void set(T thisRef, P1 value) {
        this.invoke(thisRef, value);
    }
}
