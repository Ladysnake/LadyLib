package ladylib;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LLInject {
    /**
     * Optional owner modid, required if this annotation is on something that is not inside the main class of a mod container.
     * This is required to prevent mods from classloading other, potentially disabled mods.
     */
    String owner() default "";
}
