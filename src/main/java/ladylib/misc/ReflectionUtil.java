package ladylib.misc;

import org.objectweb.asm.Type;

import java.lang.invoke.*;
import java.lang.reflect.Field;

public class ReflectionUtil {

    private static final MethodHandles.Lookup TRUSTED_LOOKUP;

    static {
        try {
            // Define black magic.
            // Source: https://gist.github.com/Andrei-Pozolotin/dc8b448dc590183f5459
            final MethodHandles.Lookup original = MethodHandles.lookup();
            final Field internal = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            internal.setAccessible(true);
            TRUSTED_LOOKUP = (MethodHandles.Lookup) internal.get(original);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?> getClassForType(Type type) {
        switch (type.getSort()) {
            case Type.VOID: return void.class;
            case Type.BOOLEAN: return boolean.class;
            case Type.CHAR: return char.class;
            case Type.BYTE: return byte.class;
            case Type.SHORT: return short.class;
            case Type.INT: return int.class;
            case Type.FLOAT: return float.class;
            case Type.LONG: return long.class;
            case Type.DOUBLE: return double.class;
            case Type.OBJECT:
                try {
                    return Class.forName(type.getClassName(), false, ReflectionUtil.class.getClassLoader());
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            default: throw new IllegalArgumentException();
        }
    }

    public static <T> T createFactory(Class<?> clazz, String invokedName, Class<?> lambdaType) {
        return createFactory(clazz, invokedName, lambdaType, MethodHandles.lookup());
    }

    /**
     * Creates a factory for the given class implementing the given <tt>lambdaType</tt>.
     * The constructor of the class will be looked up using the passed in <tt>lookup</tt> object.
     * @param clazz the class for which to create a factory
     * @param invokedName the name of the method to implement in the functional interface
     * @param lambdaType the class of a functional interface that the factory will implement
     * @param lookup the lookup to use to find the constructor
     * @return a factory implementing <tt>lambdaType</tt>
     */
    @SuppressWarnings("unchecked")
    public static <T> T createFactory(Class<?> clazz, String invokedName, Class<?> lambdaType, MethodHandles.Lookup lookup) {
        try {
            MethodHandle handle = lookup.findConstructor(clazz, MethodType.methodType(void.class));
            CallSite metafactory = LambdaMetafactory.metafactory(
                    lookup,
                    invokedName,
                    MethodType.methodType(lambdaType),
                    MethodType.methodType(Object.class),
                    handle,
                    MethodType.methodType(clazz)
            );
            return (T) metafactory.getTarget().invoke();
        } catch (Throwable throwable) {
            throw new UnableToGetFactoryException(throwable);
        }
    }

    /**
     * @param clazz the class that the returned lookup should report as its own
     * @return a trusted lookup that has all permissions in the given class
     */
    public static MethodHandles.Lookup getTrustedLookup(Class clazz) {
        // Invoke black magic.
        return TRUSTED_LOOKUP.in(clazz);
    }

    public static class UnableToGetFactoryException extends RuntimeException {
        public UnableToGetFactoryException(Throwable cause) {
            super(cause);
        }
    }
}
