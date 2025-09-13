package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import kheTechMod.combat.plugins.KheTimeCleanup;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

import java.awt.*;
import java.util.Objects;
import org.apache.log4j.Logger;

public class PhaseStasisCoilsUtil extends BaseHullMod {
    private static final Logger log = Logger.getLogger(PhaseStasisCoilsUtil.class);
//    final static float CLOAK_UPKEEP_MODIFIER =1f;
//    final static boolean SIMULATE_TIMEFLOW =false;
//    final static boolean clampTime=false;
//    final static float FLUX_THRESHOLD_INCREASE_PERCENT=0.0f;
//    public static float TIMEWARP_EFFECTIVENESS_MULT=0.5f;

    public static final String[] PhaseStasisModels = {
        "khephasestasisa",
        "khephasestasisb",
        "khephasestasisc",
        "khephasestasiso"
    };
    protected static Object STATUSKEYFLUXTIMEWARPA = new Object();


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
        if (!KheUtilities.isPhaseShip(ship,true,true,false))
            return "This modification is exclusive to Phase Ships";
        return null;
    }

    public void handlePhase(ShipAPI ship,String myModel,boolean doTimeWarp,boolean clampTime,float advanceAmount,float timewarpEffectiveness){
        String instancedModel=myModel+ship.getId();//use this, not 'myModel'. do a 'delete' test on mymodel after changes.
        if (!ship.isAlive()){
            unApplyTimeMultiplier(ship, instancedModel,doTimeWarp);
            return;
        }
        boolean isPlayer = (ship == Global.getCombatEngine().getPlayerShip());
        if (ship.isPhased()) {
            MutableStat time = ship.getMutableStats().getTimeMult().createCopy();
            time.unmodify(instancedModel);
            float timeElsewhere=time.computeMultMod();
            //float timeElsewhereb=KheUtilities.processStack(ship.getMutableStats().getTimeMult(),true, Collections.singletonList(instancedModel));
            if(isPlayer){KheTimeCleanup.registerTimeEffect(instancedModel);
                if (doTimeWarp) {
                    ShipSystemAPI cloak = ship.getPhaseCloak();
                    if (cloak == null) {
                        cloak = ship.getSystem();
                    }
                    if (cloak != null) {
                        float buffer = timeElsewhere;
                        if(clampTime){buffer=Math.max(1f,buffer);}
                        if(timewarpEffectiveness!=1f){buffer=(buffer-1f)*timewarpEffectiveness+1f;}
                        buffer = Math.round(buffer * 100f) / 100f;
                        if (clampTime) {
                            buffer = Math.max(0, buffer);
                        }


                        Global.getCombatEngine().maintainStatusForPlayerShip(
                            STATUSKEYFLUXTIMEWARPA, cloak.getSpecAPI().getIconSpriteName(),
                            "phase-stasis",
                            buffer + "x diverted timeflow.", false
                        );
                    }
                }
            }
            applyTimeMultiplier(ship,timeElsewhere,isPlayer,instancedModel,doTimeWarp,clampTime,timewarpEffectiveness,advanceAmount);
        }
        else{
            unApplyTimeMultiplier(ship, instancedModel,doTimeWarp);
        }
    }

    public static void handlePhaseBonuses(MutableShipStatsAPI stats, String id, float upkeepModifier, float FLUX_THRESHOLD_INCREASE_PERCENT) {
        stats.getPhaseCloakUpkeepCostBonus().modifyMult(id, upkeepModifier);
        stats.getDynamic().getMod(Stats.PHASE_CLOAK_FLUX_LEVEL_FOR_MIN_SPEED_MOD).modifyPercent(id, FLUX_THRESHOLD_INCREASE_PERCENT);
    }

    public static boolean applicable(ShipAPI ship,String id){
        return (ship != null) && (KheUtilities.isPhaseShip(ship,true,false,false)) && (!hasOtherPhaseStasis(id,ship));
    }

    public static void applyTimeMultiplier(ShipAPI ship, float timeMultNow,boolean isPlayer,String effectID,boolean timeWarp,boolean clampTime,float timewarpEffectiveness,float advanceAmount){
        if(isPlayer){
            Global.getCombatEngine().getTimeMult().modifyMult(effectID, timeMultNow);
        }
        ship.getMutableStats().getTimeMult().modifyMult(effectID,1f / timeMultNow);

        if(timeWarp) {
            if(clampTime){timeMultNow=Math.max(1f,timeMultNow);}
            if(timewarpEffectiveness!=1f){timeMultNow=(timeMultNow-1f)*timewarpEffectiveness+1f;}
            ship.getMutableStats().getBallisticAmmoRegenMult().modifyMult(effectID, timeMultNow);
            ship.getMutableStats().getCombatEngineRepairTimeMult().modifyMult(effectID, timeMultNow);
            ship.getMutableStats().getCombatWeaponRepairTimeMult().modifyMult(effectID, timeMultNow);
            ship.getMutableStats().getEnergyAmmoRegenMult().modifyMult(effectID, timeMultNow);
            //this needs the 1/n treatment, otherwise it does the opposite of desired.
            ship.getMutableStats().getFighterRefitTimeMult().modifyMult(effectID, 1f/timeMultNow);
            ship.getMutableStats().getHullCombatRepairRatePercentPerSecond().modifyMult(effectID, timeMultNow);
            ship.getMutableStats().getMissileAmmoRegenMult().modifyMult(effectID, timeMultNow);
            ship.getMutableStats().getRecoilDecayMult().modifyMult(effectID, 1f / timeMultNow);
            //ship.getMutableStats().getSystemCooldownBonus().modifyMult(effectID, 1f / timeMultNow);//disabled as it is too much of a migraine for balance. phase cloaks are systems.
            ship.getMutableStats().getWeaponTurnRateBonus().modifyMult(effectID, 1f / timeMultNow);
            ship.getMutableStats().getFluxDissipation().modifyMult(effectID, timeMultNow);//why was this 1/n? that...makes flux dissipation WORSE
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
            //ship.getMutableStats().getSystemCooldownBonus().unmodify(effectID);
            ship.getMutableStats().getWeaponTurnRateBonus().unmodify(effectID);
            ship.getMutableStats().getFluxDissipation().unmodify(effectID);
        }
    }

    public static void tooltipHandler(
            TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec,
            float upkeepModifier,float fluxThresholdModifier,boolean timeWarp,boolean clampTime,float timewarpEffectiveness
    ){
        Color bad = Misc.getNegativeHighlightColor();
        Color good = Misc.getHighlightColor();
        //Color darkBad = Misc.setAlpha(Misc.scaleColorOnly(bad, 0.4f), 175);

        if (ship == null || ship.getMutableStats() == null) return;
        float opad = 10f;

        boolean upkeep=false;boolean threshold=false;

        if(upkeepModifier!=1f){upkeep=true;}
        if(fluxThresholdModifier>0f){threshold=true;}

        tooltip.addSectionHeading("Stats", Alignment.MID, opad);
        tooltip.addPara("Ship is normalized to 1.0x timeflow.",opad);
        if(upkeep){
            Color upkeepHighlight;
            if(upkeepModifier>1.0f){upkeepHighlight=bad;}else{upkeepHighlight=good;}
            tooltip.addPara("Phase Cloak Upkeep %s",
                    opad,upkeepHighlight,
                    "x"+Math.round(upkeepModifier * 100f)/100f +"%"
            );
        }
        if(threshold){
            tooltip.addPara("Flux-induced speed penalty almost entirely neutralized.",opad);
        }
        if(timeWarp){
            tooltip.addSectionHeading("Diverted Timeflow", Alignment.MID, opad);
            String buffer="Increases and decreases to timeflow are applied to weapons, fighter bays (but not fighters), flux dissipation, and engine and hull repairs at %s of their value";
            if(clampTime){buffer+=", but may not bring the ship below 1.0x timeflow.";}else{buffer+=".";}
            tooltip.addPara(buffer,opad,good,
                (int)(Math.round(timewarpEffectiveness*1000f)/10f)+"%"
            );
        }

    }

}
