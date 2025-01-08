package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.combat.*;

public class Damn90s extends BaseHullMod {

    final static float maxOverload = 9f;
    final static float overloadPenalty = 0.9f;
    final static float autoVentThreshold = 0.9f;
    final static float ventBonus = 9f;

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return (ventBonus) + "%";
        if (index == 1) return (autoVentThreshold*100f) + "%";
        if (index == 2) return (1+overloadPenalty) + "x";
        if (index == 3) return (maxOverload) + "s";
        return "PIGEON";
    }

    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getVentRateMult().modifyPercent(id, ventBonus);
        stats.getOverloadTimeMod().modifyMult(id,1f+overloadPenalty);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.isAlive()||ship.isPiece()||ship.getFluxTracker().isVenting()||ship.isPhased()) return;

        if (ship.getFluxTracker().getOverloadTimeRemaining() > maxOverload) {
            ship.getFluxTracker().setOverloadDuration(maxOverload);
            return;
        }

        if (ship.getFluxTracker().isOverloaded()||(ship.getMutableStats().getVentRateMult().computeMultMod()<=0)||(ship.getMutableStats().getFluxDissipation().computeMultMod()<=0)) {
            return;
        }

        if (ship.getFluxTracker().getFluxLevel()>=autoVentThreshold) {
            //ship.giveCommand(ShipCommand.VENT_FLUX, true, 1);//do not use this.
            ship.getFluxTracker().ventFlux();
        }

    }
}
