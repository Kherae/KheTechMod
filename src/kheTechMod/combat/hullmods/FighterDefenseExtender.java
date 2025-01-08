package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.combat.*;

import java.awt.*;

public class FighterDefenseExtender extends BaseHullMod {
    public final static float PHASEBONUSES =0.5f;
    public final static float SHIELDBONUSES =2f;
    public final static float JUNKERMOD=2f/3f;

    @Override
    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
        boolean isPhase=KheUtilities.isPhaseShip(fighter,false);
        boolean isShielded=KheUtilities.isShielded(fighter,false,false);
        if(isPhase){
            ShipSystemAPI phase=fighter.getPhaseCloak();
            if(phase!=null){
                phase.setFluxPerSecond(phase.getFluxPerSecond()*PHASEBONUSES);
                phase.setFluxPerUse(phase.getFluxPerUse()*PHASEBONUSES);
            }
        }
        if(isShielded){
            ShieldAPI shield = fighter.getShield();
            if(shield!=null){
                shield.setArc(shield.getArc()*SHIELDBONUSES);
                shield.setRingRotationRate(shield.getRingRotationRate()*SHIELDBONUSES);
                shield.setInnerRotationRate(shield.getInnerRotationRate()*SHIELDBONUSES);
                shield.setRingColor(Color.green);
            }
        }
        if(!(isShielded||isPhase)){
            MutableShipStatsAPI stats=fighter.getMutableStats();
            stats.getArmorDamageTakenMult().modifyMult(id, JUNKERMOD);
            stats.getHullDamageTakenMult().modifyMult(id, JUNKERMOD);
        }
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return KheUtilities.lazyKheGetMultString(SHIELDBONUSES);
        if (index == 1) return KheUtilities.lazyKheGetMultString(PHASEBONUSES);
        if (index == 2) return KheUtilities.lazyKheGetMultString(JUNKERMOD);
        return "PIGEON";
    }

    public boolean isApplicableToShip(ShipAPI ship) {
        return KheUtilities.hasFighterBays(ship);
    }

    public String getUnapplicableReason(ShipAPI ship) {
        if (!KheUtilities.hasFighterBays(ship)){return "Ship does not have fighter bays";}
        return null;
    }
}