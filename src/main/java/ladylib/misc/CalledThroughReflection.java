package ladylib.misc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A simple annotation to tell static analysis tools (and other people) to shut up about "unused" code
 */
@PublicApi
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface CalledThroughReflection { }
