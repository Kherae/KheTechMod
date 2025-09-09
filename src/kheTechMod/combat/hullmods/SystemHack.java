package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import org.lwjgl.util.vector.Vector2f;
//import org.apache.log4j.Logger;

public class SystemHack extends BaseHullMod {
	public static final float SYSTEM_CD_BOOST=1.2f;
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getSystemCooldownBonus().modifyMult(id,1f/SYSTEM_CD_BOOST);
    }
		
	public String getDescriptionParam(int index, HullSize hullSize) {
		if(index==0){return KheUtilities.lazyKheGetMultString(SYSTEM_CD_BOOST);}
		return "Fart: "+index;
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		super.advanceInCombat(ship, amount);
		if (!ship.isAlive()) return;
        ShipSystemAPI sys = ship.getSystem();
        if(sys==null){return;}//should never happen but whatever.
		//ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
		if (!ship.getFluxTracker().isOverloadedOrVenting()) {
            if(!sys.isOn()){ship.giveCommand(ShipCommand.USE_SYSTEM,new Vector2f(),0);}
            else{ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);}
		}
	}

	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		if(ship.getSystem()==null) {
			return "Ship has no system, and thus is violating the Modding Guidelines as posted on the forums. Contact the author, get them to fix it.";
		}
		return super.getUnapplicableReason(ship);
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return (ship.getSystem()!=null);
	}

}











