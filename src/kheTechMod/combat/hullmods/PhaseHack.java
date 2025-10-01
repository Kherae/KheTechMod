package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;


public class PhaseHack extends BaseHullMod {
	public static final String myID = "khephasehack";
	public static final float FLUXPERCENTPERSECOND = 0.01f;
	final static float FLUX_THRESHOLD_INCREASE_PERCENT = 10000.0f;
	public static final float MALFUNCTIONBOOST = 100f;
	public static final float CREWLOSSMULT = 2f;
	public static final float DMODMULT = 2f;
	public static final float OVERLOADPENALTY = 2f;
	public static final float OPPENALTY = 300f;

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getPhaseCloakCooldownBonus().modifyMult(id, 0f);
		stats.getPhaseCloakUpkeepCostBonus().modifyMult(id, 0f);
		stats.getPhaseCloakActivationCostBonus().modifyMult(id, 0f);
		stats.getDynamic().getMod(Stats.PHASE_CLOAK_FLUX_LEVEL_FOR_MIN_SPEED_MOD).modifyPercent(id, FLUX_THRESHOLD_INCREASE_PERCENT);

		stats.getFluxDissipation().modifyMult(id, 0f);
		stats.getVentRateMult().modifyMult(id, 0f);
		stats.getOverloadTimeMod().modifyMult(id, OVERLOADPENALTY);
		stats.getAllowZeroFluxAtAnyLevel().modifyFlat(id, 1f);

		stats.getCrewLossMult().modifyMult(id, CREWLOSSMULT);
		stats.getDynamic().getMod(Stats.DMOD_EFFECT_MULT).modifyMult(id, DMODMULT);


		stats.getWeaponMalfunctionChance().modifyFlat(id, MALFUNCTIONBOOST);
		for (WeaponAPI.WeaponSize wS : KheUtilities.ALLSIZES) {
			for (WeaponAPI.WeaponType wT : KheUtilities.ALLCOSTMODWEAPONTYPES) {
				stats.getDynamic().getMod(KheUtilities.selectCostModString(wT, wS, false, false)).modifyFlat(id, OPPENALTY);
			}
		}
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		if (hasBadMods(ship)) {
			ship.getVariant().removeMod(myID);
		}
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		super.advanceInCombat(ship, amount);
		if (!ship.isAlive()) return;
		//be on the safe side.
		ShipSystemAPI cloak = ship.getPhaseCloak();
		if(cloak==null){return;}

		ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
		ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);

		if (!ship.getFluxTracker().isOverloadedOrVenting()) {
			ship.getFluxTracker().increaseFlux(ship.getFluxTracker().getMaxFlux() * FLUXPERCENTPERSECOND * amount, true);
			if (!cloak.isOn()) {
				cloak.forceState(ShipSystemAPI.SystemState.IN, 1f);
			}
		} else {
			if (ship.getFluxLevel() >= 0.90) {
				ship.getFluxTracker().setCurrFlux(0f);
			}
		}

		ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
		ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
	}

	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		if (hasBadMods(ship)) {
			return "Incompatible Phase Anchor and certain special phase overrides.";
		}
		if (!KheUtilities.isPhaseShip(ship, true, false, false)) {
			return "Ship cannot phase.";
		}
		return super.getUnapplicableReason(ship);
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return
				KheUtilities.isPhaseShip(ship, true, false, false) &&
						(!hasBadMods(ship)) &&
						(super.isApplicableToShip(ship))
				;
	}

	public static boolean hasBadMods(ShipAPI ship) {
		return
				(KheUtilities.shipHasHullmod(ship, "phase_anchor")) ||
						(KheUtilities.shipHasHullmod(ship, "khe_phase_anchor")) ||
						(KheUtilities.shipHasHullmod(ship, "ugh_inverteddilator")) ||
						(KheUtilities.shipHasHullmod(ship, "ugh_phasearmor")) ||
						(KheUtilities.shipHasHullmod(ship, "prv_flickercore"))
				;
	}

	@Override
	public boolean affectsOPCosts() {
		return true;
	}

	@Override
	public boolean canBeAddedOrRemovedNow(ShipAPI ship, MarketAPI marketOrNull, CampaignUIAPI.CoreUITradeMode mode) {
		if (KheUtilities.isThisRatsExoship(marketOrNull)) {
			return false;
		}
		if (KheUtilities.shipHasHullmod(ship, myID)) {
			return super.canBeAddedOrRemovedNow(ship, marketOrNull, mode);
		}
		if (KheUtilities.wouldAdditionPutOverLimit(myID, ship, KheUtilities.ALLCOSTMODWEAPONTYPES, KheUtilities.ALLSIZES, false, false, OPPENALTY) > 0f) {
			return false;
		}
		return super.canBeAddedOrRemovedNow(ship, marketOrNull, mode);
	}

	@Override
	public String getCanNotBeInstalledNowReason(ShipAPI ship, MarketAPI marketOrNull, CampaignUIAPI.CoreUITradeMode mode) {
		if (KheUtilities.isThisRatsExoship(marketOrNull)) {
			return KheUtilities.RATSEXOSHIPNOREMOVALSTRING;
		}
		float buffer = KheUtilities.wouldAdditionPutOverLimit(myID, ship, KheUtilities.ALLCOSTMODWEAPONTYPES, KheUtilities.ALLSIZES, false, false, OPPENALTY);
		if (buffer > 0f) {
			return "Empty all weapon mounts.";
		}
		return super.getCanNotBeInstalledNowReason(ship, marketOrNull, mode);
	}
}









