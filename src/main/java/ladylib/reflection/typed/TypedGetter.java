package ladylib.reflection.typed;

import java.lang.invoke.MethodHandle;

/**
 * A typed getter using a method handle to access a field
 * @param <T> type of the field's owner
 * @param <R> type of the field's value
 *
 * @since 2.6
 */
public class TypedGetter<T, R> extends TypedMethod0<T, R> {
    public TypedGetter(MethodHandle methodHandle, String name) {
        super(methodHandle, name);
    }

    public R get(T thisRef) {
        return this.invoke(thisRef);
    }
}
