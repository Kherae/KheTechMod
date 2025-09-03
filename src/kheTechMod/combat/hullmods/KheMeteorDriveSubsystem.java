package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.combat.*;

import java.awt.*;
import java.util.Objects;

import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
//import com.fs.starfarer.api.impl.campaign.ids.Stats;
import org.lwjgl.util.vector.Vector2f;

public class KheMeteorDriveSubsystem extends BaseHullMod {
//    public final float DPPENALTYPERCENT=100;
//    private final Logger log = Logger.getLogger(KheMeteorDriveSubsystem.class);

    public final static float MASS_MULT=33f;
    //100 was way, way too strong. oneshot a radiant, 90% of an onslaught. 10 is too weak, doesnt even touch armor. 33 is a nice good number.
    public final static String myID = "KheMeteorDriveSubsystem";

    public static class KheMeteorDriveDeepSystem implements AdvanceableListener, HullDamageAboutToBeTakenListener {
        private float massStart;
        private boolean wasOn = false;
        private boolean overloading = false;
        private boolean invuln=false;

        public final ShipAPI ship;
        public KheMeteorDriveDeepSystem(ShipAPI ship) {
            this.ship = ship;
        }

        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
               return false;
        }

        @Override
        public void advance(float amount) {
            MutableShipStatsAPI stats = ship.getMutableStats();
            if ((ship.getSystem() != null) && (Objects.equals(ship.getSystem().getId(), "kheburndrive"))) {
                if (ship.getSystem().isOn()) {
                    ShipSystemAPI.SystemState systemState = ship.getSystem().getState();
                    if(systemState.equals(ShipSystemAPI.SystemState.IN)){
                        if(!invuln) {
                            invuln=KheUtilities.handleInvuln(myID,true,stats);
                        }
                    }else if(systemState.equals(ShipSystemAPI.SystemState.ACTIVE)){
                        if (!wasOn) {
                            wasOn = true;
                            massStart = ship.getMass();
                            ship.setMass(massStart * MASS_MULT);
                        }
                        for(WeaponAPI weapon:ship.getAllWeapons()){
                            weapon.disable();
                        }
                    }
                } else if (wasOn) {
                    wasOn = false;
                    overloading = true;
                    ship.getFluxTracker().forceOverload(5f);
                    ship.getFluxTracker().setCurrFlux(ship.getMaxFlux());
                    ship.getFluxTracker().setHardFlux(ship.getMaxFlux());
                    ship.getEngineController().forceFlameout();
                    if(invuln){
                        invuln=KheUtilities.handleInvuln(myID,false,stats);
                    }
                } else if (overloading) {
                    if (!ship.getFluxTracker().isOverloadedOrVenting()) {
                        overloading=false;
                        ship.setMass(massStart);
                    }
                } else
                if(invuln){
                    invuln=KheUtilities.handleInvuln(myID,false,stats);
                }
            }
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new KheMeteorDriveSubsystem.KheMeteorDriveDeepSystem(ship));
    }
//
//    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
//        if (index == 0) return (DPPENALTYPERCENT) + "%";
//        return "PIGEON";
//    }
}
