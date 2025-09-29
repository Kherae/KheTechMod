package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class LuddicTorch extends BaseHullMod {

	final static float EXPLODEDAMAGEMULT = 1.2f;
	final static float DMODEXPLODEDAMAGEMULT = 0.2f;
	final static float EXPLODERADIUSMULT = 2.5f;
	final static float DMODEXPLODERADIUSMULT = 0.2f;
	final static float DAMAGEPERSECOND = 1f / 120f;
	final static float RECOVERMODIFIER = 1000f;
	final static float DMODCHANCEMODIFIER = 100f;

	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) {
			return KheUtilities.lazyKheGetMultString(EXPLODEDAMAGEMULT);
		}
		if (index == 1) {
			return KheUtilities.lazyKheGetMultString(DMODEXPLODEDAMAGEMULT);
		}
		if (index == 2) {
			return KheUtilities.lazyKheGetMultString(EXPLODERADIUSMULT);
		}
		if (index == 3) {
			return KheUtilities.lazyKheGetMultString(DMODEXPLODERADIUSMULT);
		}
		if (index == 4) {
			return Math.round(DAMAGEPERSECOND * 10000f) / 100f + "%";
		}
		return "PIGEON";
	}

	@Override
	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat(id, RECOVERMODIFIER);
		stats.getDynamic().getMod("dmod_acquire_prob_mod").modifyMult(id, DMODCHANCEMODIFIER);
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		MutableShipStatsAPI stats = ship.getMutableStats();
		float dmodcount = DModManager.getNumDMods(stats.getVariant());
		stats.getDynamic().getStat(Stats.EXPLOSION_DAMAGE_MULT).modifyMult(id, EXPLODEDAMAGEMULT + (dmodcount * DMODEXPLODEDAMAGEMULT));
		stats.getDynamic().getStat(Stats.EXPLOSION_RADIUS_MULT).modifyMult(id, EXPLODERADIUSMULT + (dmodcount * DMODEXPLODERADIUSMULT));
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		float currentHP = ship.getHitpoints();
		if (currentHP > 1f) {
			ship.setHitpoints(Math.max(1f, currentHP - ((DAMAGEPERSECOND * amount) * ship.getMaxHitpoints())));
		}
		super.advanceInCombat(ship, amount);
	}
}
