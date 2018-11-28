package ladylib.misc;

import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper.UnableToFindClassException;
import net.minecraftforge.fml.relauncher.ReflectionHelper.UnableToFindFieldException;
import net.minecraftforge.fml.relauncher.ReflectionHelper.UnableToFindMethodException;
import org.objectweb.asm.Type;

import javax.annotation.Nullable;
import java.lang.invoke.*;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ReflectionUtil {
    private ReflectionUtil() { }

    private static final Lookup TRUSTED_LOOKUP;

    static {
        try {
            // Define black magic.
            // Source: https://gist.github.com/Andrei-Pozolotin/dc8b448dc590183f5459
            final Lookup original = MethodHandles.lookup();
            final Field internal = Lookup.class.getDeclaredField("IMPL_LOOKUP");
            internal.setAccessible(true);
            TRUSTED_LOOKUP = (Lookup) internal.get(original);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ReflectionFailedException("Could not access trusted lookup", e);
        }
    }

    public static Class<?> getClassForType(Type type) {
        switch (type.getSort()) {
            case Type.VOID:
                return void.class;
            case Type.BOOLEAN:
                return boolean.class;
            case Type.CHAR:
                return char.class;
            case Type.BYTE:
                return byte.class;
            case Type.SHORT:
                return short.class;
            case Type.INT:
                return int.class;
            case Type.FLOAT:
                return float.class;
            case Type.LONG:
                return long.class;
            case Type.DOUBLE:
                return double.class;
            case Type.OBJECT:
                try {
                    return Class.forName(type.getClassName(), false, ReflectionUtil.class.getClassLoader());
                } catch (ClassNotFoundException e) {
                    throw new UnableToFindClassException(new String[]{type.getClassName()}, e);
                }
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Finds a method with the specified SRG name and parameters in the given class and generates a {@link MethodHandle method handle} for it. <br>
     * Note: for performance, store the returned value and avoid calling this repeatedly.
     * <p>
     * Throws an exception if the method is not found.
     *
     * @param clazz          The class to find the method on.
     * @param methodObfName  The obfuscated name of the method to find (used in obfuscated environments, i.e. "getWorldTime").
     *                       If the name you are looking for is on a class that is never obfuscated, this should be null.
     * @param returnType     The return type of the method to find.
     * @param parameterTypes The parameter types of the method to find.
     * @return A handle for the method with the specified name and parameters in the given class.
     * @throws UnableToFindMethodException if an issue prevents the method from being reflected
     */
    @PublicApi
    public static MethodHandle findMethodHandleFromObfName(Class<?> clazz, String methodObfName, Class<?> returnType, Class<?>... parameterTypes) {
        try {
            return MethodHandles.lookup().unreflect(ObfuscationReflectionHelper.findMethod(clazz, methodObfName, returnType, parameterTypes));
        } catch (IllegalAccessException e) {
            throw new UnableToFindMethodException(e);
        }
    }

    /**
     * Finds a field with the specified SRG name and type in the given class and generates a {@link MethodHandle method handle} for its getter.
     * Note: for performance, store the returned value and avoid calling this repeatedly.
     * <p>
     * Throws an exception if the field is not found.
     *
     * @param clazz        The class to find the method on.
     * @param fieldObfName The obfuscated name of the method to find (used in obfuscated environments, i.e. "getWorldTime").
     * @param type         The type of the field to find.
     * @return A handle for the getter of the field with the specified name and type in the given class.
     * @throws UnableToFindFieldException if an issue prevents the field from being reflected
     */
    @PublicApi
    public static MethodHandle findGetterFromObfName(Class<?> clazz, String fieldObfName, Class<?> type) {
        try {
            return MethodHandles.lookup().unreflectGetter(ObfuscationReflectionHelper.findField(clazz, fieldObfName));
        } catch (IllegalAccessException e) {
            throw new UnableToFindFieldException(e);
        }
    }

    /**
     * Finds a field with the specified SRG name and type in the given class and generates a {@link MethodHandle method handle} for its setter.
     * Note: for performance, store the returned value and avoid calling this repeatedly.
     * <p>
     * Throws an exception if the field is not found.
     *
     * @param clazz        The class to find the method on.
     * @param fieldObfName The obfuscated name of the method to find (used in obfuscated environments, i.e. "getWorldTime").
     * @param type         The type of the field to find.
     * @return A handle for the setter of the field with the specified name and type in the given class.
     * @throws UnableToFindFieldException if an issue prevents the field from being reflected
     */
    @PublicApi
    public static MethodHandle findSetterFromObfName(Class<?> clazz, String fieldObfName, Class<?> type) {
        try {
            return MethodHandles.lookup().unreflectSetter(ObfuscationReflectionHelper.findField(clazz, fieldObfName));
        } catch (IllegalAccessException e) {
            throw new UnableToFindFieldException(e);
        }
    }


    /**
     * Creates a factory for the given class implementing the given <tt>lambdaType</tt>.
     * The constructor of the class will be looked up using the default public {@link Lookup} object.
     *
     * @param clazz       the class for which to create a factory
     * @param invokedName the name of the method to implement in the functional interface
     * @param lambdaType  the class of a functional interface that the factory will implement
     * @return a factory implementing <tt>lambdaType</tt>
     * @see #createFactory(Class, String, Class, Lookup)
     */
    @PublicApi
    public static <T> T createFactory(Class<?> clazz, String invokedName, Class<?> lambdaType) {
        return createFactory(clazz, invokedName, lambdaType, MethodHandles.lookup());
    }

    /**
     * Creates a factory for the given class implementing the given <tt>lambdaType</tt>.
     * The constructor of the class will be looked up using the passed in <tt>lookup</tt> object.
     *
     * @param clazz       the class for which to create a factory
     * @param invokedName the name of the method to implement in the functional interface
     * @param lambdaType  the class of a functional interface that the factory will implement
     * @param lookup      the lookup to use to find the constructor
     * @return a factory implementing <tt>lambdaType</tt>
     */
    @PublicApi
    @SuppressWarnings("unchecked")
    public static <T> T createFactory(Class<?> clazz, String invokedName, Class<?> lambdaType, Lookup lookup) {
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
    public static Lookup getTrustedLookup(Class clazz) {
        // Invoke black magic.
        return TRUSTED_LOOKUP.in(clazz);
    }

    /**
     * Finds a method with the specified SRG name and parameters in the given class and makes it accessible.
     * Note: for performance, store the returned value and avoid calling this repeatedly.
     * <p>
     * Throws an exception if the method is not found.
     *
     * @param clazz          The class to find the method on.
     * @param methodObfName  The obfuscated name of the method to find (used in obfuscated environments, i.e. "getWorldTime").
     *                       If the name you are looking for is on a class that is never obfuscated, this should be null.
     * @param returnType     The return type of the method to find.
     * @param parameterTypes The parameter types of the method to find.
     * @return The method with the specified name and parameters in the given class.
     * @throws UnableToFindMethodException if an issue prevents the method from being reflected
     * @deprecated use {@link ObfuscationReflectionHelper#findMethod(Class, String, Class, Class[])}
     */
    @Deprecated
    public static Method findMethodFromObfName(Class<?> clazz, String methodObfName, Class<?> returnType, Class<?>... parameterTypes) {
        return ObfuscationReflectionHelper.findMethod(clazz, methodObfName, returnType, parameterTypes);
    }

    /**
     * Finds a field with the specified SRG name and type in the given class and makes it accessible. <br>
     * Note: for performance, store the returned value and avoid calling this repeatedly.
     * <p>
     * Throws an exception if the field is not found.
     *
     * @param clazz        The class to find the method on.
     * @param fieldObfName The obfuscated name of the method to find (used in obfuscated environments, i.e. "getWorldTime").
     * @param type         The type of the field to find.
     * @return The field with the specified name and type in the given class.
     * @throws UnableToFindFieldException if an issue prevents the field from being reflected
     * @deprecated use {@link ObfuscationReflectionHelper#findField(Class, String)}
     */
    @Deprecated
    public static Field findFieldFromObfName(Class<?> clazz, String fieldObfName, Class<?> type) {
        return ObfuscationReflectionHelper.findField(clazz, fieldObfName);
    }

    /**
     * Finds a field with the specified SRG name and type in the given class and returns its value for the given <code>instance</code>.<br>
     * Note: for performance, avoid using this method when you need to obtain the value more than once.
     * <p>
     * Throws an exception if the field is not found.
     *
     * @param clazz        The class to find the method on.
     * @param instance     An instance of <code>clazz</code>. Use <code>null</code> if the field is static.
     * @param fieldObfName The obfuscated name of the method to find (used in obfuscated environments, i.e. "getWorldTime").
     * @param type         The type of the field to find.
     * @return The value of the field for the given instance.
     * @throws UnableToFindFieldException if an issue prevents the field from being reflected
     * @deprecated use {@link ObfuscationReflectionHelper#getPrivateValue(Class, Object, String)}
     */
    @Deprecated
    public static <C, T> T getPrivateValue(Class<C> clazz, @Nullable C instance, String fieldObfName, @SuppressWarnings("unused") Class<? super T> type) {
        return ObfuscationReflectionHelper.getPrivateValue(clazz, instance, fieldObfName);
    }

    /**
     * Finds a field with the specified SRG name and type in the given class and returns its value for the given <code>instance</code>.<br>
     * Note: for performance, avoid using this method when you need to obtain the value more than once.
     * <p>
     * Throws an exception if the field is not found.
     *
     * @param clazz        The class to find the method on.
     * @param instance     An instance of <code>clazz</code>. Use <code>null</code> if the field is static.
     * @param fieldObfName The obfuscated name of the method to find (used in obfuscated environments, i.e. "getWorldTime").
     * @param type         The type of the field to find.
     *
     * @throws UnableToFindFieldException if an issue prevents the field from being reflected
     * @deprecated use {@link ObfuscationReflectionHelper#setPrivateValue(Class, Object, Object, String)}
     */
    @Deprecated
    public static <C, T> void setPrivateValue(Class<C> clazz, @Nullable C instance, String fieldObfName, @SuppressWarnings("unused") Class<? super T> type, T value) {
        ObfuscationReflectionHelper.setPrivateValue(clazz, instance, value, fieldObfName);
    }

    public static class UnableToGetFactoryException extends RuntimeException {
        public UnableToGetFactoryException(Throwable cause) {
            super(cause);
        }
    }
}
