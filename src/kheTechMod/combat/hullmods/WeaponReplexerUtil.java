package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
//import org.apache.log4j.Logger;

public class WeaponReplexerUtil extends BaseHullMod {
//	private final Logger log = Logger.getLogger(WeaponReplexerUtil.class);
	public static final float COSTREDUCTION = 100f;
	public static final float OPMULT = 1f;
	public static final float FLUXPENALTYMULT = 1f;
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

	public void valueMath(String id,ShipAPI ship, float opMult, float penaltyMult) {
		MutableShipStatsAPI stats = ship.getMutableStats();
		List<WeaponAPI> weaponList=ship.getAllWeapons();
		float validWeaponModifier=0f;
		float invalidWeaponModifier=1f;
		for (WeaponAPI weaponEntry : weaponList) {
			if(weaponMatches(weaponEntry,true)){
				float baseOP=Math.max(0f,weaponEntry.getOriginalSpec().getOrdnancePointCost(null));
				if(weaponEntry.getFluxCostToFire()<=0f){
					invalidWeaponModifier*=(1f-(penaltyMult*baseOP*opMult)/100f);
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

	public String descParamResolve(int index, float costreduction, float opmult, float fluxpenaltymult, float overloadpenaltymult){
		if (index==0){return overloadpenaltymult +"x";}
		if (index==1){
			String buffer="";
			if(isBeamMode()) {
				buffer+="Beam";
			}else if(getWeaponType()!=null){
				buffer+=getWeaponType().getDisplayName();
			}
			return buffer;
		}
		if (index==2){return Math.floor(costreduction)+"";}
		if (index==3){return "Flux Cost: "+opmult+"% per Base OP. Fluxless: (1-((BaseOP*"+fluxpenaltymult+")/100))x, per weapon.";}
		return "PIGEON";
	}

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
			KheUtilities.getAllFighterOPCost(ship)+
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
}