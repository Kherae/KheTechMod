package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
//import org.apache.log4j.Logger;

public class WeaponReplexerUtil extends BaseHullMod {
//	private final Logger log = Logger.getLogger(WeaponReplexerUtil.class);
	public static final float COSTREDUCTION = 100f;
	//public static final float OPMULT = 1f;
	public static final float OPMULT_SMALL = 1.5f;
	public static final float OPMULT_MEDIUM = 1.25f;
	public static final float OPMULT_LARGE = 1f;
	//public static final float FLUXPENALTYMULT = 1f;
    public static final float FLUXPENALTYMULT_LARGE = 1f;
	public static final float FLUXPENALTYMULT_MEDIUM = 2f;
	public static final float FLUXPENALTYMULT_SMALL = 3f;
	public static final float OVERLOADPENALTYMULT = 2f;
	public static final String REPLEXERNOREMOVALSTRING="Removal would cause OP to exceed maximum by ";
	public final static List <WeaponAPI.WeaponSize> VALIDSIZES = Arrays.asList(WeaponAPI.WeaponSize.LARGE, WeaponAPI.WeaponSize.MEDIUM, WeaponAPI.WeaponSize.SMALL);

	@Override
	public boolean affectsOPCosts() {return true;}

	@SuppressWarnings("SameReturnValue")
    public static boolean isBeamMode(){return false;}
	public WeaponAPI.WeaponType getWeaponType(){return null;}

	public static List <WeaponAPI.WeaponType> selectMountList(WeaponAPI.WeaponType weapType) {
		return KheUtilities.selectMountList(weapType,isBeamMode());
	}

	public void applyBeforeCreationModifiers(MutableShipStatsAPI stats, String id, float reduction, float overloadpenaltymult) {
		for (WeaponAPI.WeaponSize size : VALIDSIZES) {
			KheUtilities.getOPCostStatBonus(stats,size,getWeaponType(),isBeamMode(),false).modifyFlat(id,-reduction);
		}
		stats.getOverloadTimeMod().modifyMult(id,overloadpenaltymult);
	}


    public void valueMath(String id,ShipAPI ship, float opMult, float penaltyMult) {valueMath(id,ship,opMult,opMult,opMult,penaltyMult,penaltyMult,penaltyMult);}
    public void valueMath(
            String id,ShipAPI ship,
            float opMult_small,float opMult_med,float opMult_large,
            float penaltyMult_small,float penaltyMult_med,float penaltyMult_large
    ) {
		MutableShipStatsAPI stats = ship.getMutableStats();
		List<WeaponAPI> weaponList=ship.getAllWeapons();
		float validWeaponModifier=0f;
		float invalidWeaponModifier=1f;
		for (WeaponAPI weaponEntry : weaponList) {
			if(weaponMatches(weaponEntry,true)){
				float baseOP=Math.max(0f,weaponEntry.getOriginalSpec().getOrdnancePointCost(null));
                WeaponAPI.WeaponSize wS = weaponEntry.getSize();
                float penaltyMult=(wS==WeaponAPI.WeaponSize.LARGE)?penaltyMult_large:((wS==WeaponAPI.WeaponSize.MEDIUM)?penaltyMult_med:penaltyMult_small);
                float opMult=(wS==WeaponAPI.WeaponSize.LARGE)?opMult_large:((wS==WeaponAPI.WeaponSize.MEDIUM)?opMult_med:opMult_small);

				if(weaponEntry.getFluxCostToFire()<=0f){
					invalidWeaponModifier*=(1f-(penaltyMult *baseOP*opMult)/100f);
				}
				else{
					validWeaponModifier += baseOP*opMult;
				}
			}
		}
		if(invalidWeaponModifier<1f) {
			stats.getFluxDissipation().modifyMult(id,invalidWeaponModifier);
		}
		if(isBeamMode()){
			KheUtilities.getFluxCostStatBonus(stats,true).modifyPercent(id, validWeaponModifier);
		}
		else {
			StatBonus buffer=KheUtilities.getFluxCostStatBonus(stats,getWeaponType());
			if(buffer!=null){
				buffer.modifyPercent(id, validWeaponModifier);
			}
			else{
				stats.getFluxDissipation().modifyMult(id,Math.max(1f-(validWeaponModifier/100f),0f));
			}
		}
	}

    //this isnt really used anymore, more of a legacy thing.
//	public String descParamResolve(int index, float costreduction, float opmult, float fluxpenaltymult, float overloadpenaltymult){
//		if (index==0){return overloadpenaltymult +"x";}
//		if (index==1){
//			String buffer="";
//			if(isBeamMode()) {
//				buffer+="Beam";
//			}else if(getWeaponType()!=null){
//				buffer+=getWeaponType().getDisplayName();
//			}
//			return buffer;
//		}
//		if (index==2){return Math.floor(costreduction)+"";}
//		if (index==3){return "Flux Cost: "+opmult+"% per Base OP. Fluxless: (1-((BaseOP*"+fluxpenaltymult+")/100))x, per weapon.";}
//		return "PIGEON";
//	}

	public String unapplicableResolve(ShipAPI ship){
		if(!applicableResolve(ship)) {
			return "Ship has no matching slots.";
		}
		return null;
	}

	public boolean applicableResolve(ShipAPI ship){
		return KheUtilities.shipHasMatchingSlot(ship,selectMountList(getWeaponType()),null);
	}

	public String addRemoveReasonResolve(ShipAPI ship, String myID, MarketAPI marketOrNull){
		if(KheUtilities.isThisRatsExoship(marketOrNull)){return KheUtilities.RATSEXOSHIPNOREMOVALSTRING;}
		if (preventRemoveOrAdd(ship,myID,marketOrNull)){return REPLEXERNOREMOVALSTRING+wouldRemovalPutOverLimit(myID, ship)+".";}
		return null;
	}

	public boolean preventRemoveOrAdd(ShipAPI ship, String myID, MarketAPI marketOrNull) {
		if(ship==null){return false;}
		if(KheUtilities.isThisRatsExoship(marketOrNull)){return true;}
		if (!KheUtilities.shipHasHullmod(ship,myID)){return false;}
        return wouldRemovalPutOverLimit(myID, ship)>0;
    }

	public boolean weaponMatches(WeaponAPI weaponEntry,boolean noBuiltIns){
		return KheUtilities.weaponMatches(weaponEntry,noBuiltIns,isBeamMode(),false,getWeaponType(),KheUtilities.ALLSIZES);
    }

	public int wouldRemovalPutOverLimit(String myID,ShipAPI ship){
		List<String>ignoreIDs=Collections.singletonList(myID);

		PersonAPI captain=ship.getCaptain();
		if (captain==null){captain=Global.getFactory().createPerson();}
		MutableCharacterStatsAPI captainStats = captain.getStats();

		float currentOPBuffer=
			ship.getVariant().getNumFluxVents()+
			ship.getVariant().getNumFluxCapacitors()+
			KheUtilities.getNonBuiltInFighterOPCost(ship)+
			KheUtilities.getHullModOPCost(ship)
		;
		float limit=ship.getHullSpec().getOrdnancePoints(captainStats);
		MutableShipStatsAPI shipStats = ship.getMutableStats();

		for (WeaponAPI weaponEntry : ship.getAllWeapons()) {
			WeaponSpecAPI wOSpec = weaponEntry.getOriginalSpec();
            if(!KheUtilities.weaponIsBuiltIn(weaponEntry)){
                if (weaponMatches(weaponEntry,false)) {
                    currentOPBuffer+=KheUtilities.specialOPCostCalc(wOSpec, captainStats, shipStats, ignoreIDs,null,false);
                }
                else{
                    currentOPBuffer+=wOSpec.getOrdnancePointCost(captainStats, shipStats);
                }
            }
		}
		return (int) (currentOPBuffer - limit);
    }

    public static void tooltipHandler(
            TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec,
            List<WeaponAPI.WeaponType> validTypes, List<WeaponAPI.WeaponSize> validSizes,
            float costreduction, float opmult_small,float opmult_med,float opmult_large,
            float overloadpenalty,float fluxpenaltymult_small,float fluxpenaltymult_med,float fluxpenaltymult_lrg
    ){
        Color bad = Misc.getNegativeHighlightColor();
        Color good = Misc.getHighlightColor();
        Color mid = Misc.getTextColor();
        Color darkBad = Misc.setAlpha(Misc.scaleColorOnly(bad, 0.4f), 175);
//        Color verygood=Misc.getStoryOptionColor();
//        Color verygood2=Misc.getStoryDarkColor();

        if (ship == null || ship.getMutableStats() == null) return;
        float opad = 10f;

        tooltip.addSectionHeading("Applicable Weapons", Alignment.MID, opad);
        tooltip.addPara("Size: %s\nType: %s",opad,good,KheUtilities.slotSizeListString(validSizes),KheUtilities.slotTypeListString(validTypes));

        tooltip.addSectionHeading("Stats", Alignment.MID, opad);
        tooltip.addPara("OP cost decrease: %s",opad,good,Math.round(costreduction)+"");
        if(overloadpenalty!=1f){
            Color overloadColor;if(overloadpenalty<1f){overloadColor=good;}else{overloadColor=bad;}
            tooltip.addPara("Overload duration: %s",opad,overloadColor,KheUtilities.lazyKheGetMultString(overloadpenalty,1));
        }

        tooltip.addSectionHeading("Tech Notes: Ordnance Overload", mid, darkBad, Alignment.MID, opad);
        tooltip.addPara("Weapon flux cost penalty per Base OP, additive multiplier for all affected weapons:\nSmall: %s\nMedium: %s\nLarge: %s",
                opad,bad,KheUtilities.lazyKheGetPercentString(opmult_small,2),
                KheUtilities.lazyKheGetPercentString(opmult_med,2),
                KheUtilities.lazyKheGetPercentString(opmult_large,2)
        );
        tooltip.addPara("Fluxless weapon dissipation penalty per Base OP, multiplicative per weapon:\nSmall: %s\nMedium: %s\nLarge: %s",
                opad,bad,KheUtilities.lazyKheGetPercentString(fluxpenaltymult_small,2),
                KheUtilities.lazyKheGetPercentString(fluxpenaltymult_med,2),
                KheUtilities.lazyKheGetPercentString(fluxpenaltymult_lrg,2)
        );
        tooltip.addPara("%s",
                opad,bad,"Stat UIs may not properly reflect cost increases!"
        );

    }
}