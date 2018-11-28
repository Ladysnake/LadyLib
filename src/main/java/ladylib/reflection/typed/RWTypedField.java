package ladylib.reflection.typed;

public class RWTypedField<T, R> {
    private TypedGetter<T, R> getter;
    private TypedSetter<T, R> setter;

    public RWTypedField(TypedGetter<T, R> getter, TypedSetter<T, R> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    public R get(T thisRef) {
        return this.getter.get(thisRef);
    }

    public void set(T thisRef, R value) {
        this.setter.set(thisRef, value);
    }
}
