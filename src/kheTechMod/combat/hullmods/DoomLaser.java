package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

public class DoomLaser extends BaseHullMod {
	private static final float DMG_MULT = 3f;
	private static final float COST_MULT = 3f;
	private static final float RANGE_PENALTY = 1f/3f;
	private static final float FIRE_PENALTY = 1f/3f;
	private static final float TURN_BOOST = 3f;

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getBeamWeaponDamageMult().modifyMult(id,DMG_MULT);
		stats.getBeamWeaponFluxCostMult().modifyMult(id,COST_MULT);
		stats.getBeamWeaponRangeBonus().modifyMult(id,RANGE_PENALTY);
		stats.getBeamWeaponTurnRateBonus().modifyMult(id,TURN_BOOST);
		stats.getEnergyRoFMult().modifyMult(id,FIRE_PENALTY);
		stats.getBallisticRoFMult().modifyMult(id,FIRE_PENALTY);
		stats.getMissileRoFMult().modifyMult(id,FIRE_PENALTY);
	}

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec){
        Color bad = Misc.getNegativeHighlightColor();
        Color good = Misc.getHighlightColor();

        if (ship == null || ship.getMutableStats() == null) return;
        float opad = 10f;
        tooltip.addSectionHeading("Stats", Alignment.MID, opad);
        tooltip.addPara("Beam damage: %s\nBeam turn speed: %s",opad,good,
                KheUtilities.lazyKheGetMultString(DMG_MULT,2),
                KheUtilities.lazyKheGetMultString(TURN_BOOST,2)
        );
        tooltip.addPara(
            "Beam range: %s\nBeam flux: cost %s\nALL weapon fire rate: %s",opad,bad,
            KheUtilities.lazyKheGetMultString(RANGE_PENALTY,2),
                KheUtilities.lazyKheGetMultString(COST_MULT,2),
                KheUtilities.lazyKheGetMultString(FIRE_PENALTY,2)
        );
    }

}
