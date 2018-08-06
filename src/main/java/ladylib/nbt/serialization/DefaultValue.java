package ladylib.nbt.serialization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Sets the default value used when an object fails to be deserialized from NBT.
 * <p>
 * If set on a method, that method should have no parameters and return a default instance.
 * Exemple: <pre>
 * &#64DefaultValue(EnumAction.class)
 * public static EnumAction getDefaultAction() {
 *     return EnumAction.SUCCESS;
 * }</pre>
 * If set on a field, that field's value will be directly used as default.
 * Exemple: <pre>
 * &#64DefaultValue(EnumAction.class)
 * public static EnumAction defaultAction = EnumAction.SUCCESS;
 * </pre>
 * Default fields should only be used with immutable values, as the same object will be returned
 * each time a default is queried.
 * </p>
 */
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface DefaultValue {
    /**
     * Defines the classes for which the default should apply.
     * If left empty, it will apply to the surrounding class.
     */
    Class<?>[] value() default {};
}
