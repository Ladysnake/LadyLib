package ladylib.misc;

import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper.UnableToFindFieldException;
import net.minecraftforge.fml.relauncher.ReflectionHelper.UnableToFindMethodException;
import org.objectweb.asm.Type;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

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
                    throw new ReflectionHelper.UnableToFindClassException(new String[]{type.getClassName()}, e);
                }
            default:
                throw new IllegalArgumentException();
        }
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
     */
    public static Method findMethodFromObfName(Class<?> clazz, String methodObfName, Class<?> returnType, Class<?>... parameterTypes) throws UnableToFindMethodException {
        String methodDesc = Type.getMethodDescriptor(Type.getType(returnType), Arrays.stream(parameterTypes).map(Type::getType).toArray(Type[]::new));
        String deobfName = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(clazz.getName().replace('.', '/'), methodObfName, methodDesc);
        return ReflectionHelper.findMethod(clazz, deobfName, methodObfName, parameterTypes);
    }

    /**
     * Finds a field with the specified SRG name and type in the given class and makes it accessible.
     * Note: for performance, store the returned value and avoid calling this repeatedly.
     * <p>
     * Throws an exception if the field is not found.
     *
     * @param clazz          The class to find the method on.
     * @param fieldObfName   The obfuscated name of the method to find (used in obfuscated environments, i.e. "getWorldTime").
     * @param type           The type of the field to find.
     * @return The field with the specified name and type in the given class.
     * @throws UnableToFindFieldException if an issue prevents the field from being reflected
     */
    public static Field findFieldFromObfName(Class<?> clazz, String fieldObfName, Class<?> type) throws UnableToFindFieldException {
        String deobfName = FMLDeobfuscatingRemapper.INSTANCE.mapFieldName(clazz.getName().replace('.', '/'), fieldObfName, Type.getType(type).getDescriptor());
        return ReflectionHelper.findField(clazz, fieldObfName, deobfName);
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
    public static MethodHandle findMethodHandleFromObfName(Class<?> clazz, String methodObfName, Class<?> returnType, Class<?>... parameterTypes) throws UnableToFindMethodException {
        try {
            return MethodHandles.lookup().unreflect(findMethodFromObfName(clazz, methodObfName, returnType, parameterTypes));
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
     * @param clazz          The class to find the method on.
     * @param fieldObfName   The obfuscated name of the method to find (used in obfuscated environments, i.e. "getWorldTime").
     * @param type           The type of the field to find.
     * @return A handle for the getter of the field with the specified name and type in the given class.
     * @throws UnableToFindFieldException if an issue prevents the field from being reflected
     */
    public static MethodHandle findGetterFromObfName(Class<?> clazz, String fieldObfName, Class<?> type) throws UnableToFindFieldException {
        try {
            return MethodHandles.lookup().unreflectGetter(findFieldFromObfName(clazz, fieldObfName, type));
        } catch (IllegalAccessException e) {
            throw new ReflectionHelper.UnableToFindFieldException(new String[]{fieldObfName}, e);
        }
    }

    /**
     * Finds a field with the specified SRG name and type in the given class and generates a {@link MethodHandle method handle} for its setter.
     * Note: for performance, store the returned value and avoid calling this repeatedly.
     * <p>
     * Throws an exception if the field is not found.
     *
     * @param clazz          The class to find the method on.
     * @param fieldObfName   The obfuscated name of the method to find (used in obfuscated environments, i.e. "getWorldTime").
     * @param type           The type of the field to find.
     * @return A handle for the setter of the field with the specified name and type in the given class.
     * @throws UnableToFindFieldException if an issue prevents the field from being reflected
     */
    public static MethodHandle findSetterFromObfName(Class<?> clazz, String fieldObfName, Class<?> type) throws UnableToFindFieldException {
        try {
            return MethodHandles.lookup().unreflectSetter(findFieldFromObfName(clazz, fieldObfName, type));
        } catch (IllegalAccessException e) {
            throw new ReflectionHelper.UnableToFindFieldException(new String[]{fieldObfName}, e);
        }
    }


    /**
     * Creates a factory for the given class implementing the given <tt>lambdaType</tt>.
     * The constructor of the class will be looked up using the default public {@link java.lang.invoke.MethodHandles.Lookup} object.
     *
     * @param clazz       the class for which to create a factory
     * @param invokedName the name of the method to implement in the functional interface
     * @param lambdaType  the class of a functional interface that the factory will implement
     * @return a factory implementing <tt>lambdaType</tt>
     * @see #createFactory(Class, String, Class, MethodHandles.Lookup)
     */
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
