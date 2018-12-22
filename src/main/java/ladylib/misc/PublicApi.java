package ladylib.misc;

import org.apiguardian.api.API;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import static org.apiguardian.api.API.Status.MAINTAINED;

/**
 * Indicates that the annotated element is to be used by third parties rather than in the current codebase.
 *<p><em>Note:</em> Consider using the more expressive {@link API} instead
 */
@API(status = MAINTAINED, since = "2.6.2")
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface PublicApi { }
