package ladylib.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoCapability {

    Class<? extends Capability.IStorage> storage() default Capability.IStorage.class;

    Class<? extends ICapabilityProvider> provider() default ICapabilityProvider.class;

}
