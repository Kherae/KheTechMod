package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public class PhaseStasisCoilsC extends PhaseStasisCoilsUtil {
	final static float CLOAK_UPKEEP_MODIFIER = 1.0f;
	final static boolean SIMULATE_TIMEFLOW = true;
	public static float TIMEWARP_EFFECTIVENESS_MULT = 0.5f;
	final static boolean TIMEFLOW_SIM_CLAMP = false;
	final static float FLUX_THRESHOLD_INCREASE_PERCENT = 0.0f;
	final static String myModel = "khephasestasisc";

	//old and probably shouldnt remain
	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return (CLOAK_UPKEEP_MODIFIER * 100f) + "%";
		return "PIGEON";
	}

	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		handlePhaseBonuses(stats, id, CLOAK_UPKEEP_MODIFIER, FLUX_THRESHOLD_INCREASE_PERCENT);
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		handlePhase(ship, myModel, SIMULATE_TIMEFLOW, TIMEFLOW_SIM_CLAMP, TIMEWARP_EFFECTIVENESS_MULT, amount);
	}

	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		return reasonString(ship, myModel);
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return applicable(ship, myModel);
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		tooltipHandler(tooltip, hullSize, ship, width, isForModSpec, CLOAK_UPKEEP_MODIFIER, FLUX_THRESHOLD_INCREASE_PERCENT, SIMULATE_TIMEFLOW, TIMEFLOW_SIM_CLAMP, TIMEWARP_EFFECTIVENESS_MULT);
	}
}
