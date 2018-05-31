package ladylib.registration;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Automatically registers a tile entity class to the game registry.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoRegisterTile {

    /**
     * The mod id to which the annotated class belongs
     */
    String value();

    /**
     * Sets the name that will be used by the game registry
     */
    String name() default "";

    /**
     * Sets a render class that will be bound to this tile entity class. <br/>
     * If this value is not manually defined or its value is the TileEntitySpecialRenderer base class,
     * no TESR will be bound.
     */
    Class<? extends TileEntitySpecialRenderer> renderer() default TileEntitySpecialRenderer.class;
}
