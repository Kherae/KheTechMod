package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

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

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0){return KheUtilities.lazyKheGetMultString(DMG_MULT);}
		if (index == 1){return KheUtilities.lazyKheGetMultString(COST_MULT);}
		if (index == 2){return KheUtilities.lazyKheGetMultString(RANGE_PENALTY);}
		if (index == 3){return KheUtilities.lazyKheGetMultString(TURN_BOOST);}
		if (index == 4){return KheUtilities.lazyKheGetMultString(FIRE_PENALTY);}
		return "PIGEON";
	}

}
