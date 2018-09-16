package ladylib.client.lighting;

import net.minecraft.entity.Entity;

import java.awt.*;
import java.lang.ref.WeakReference;

public class AttachedCheapLight implements CheapLight {
    private MutableCheapLight delegate;
    private WeakReference<Entity> hooked;

    public AttachedCheapLight(MutableCheapLight delegate, Entity hooked) {
        this.delegate = delegate;
        this.hooked = new WeakReference<>(hooked);
    }

    @Override
    public double getPosX() {
        return delegate.getPosX();
    }

    @Override
    public double getPosY() {
        return delegate.getPosY();
    }

    @Override
    public double getPosZ() {
        return delegate.getPosZ();
    }

    @Override
    public double getLastPosX() {
        return delegate.getLastPosX();
    }

    @Override
    public double getLastPosY() {
        return delegate.getLastPosY();
    }

    @Override
    public double getLastPosZ() {
        return delegate.getLastPosZ();
    }

    @Override
    public void tick() {
        delegate.tick();
        Entity hooked = this.hooked.get();
        if (hooked == null || hooked.isDead) {
            delegate.setExpired(true);
        } else {
            delegate.setPosition(hooked.posX, hooked.posY, hooked.posZ);
        }
    }

    @Override
    public float getRadius() {
        return delegate.getRadius();
    }

    @Override
    public Color getColor() {
        return delegate.getColor();
    }

    @Override
    public boolean isExpired() {
        return delegate.isExpired();
    }
}
