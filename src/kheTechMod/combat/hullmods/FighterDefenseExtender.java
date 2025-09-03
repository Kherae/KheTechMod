package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

public class FighterDefenseExtender extends BaseHullMod {
    public final static float PHASEBONUSES =0.5f;
    public final static float SHIELDBONUSES =2f;
    public final static float JUNKERMOD=2f/3f;

    @Override
    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
        boolean isPhase=KheUtilities.isPhaseShip(fighter,true,true,false);
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
            stats.getEmpDamageTakenMult().modifyMult(id, JUNKERMOD);
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


    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec){
        Color bad = Misc.getNegativeHighlightColor();
        Color good = Misc.getHighlightColor();
        if (ship == null || ship.getMutableStats() == null) return;
        float opad = 10f;
        tooltip.addSectionHeading("Shielded Fighter Bonuses", Alignment.MID, opad);
        tooltip.addPara("Shield radius: %s\nShield turn rate: %s",opad,good,KheUtilities.lazyKheGetMultString(SHIELDBONUSES),KheUtilities.lazyKheGetMultString(SHIELDBONUSES));
        tooltip.addSectionHeading("Phase Fighter Bonuses", Alignment.MID, opad);
        tooltip.addPara("Phase activation: %s\nPhase upkeep: %s",opad,good,KheUtilities.lazyKheGetMultString(PHASEBONUSES),KheUtilities.lazyKheGetMultString(PHASEBONUSES));
        tooltip.addSectionHeading("Hull-Only Fighter Bonuses", Alignment.MID, opad);
        tooltip.addPara("Hull damage taken: %s\nArmor damage taken: %s\nEMP damage taken: %s",opad,good,
                KheUtilities.lazyKheGetMultString(JUNKERMOD),KheUtilities.lazyKheGetMultString(JUNKERMOD),KheUtilities.lazyKheGetMultString(JUNKERMOD));
    }
}