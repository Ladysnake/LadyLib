package ladylib.misc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated element is to be used by third parties rather than in the current codebase.
 */
@PublicApi
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface PublicApi { }
