package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import kheTechMod.combat.plugins.KheTimeCleanup;
//import kheTechMod.combat.hullmods.KheUtilities;

public class ButtercupReactor extends BaseHullMod {
	public static final String myID = "kheslowtimereactor";
	public static final float PEAK_MULT = 1f + (1f / 3f);
	//	public static final float INC_DMG_MULT_NORMAL = 0.8f;//(2f/3f);
//	public static final float INC_DMG_MULT_SMOD = 0.4f;//1f/2.5f;
	public static final float TIME_MULT_NORMAL = 2f / 3f;
	public static final float TIME_MULT_SMOD = 1f / 3f;

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getPeakCRDuration().modifyMult(id, PEAK_MULT);
	}

//    @Override
//    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
//        float dMult=isSMod(ship.getMutableStats()) ? INC_DMG_MULT_SMOD : INC_DMG_MULT_NORMAL;
//        MutableShipStatsAPI stats = fighter.getMutableStats();
//        stats.getHullDamageTakenMult().modifyMult(id, dMult);
//        stats.getArmorDamageTakenMult().modifyMult(id, dMult);
//        stats.getShieldDamageTakenMult().modifyMult(id, dMult);
//    }

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		if (Global.getCombatEngine().isPaused()) {
			return;
		}
		String localID = myID + ship.getId();
		float timeM = (isSMod(ship.getMutableStats()) ? TIME_MULT_SMOD : TIME_MULT_NORMAL);
		if (ship.isAlive() && (!ship.isPiece())) {
			MutableShipStatsAPI stats = ship.getMutableStats();
			stats.getTimeMult().modifyMult(localID, timeM);
			//honestly dont need any fancy stuff for this. if timeflow is zero we have bigger problems.
			//float currentTimeMult=KheUtilities.processStack(stats.getTimeMult(),true,null);
			float currentTimeMult = stats.getTimeMult().getModifiedValue();
			stats.getHullDamageTakenMult().modifyMult(myID, currentTimeMult);
			stats.getArmorDamageTakenMult().modifyMult(myID, currentTimeMult);
			stats.getShieldDamageTakenMult().modifyMult(myID, currentTimeMult);
			if (ship == Global.getCombatEngine().getPlayerShip()) {
				Global.getCombatEngine().getTimeMult().modifyMult(localID, 1f / timeM);
				//sadly no easy way to enforce clearing once the ship retreats. gonegone. thus, an external tracker is used.
				KheTimeCleanup.registerTimeEffect(localID);
			} else {
				Global.getCombatEngine().getTimeMult().unmodify(localID);
			}
		} else {
			MutableShipStatsAPI stats = ship.getMutableStats();
			stats.getTimeMult().unmodify(localID);
			Global.getCombatEngine().getTimeMult().unmodify(localID);
			stats.getHullDamageTakenMult().unmodify(myID);
			stats.getArmorDamageTakenMult().unmodify(myID);
			stats.getShieldDamageTakenMult().unmodify(myID);
		}
	}

	@Override
	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) {
			return KheUtilities.lazyKheGetMultString(TIME_MULT_NORMAL);
		}
		if (index == 1) {
			return KheUtilities.lazyKheGetMultString(PEAK_MULT);
		}
		if (index == 2) {
			return "the ship's total timeflow multiplier";
		}
		return "PIGEON";
	}

	@Override
	public String getSModDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return KheUtilities.lazyKheGetMultString(TIME_MULT_SMOD);
		return "PIGEON";
	}
}
