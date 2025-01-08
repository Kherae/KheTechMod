package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class PeanutShell extends BaseHullMod {
	private static final float BONUS_MULT = 2f;
	private static final float PENALTY_MULT = 1f/2f;
	private static final float LOGISTICS_MULT = 2f;

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getSuppliesToRecover().modifyMult(id,LOGISTICS_MULT);
		stats.getFuelUseMod().modifyMult(id,LOGISTICS_MULT);
		stats.getSuppliesPerMonth().modifyMult(id,LOGISTICS_MULT);
		stats.getArmorBonus().modifyMult(id,BONUS_MULT);
		stats.getHullBonus().modifyMult(id,PENALTY_MULT);
		stats.getMaxSpeed().modifyMult(id,PENALTY_MULT);
		stats.getMaxTurnRate().modifyMult(id,PENALTY_MULT);
		stats.getZeroFluxSpeedBoost().modifyMult(id,0f);
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		float fMassStart = ship.getMass()*BONUS_MULT;
		ship.setMass(fMassStart);
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0){return KheUtilities.lazyKheGetMultString(BONUS_MULT);}
		if (index == 1){return KheUtilities.lazyKheGetMultString(PENALTY_MULT);}
		if (index == 2){return KheUtilities.lazyKheGetMultString(LOGISTICS_MULT);}
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
