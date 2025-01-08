package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import kheTechMod.combat.plugins.KheTimeCleanup;

public class ButtercupReactor extends BaseHullMod {
    public static final String myID="kheslowtimereactor";
	public static final float PEAK_MULT = 1f+(1f/3f);
	public static final float INC_DMG_MULT_NORMAL = 0.8f;//(2f/3f);
	public static final float INC_DMG_MULT_SMOD = 0.4f;//1f/2.5f;
	public static final float TIME_MULT_NORMAL = 2f/3f;
	public static final float TIME_MULT_SMOD = 1f/3f;

    @Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        float dMult=isSMod(stats) ? INC_DMG_MULT_SMOD : INC_DMG_MULT_NORMAL;
        stats.getPeakCRDuration().modifyMult(id, PEAK_MULT);
        stats.getHullDamageTakenMult().modifyMult(id, dMult);
        stats.getArmorDamageTakenMult().modifyMult(id, dMult);
        stats.getShieldDamageTakenMult().modifyMult(id, dMult);
	}
	
    @Override
    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
        float dMult=isSMod(ship.getMutableStats()) ? INC_DMG_MULT_SMOD : INC_DMG_MULT_NORMAL;
        MutableShipStatsAPI stats = fighter.getMutableStats();
        stats.getHullDamageTakenMult().modifyMult(id, dMult);
        stats.getArmorDamageTakenMult().modifyMult(id, dMult);
        stats.getShieldDamageTakenMult().modifyMult(id, dMult);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (Global.getCombatEngine().isPaused()) {return;}
        String localID=myID+ship.getId();
        float timeM = (isSMod(ship.getMutableStats()) ? TIME_MULT_SMOD : TIME_MULT_NORMAL);
        if (ship.isAlive()&&(!ship.isPiece())){
            ship.getMutableStats().getTimeMult().modifyMult(localID, timeM);
            if (ship == Global.getCombatEngine().getPlayerShip()) {
                Global.getCombatEngine().getTimeMult().modifyMult(localID, 1f / timeM);
                //sadly no easy way to enforce clearing once the ship retreats. gonegone. thus, an external tracker is used.
                KheTimeCleanup.registerTimeEffect(localID);
            } else {
                Global.getCombatEngine().getTimeMult().unmodify(localID);
            }
        } else {
            ship.getMutableStats().getTimeMult().unmodify(localID);
            Global.getCombatEngine().getTimeMult().unmodify(localID);
        }
	}
    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0){return KheUtilities.lazyKheGetMultString(INC_DMG_MULT_NORMAL);}
        if (index == 1){return KheUtilities.lazyKheGetMultString(TIME_MULT_NORMAL);}
        if (index == 2){return KheUtilities.lazyKheGetMultString(PEAK_MULT);}
        return "PIGEON";
    }
    @Override
	public String getSModDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return KheUtilities.lazyKheGetMultString(TIME_MULT_SMOD);
        if (index == 1) return KheUtilities.lazyKheGetMultString(INC_DMG_MULT_SMOD);
        return "PIGEON";
	}
	
	@Override
	public boolean isSModEffectAPenalty() {
		return true;
	}
}
