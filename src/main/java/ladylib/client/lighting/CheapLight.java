package ladylib.client.lighting;

import java.awt.*;

public interface CheapLight {

    double getPosX();

    double getPosY();

    double getPosZ();

    double getLastPosX();

    double getLastPosY();

    double getLastPosZ();

    float getRadius();

    Color getColor();

    boolean isExpired();

    void tick();

}
