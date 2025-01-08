package kheTechMod.combat.hullmods;

import kheTechMod.combat.plugins.KheTimeCleanup;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

import java.util.Objects;

public class PhaseStasisCoilsUtil extends BaseHullMod {

    public static final String[] PhaseStasisModels = {
        "khephasestasisa",
        "khephasestasisb",
        "khephasestasisc",
        "khephasestasiso"
    };

    public static boolean hasOtherPhaseStasis(String id,ShipAPI ship){
        boolean result=false;
        boolean foundSelf=false;
        for (String phaseStasisModel : PhaseStasisModels) {
            if ((!Objects.equals(phaseStasisModel, id)) && KheUtilities.shipHasHullmod(ship,phaseStasisModel)) {
                result=true;
            }
            else if(Objects.equals(phaseStasisModel,id)){foundSelf=true;}
        }
        return result&&foundSelf;
    }

    public static String reasonString(ShipAPI ship,String id){
        if (hasOtherPhaseStasis(id,ship)) {
            return "Incompatible with other Phase Stasis Coil modifications.";
        }
        if (!KheUtilities.isPhaseShip(ship,false))
            return "This modification is exclusive to Phase Ships";
        return null;
    }

    public static boolean handlePhase(ShipAPI ship,String myModel,boolean doTimeWarp,boolean wasPhased,boolean clampTime){
        String instancedModel=myModel+ship.getId();
        if (!ship.isAlive()) {
            unApplyTimeMultiplier(ship, instancedModel,doTimeWarp);
            return false;
        }

        boolean isPlayer = (ship == Global.getCombatEngine().getPlayerShip());
        boolean retPhased=wasPhased;

        if (ship.isPhased()) {
            if(isPlayer){KheTimeCleanup.registerTimeEffect(instancedModel);}
            retPhased=true;
            MutableStat time=ship.getMutableStats().getTimeMult();

            float timeMultNow = time.computeMultMod();

            if ((timeMultNow!=0.0f) && (timeMultNow!=1f)) {
                unApplyTimeMultiplier(ship, instancedModel,doTimeWarp);
                timeMultNow = time.computeMultMod();
                applyTimeMultiplier(ship,timeMultNow,isPlayer,instancedModel,doTimeWarp,clampTime);
            }
        }
        else if(wasPhased){
            retPhased=false;
            unApplyTimeMultiplier(ship, instancedModel,doTimeWarp);
        }
        return retPhased;
    }

    public static void handlePhaseBonuses(MutableShipStatsAPI stats, String id, float upkeepModifier, float FLUX_THRESHOLD_INCREASE_PERCENT) {
        stats.getPhaseCloakUpkeepCostBonus().modifyPercent(id, upkeepModifier*100f);
        stats.getDynamic().getMod(Stats.PHASE_CLOAK_FLUX_LEVEL_FOR_MIN_SPEED_MOD).modifyPercent(id, FLUX_THRESHOLD_INCREASE_PERCENT);
    }

    public static boolean applicable(ShipAPI ship,String id){
        return (ship != null) && (KheUtilities.isPhaseShip(ship,false)) && (!hasOtherPhaseStasis(id,ship));
    }

    public static void applyTimeMultiplier(ShipAPI ship, float timeMultNow,boolean isPlayer,String effectID,boolean timeWarp,boolean clampTime){
        if(isPlayer){Global.getCombatEngine().getTimeMult().modifyMult(effectID, timeMultNow);}
        /*KheTimeCleanup.registerTimeEffect(effectID);*//*this does not go here, this function is only called only when the value changes!*/
        ship.getMutableStats().getTimeMult().modifyMult(effectID,1f / timeMultNow);
        if(clampTime){timeMultNow=Math.max(1f,timeMultNow);}
        if(timeWarp) {
            ship.getMutableStats().getBallisticAmmoRegenMult().modifyMult(effectID, timeMultNow);
            ship.getMutableStats().getCombatEngineRepairTimeMult().modifyMult(effectID, timeMultNow);
            ship.getMutableStats().getCombatWeaponRepairTimeMult().modifyMult(effectID, timeMultNow);
            ship.getMutableStats().getEnergyAmmoRegenMult().modifyMult(effectID, timeMultNow);
            //this needs the 1/n treatment, otherwise it does the opposite of desired.
            ship.getMutableStats().getFighterRefitTimeMult().modifyMult(effectID, 1f/timeMultNow);
            ship.getMutableStats().getHullCombatRepairRatePercentPerSecond().modifyMult(effectID, timeMultNow);
            ship.getMutableStats().getMissileAmmoRegenMult().modifyMult(effectID, timeMultNow);
            ship.getMutableStats().getRecoilDecayMult().modifyMult(effectID, 1f / timeMultNow);
            ship.getMutableStats().getSystemCooldownBonus().modifyMult(effectID, 1f / timeMultNow);
            ship.getMutableStats().getWeaponTurnRateBonus().modifyMult(effectID, 1f / timeMultNow);
            ship.getMutableStats().getFluxDissipation().modifyMult(effectID, 1f / timeMultNow);
        }
    }

    public static void unApplyTimeMultiplier(ShipAPI ship, String effectID, boolean timeWarp){
        Global.getCombatEngine().getTimeMult().unmodify(effectID);
        ship.getMutableStats().getTimeMult().unmodify(effectID);
        if(timeWarp) {
            ship.getMutableStats().getBallisticAmmoRegenMult().unmodify(effectID);
            ship.getMutableStats().getCombatEngineRepairTimeMult().unmodify(effectID);
            ship.getMutableStats().getCombatWeaponRepairTimeMult().unmodify(effectID);
            ship.getMutableStats().getEnergyAmmoRegenMult().unmodify(effectID);
            ship.getMutableStats().getFighterRefitTimeMult().unmodify(effectID);
            ship.getMutableStats().getHullCombatRepairRatePercentPerSecond().unmodify(effectID);
            ship.getMutableStats().getMissileAmmoRegenMult().unmodify(effectID);
            ship.getMutableStats().getRecoilDecayMult().unmodify(effectID);
            ship.getMutableStats().getSystemCooldownBonus().unmodify(effectID);
            ship.getMutableStats().getWeaponTurnRateBonus().unmodify(effectID);
            ship.getMutableStats().getFluxDissipation().unmodify(effectID);
        }
    }
}
