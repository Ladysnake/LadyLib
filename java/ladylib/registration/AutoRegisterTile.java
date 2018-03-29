package ladylib.registration;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Equivalent to the above but specifies that this type should be registered as a tile entity
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoRegisterTile {
    String value();
    String name() default "";
    Class<? extends TileEntitySpecialRenderer> renderer();
}
