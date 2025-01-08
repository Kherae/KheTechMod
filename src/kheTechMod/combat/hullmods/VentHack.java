package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class VentHack extends BaseHullMod {
	//these statics are shared by all users of the class. they should not be modified. ever.
	private static final float FLUX_CAPACITY_MULT = 0.5f;
//	private static final float BASE_VENT_MULTIPLIER = 1.5f/2f;
	private static final float BASE_VENT_MULTIPLIER = 1f;
	private static final float NATIVE_VENT_RATE=2.0f;
	private static final float SUPPLY_UPKEEP_PENALTY=2.0f;
	//make sure this matches ID in hull_mods.csv
	private final static String THIS_HULLMOD_ID ="kheventhack";

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getFluxCapacity().modifyMult(id, FLUX_CAPACITY_MULT);
		stats.getVentRateMult().modifyMult(id, 0f);
		stats.getZeroFluxSpeedBoost().modifyMult(id,0f);
		stats.getSuppliesPerMonth().modifyPercent(id,SUPPLY_UPKEEP_PENALTY);
	}

	private void handleFluxDissipation(MutableShipStatsAPI stats){
		float val=KheUtilities.processStack(stats.getVentRateMult(), true,null);
		val*=(NATIVE_VENT_RATE * BASE_VENT_MULTIPLIER);
		stats.getFluxDissipation().modifyMult(THIS_HULLMOD_ID,val);
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		//if (!ship.isAlive()) return;//interferes with refit screen stat display.
		handleFluxDissipation(ship.getMutableStats());
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0){return (NATIVE_VENT_RATE*BASE_VENT_MULTIPLIER)+"x";}
		if (index == 1){return FLUX_CAPACITY_MULT+"x";}
		if (index == 2){return KheUtilities.lazyKheGetMultString(SUPPLY_UPKEEP_PENALTY);}
		return "PIGEON";
	}

	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		if (ship==null){return null;}
		//too unbalanced otherwise...
		boolean isPhase=KheUtilities.isPhaseShip(ship,false);
		boolean isShielded=KheUtilities.isShielded(ship,true,false);
		if (isPhase) {
			return "Cannot be installed on phase ships.";
		}
		else if (isShielded) {
//			return "Cannot be installed on ships that natively have shields.";
			return "Cannot be installed on ships with shields.";
		}
		return null;
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		boolean isPhase=KheUtilities.isPhaseShip(ship,false);
		boolean isShielded=KheUtilities.isShielded(ship,true,false);
		return (!(isPhase || isShielded));
	}

}
