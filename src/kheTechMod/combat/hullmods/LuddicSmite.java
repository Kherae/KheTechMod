package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.combat.*;

public class LuddicSmite extends LuddicTorch {
    final static String myID="kheluddicsmite";
    final static float MANEUVERABILITYBOOST = 2f;
    final static float SPEEDBOOST = 1.5f;

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if(index==5){return KheUtilities.lazyKheGetMultString(MANEUVERABILITYBOOST,2);}
        if(index==6){return KheUtilities.lazyKheGetMultString(SPEEDBOOST,2);}
        return super.getDescriptionParam(index,hullSize);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getHullCombatRepairRatePercentPerSecond().modifyMult(id,0f);
        stats.getMaxCombatHullRepairFraction().modifyMult(id,0f);
        super.applyEffectsBeforeShipCreation(hullSize,stats,id);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        super.advanceInCombat(ship, amount);
        MutableShipStatsAPI stats=ship.getMutableStats();
        stats.getMaxTurnRate().modifyMult(myID,KheUtilities.lerp(1f,MANEUVERABILITYBOOST,1f-ship.getHullLevel()));
        stats.getTurnAcceleration().modifyMult(myID,KheUtilities.lerp(1f,MANEUVERABILITYBOOST,1f-ship.getHullLevel()));
        stats.getMaxSpeed().modifyMult(myID,KheUtilities.lerp(1f,SPEEDBOOST,1f-ship.getHullLevel()));
        stats.getAcceleration().modifyMult(myID,KheUtilities.lerp(1f,SPEEDBOOST,1f-ship.getHullLevel()));
    }
}
