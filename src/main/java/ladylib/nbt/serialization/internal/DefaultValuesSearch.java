package ladylib.nbt.serialization.internal;

import com.google.gson.reflect.TypeToken;
import ladylib.LadyLib;
import ladylib.misc.ReflectionFailedException;
import ladylib.misc.ReflectionUtil;
import ladylib.nbt.serialization.DefaultValue;
import ladylib.nbt.serialization.TagAdapters;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import org.apache.logging.log4j.message.FormattedMessage;
import org.objectweb.asm.Type;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class DefaultValuesSearch {
    private DefaultValuesSearch() { }

    @SuppressWarnings("unchecked")
    public static void searchDefaultValues(ASMDataTable asmData) {
        // find all classes that will be handled by this registrar
        Set<ASMDataTable.ASMData> allDefaultValues = asmData.getAll(DefaultValue.class.getName());
        for (ASMDataTable.ASMData entry : allDefaultValues) {
            final String targetClass = entry.getClassName();
            final String targetName = entry.getObjectName();
            List<Type> types = (List<Type>)entry.getAnnotationInfo().get("value");
            if (types == null) {
                LadyLib.LOGGER.warn("Unable to inject capability at {}.{} (Invalid Annotation)", targetClass, targetName);
                continue;
            }
            boolean isMethod = entry.getObjectName().indexOf('(') >= 0;
            for (Type type : types) {
                try {
                    Class clazz = Class.forName(targetClass, false, DefaultValuesSearch.class.getClassLoader());
                    Class typeClass = ReflectionUtil.getClassForType(type);
                    if (isMethod) {
                        Method method = clazz.getDeclaredMethod(targetName);
                        if (!Modifier.isStatic(method.getModifiers()) || Modifier.isAbstract(method.getModifiers())) {
                            throw new ReflectionFailedException("Default value supplying methods must be static and non abstract");
                        }
                        MethodHandles.Lookup lookup = MethodHandles.lookup();
                        MethodHandle mh = lookup.unreflect(method);
                        CallSite metaFactory = LambdaMetafactory.metafactory(
                                lookup,
                                "get",
                                MethodType.methodType(Supplier.class),
                                MethodType.methodType(Object.class),
                                mh,
                                MethodType.methodType(typeClass)
                        );
                        TagAdapters.setDefaultValue(TypeToken.get(typeClass), (Supplier) metaFactory.getTarget().invoke());
                    } else {
                        Field f = clazz.getDeclaredField(targetName);
                        if (!Modifier.isStatic(f.getModifiers()) || !Modifier.isFinal(f.getModifiers())) {
                            throw new ReflectionFailedException("Default value fields must be static and final");
                        }
                        Object o = f.get(null);
                        TagAdapters.setDefaultValue(TypeToken.get(typeClass), () -> o);
                    }
                } catch (Throwable t) {
                    LadyLib.LOGGER.error(new FormattedMessage("Could not register default value {} for type {} in class {}", targetName, type, targetClass), t);
                }
            }
        }
    }
}
