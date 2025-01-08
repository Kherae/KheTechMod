package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;

public class WarpedHull extends BaseHullMod {
    public static final String RETROFITHULLMODID="RetrofittedBridge";
    //HullMods.AUTOMATED

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ShipVariantAPI shipVar = ship.getVariant();
        KheUtilities.removeMod(shipVar,RETROFITHULLMODID);
        KheUtilities.addMod(shipVar,HullMods.AUTOMATED);
        KheUtilities.removeDMods(ship.getVariant());
    }
}
