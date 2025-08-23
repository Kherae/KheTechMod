package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.combat.*;

public class WarpedHull extends BaseHullMod {
    public static final float captaincyMalus=2f;
    public static final float crewMalus=0f;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize,MutableShipStatsAPI stats, String id){
        stats.getMaxCrewMod().modifyMult(id,crewMalus);
        stats.getMinCrewMod().modifyMult(id,crewMalus);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        KheUtilities.removeDMods(ship.getVariant());
        if((ship.getCaptain()!=null)&&(!ship.getCaptain().isDefault())&&(!ship.getCaptain().isAICore())){
            ship.getMutableStats().getSensorProfile().modifyMult(id,captaincyMalus);
            ship.getMutableStats().getFuelUseMod().modifyMult(id,captaincyMalus);
            ship.getMutableStats().getSuppliesPerMonth().modifyMult(id,captaincyMalus);
        }
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if(index==0){return crewMalus+"x";}
        else if(index==1){return captaincyMalus+"x";}
        return null;
    }

}
