package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

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
		if (index == 2){return KheUtilities.lazyKheGetMultString(SUPPLY_UPKEEP_PENALTY,2);}
		return "PIGEON";
	}

	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		if (ship==null){return null;}
		//too unbalanced otherwise...
		boolean isPhase=KheUtilities.isPhaseShip(ship,true,true,true);
		boolean isShielded=KheUtilities.isShielded(ship,true,false);
		if (isPhase) {
			return "Cannot be installed on phase ships or ships with a damper field.";
		}
		else if (isShielded) {
//			return "Cannot be installed on ships that natively have shields.";
			return "Cannot be installed on ships with shields.";
		}
		return null;
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
        boolean isPhase=KheUtilities.isPhaseShip(ship,true,true,true);
		boolean isShielded=KheUtilities.isShielded(ship,true,false);
		return (!(isPhase || isShielded));
	}

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec){
        Color bad = Misc.getNegativeHighlightColor();
        Color good = Misc.getHighlightColor();

        if (ship == null || ship.getMutableStats() == null) return;
        float opad = 10f;
        tooltip.addSectionHeading("Stats", Alignment.MID, opad);
        tooltip.addPara("Flux dissipation: %s, scales with %s",opad,good,
                KheUtilities.lazyKheGetMultString(NATIVE_VENT_RATE*BASE_VENT_MULTIPLIER,2),"non-zero vent rate modifiers");
        tooltip.addPara(
                "Flux capacity: %s\nZero-flux speed: %s\nSupply upkeep: %s",opad,bad,
                KheUtilities.lazyKheGetMultString(FLUX_CAPACITY_MULT,2),KheUtilities.lazyKheGetMultString(0),
                KheUtilities.lazyKheGetMultString(SUPPLY_UPKEEP_PENALTY,2)
        );
    }

}
