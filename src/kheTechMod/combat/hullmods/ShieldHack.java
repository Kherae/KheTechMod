package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class ShieldHack extends BaseHullMod {
	public static final float SHIELDUNFOLDBOOST=2f;
	public static final float SHIELDTURNBOOST =2f;

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getShieldUnfoldRateMult().modifyMult(id,SHIELDUNFOLDBOOST);
		stats.getShieldTurnRateMult().modifyMult(id, SHIELDTURNBOOST);
	}
		
	public String getDescriptionParam(int index, HullSize hullSize) {
		if(index==0){KheUtilities.lazyKheGetMultString(SHIELDUNFOLDBOOST);}
		if(index==1){KheUtilities.lazyKheGetMultString(SHIELDTURNBOOST);}
		return null;
	}
	
	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		super.advanceInCombat(ship, amount);
		if (!ship.isAlive()) return;

		ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
		//ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);

		if (!ship.getFluxTracker().isOverloadedOrVenting()) {
			if (!ship.getShield().isOn()) {
				ship.getShield().toggleOn();
			}
		}
	}

	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		if(KheUtilities.shipHasHullmod(ship,"shield_always_on")) {
			return "Ship already has a hullmod that locks the shield on.";
		}
		if(!KheUtilities.isShielded(ship,true,false)) {
			return "Ship has no shields.";
		}
		return super.getUnapplicableReason(ship);
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return (!KheUtilities.shipHasHullmod(ship,"shield_always_on"))&&KheUtilities.isShielded(ship,true,false)&&(super.isApplicableToShip(ship));
	}

}











