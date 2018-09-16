package ladylib.client.lighting;

import net.minecraft.util.math.Vec3d;

import java.awt.*;

public class SimpleCheapLight implements MutableCheapLight {
    protected double posX, posY, posZ;
    protected double lastPosX, lastPosY, lastPosZ;
    protected float radius;
    protected Color color;
    private boolean expired;

    public SimpleCheapLight(Vec3d pos, float radius, Color color) {
        this.posX = pos.x;
        this.posY = pos.y;
        this.posZ = pos.z;
        this.radius = radius;
        this.color = color;
    }

    @Override
    public double getPosX() {
        return posX;
    }

    @Override
    public double getPosY() {
        return posY;
    }

    @Override
    public double getPosZ() {
        return posZ;
    }

    @Override
    public double getLastPosX() {
        return lastPosX;
    }

    @Override
    public double getLastPosY() {
        return lastPosY;
    }

    @Override
    public double getLastPosZ() {
        return lastPosZ;
    }

    @Override
    public void tick() {
        this.lastPosX = this.posX;
        this.lastPosY = this.posY;
        this.lastPosZ = this.posZ;
    }

    @Override
    public float getRadius() {
        return radius;
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    @Override
    public void setPosition(double x, double y, double z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    @Override
    public void setRadius(float radius) {
        this.radius = radius;
    }

    @Override
    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public void setExpired(boolean expired) {
        this.expired = expired;
    }
}
