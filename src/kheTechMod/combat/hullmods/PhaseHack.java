package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;


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

    //basically not used.
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) {
            return Math.round(FLUXPERCENTPERSECOND * 100f) + "%";
        }
        if (index == 1) {
            return KheUtilities.lazyKheGetMultString(OVERLOADPENALTY);
        }
        if (index == 2) {
            return Math.round(MALFUNCTIONBOOST) + "%";
        }
        if (index == 3) {
            return OPPENALTY + "";
        }
        if (index == 4) {
            return KheUtilities.lazyKheGetMultString(CREWLOSSMULT);
        }
        if (index == 5) {
            return KheUtilities.lazyKheGetMultString(DMODMULT);
        }
        return null;
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        super.advanceInCombat(ship, amount);
        if (!ship.isAlive()) return;

        ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
        ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);

        if (!ship.getFluxTracker().isOverloadedOrVenting()) {
            ship.getFluxTracker().increaseFlux(ship.getFluxTracker().getMaxFlux() * FLUXPERCENTPERSECOND * amount, true);
            if (ship.getPhaseCloak() != null) {
                if (!ship.getPhaseCloak().isOn()) {
                    ship.getPhaseCloak().forceState(ShipSystemAPI.SystemState.IN, 1f);
                }
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

//    public static final float FLUXPERCENTPERSECOND=0.01f;
//    final static float FLUX_THRESHOLD_INCREASE_PERCENT=10000.0f;
//    public static final float MALFUNCTIONBOOST=100f;
//    public static final float CREWLOSSMULT=2f;
//    public static final float DMODMULT=2f;
//    public static final float OVERLOADPENALTY=2f;
//    public static final float OPPENALTY=300f;
// too much effort for a gimmick mod
//    @Override
//    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec){
//        Color bad = Misc.getNegativeHighlightColor();
//        Color good = Misc.getHighlightColor();
//
//        if (ship == null || ship.getMutableStats() == null) return;
//        float opad = 10f;
//        tooltip.addSectionHeading("Stats", Alignment.MID, opad);
//        tooltip.addPara("Flux dissipation: %s, scales with %s",opad,good,
//                KheUtilities.lazyKheGetMultString(NATIVE_VENT_RATE*BASE_VENT_MULTIPLIER),"Non-zero vent rate modifiers");
//        tooltip.addPara(
//                "Flux capacity: %s\nZero-flux speed: %s\nSupply upkeep: ",opad,bad,
//                KheUtilities.lazyKheGetMultString(FLUX_CAPACITY_MULT),KheUtilities.lazyKheGetMultString(0),KheUtilities.lazyKheGetMultString(SUPPLY_UPKEEP_PENALTY)
//        );
//    }
//









