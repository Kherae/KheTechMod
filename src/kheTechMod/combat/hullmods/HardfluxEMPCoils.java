package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.combat.*;

public class HardfluxEMPCoils extends BaseHullMod {
    public static final String myID="khehardfluxshield";
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        return "PIGEON";
    }

    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getHardFluxDissipationFraction().modifyMult(id, 0f);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.isAlive()) return;
        float hardFlux=ship.getFluxTracker().getHardFlux();
        ship.getMutableStats().getEmpDamageTakenMult().unmodify(myID);
        ship.getMutableStats().getEmpDamageTakenMult().modifyFlat(myID,hardFlux);
        if (ship.isPhased()) {
            ship.getMutableStats().getPhaseCloakUpkeepCostBonus().modifyPercent(myID, hardFlux*100f);
        }
        else {
            ship.getMutableStats().getPhaseCloakUpkeepCostBonus().unmodify(myID);
        }
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (ship==null){return null;}
        boolean isPhase=KheUtilities.isPhaseShip(ship,false);
        boolean isShielded=KheUtilities.isShielded(ship,true,false);
        if (!(isPhase||isShielded)) {
            return "Ship has no shields and does not phase.";
        }
        return null;
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        boolean isPhase=KheUtilities.isPhaseShip(ship,false);
        boolean isShielded=KheUtilities.isShielded(ship,true,false);
        return (isPhase || isShielded);
    }
}
