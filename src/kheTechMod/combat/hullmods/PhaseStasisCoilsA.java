package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.combat.*;

public class PhaseStasisCoilsA extends PhaseStasisCoilsUtil {
    final static float upkeepModifier =1.0f;
    final static boolean doTimeWarp=false;
    final static boolean clampTime=false;
    final static float FLUX_THRESHOLD_INCREASE_PERCENT=10000.0f;
    final static String myModel="khephasestasisa";
    boolean wasPhased=false;

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return (upkeepModifier *100f) + "%";
        return "PIGEON";
    }

    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        handlePhaseBonuses(stats,id,upkeepModifier,FLUX_THRESHOLD_INCREASE_PERCENT);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        wasPhased=handlePhase(ship,myModel,doTimeWarp,wasPhased,clampTime);
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        return reasonString(ship,myModel);
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return PhaseStasisCoilsUtil.applicable(ship,myModel);
    }
}
