package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class InvertedOmegaFighters extends BaseHullMod {
    public final float DPPENALTYPERCENT=20;

    @Override
    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
        PersonAPI captain = ship.getCaptain();
        if (KheUtilities.personIsCore(captain)) {
            fighter.setCaptain(KheUtilities.clonePersonForFighter(captain));
        }
    }

    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyPercent(id,DPPENALTYPERCENT);
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return (DPPENALTYPERCENT) + "%";
        return "PIGEON";
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        boolean auto=!KheUtilities.isAutomated(ship);
        boolean bays=!KheUtilities.hasFighterBays(ship);
        if(auto&&bays){return "Ships is neither automated nor has fighter bays.";}
        if (auto){return "Only applicable to automated ships.";}
        if (bays){return "Ship does not have fighter bays.";}
        return null;
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return (KheUtilities.isAutomated(ship)&&KheUtilities.hasFighterBays(ship));
    }
}