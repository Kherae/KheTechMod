package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;
import java.util.List;
import java.util.StringJoiner;

//import org.apache.log4j.Logger;

public class WeaponArtilleryModuleUtil extends BaseHullMod {
//	private final Logger log = Logger.getLogger(WeaponArtilleryModuleUtil.class);
	public final float COSTREDUCTION = 20f;
	public final float RANGEBONUS = 1000f;
	public final float NOOPPENALTY = 1f;
	public final float FLUXPENALTYMULT = 1f;
	public final float OVERLOADTHRESHOLD=1.1f;
	public static final String ARTILLERYADDBLOCKREASON="Addition would cause OP to exceed maximum.";

	public static void valueMath(
		String id,MutableShipStatsAPI stats,ShipAPI ship,
		List<WeaponAPI.WeaponType> validTypes,List<WeaponAPI.WeaponSize> validSizes,boolean isBeam,
		float nooppenalty, float fluxlesspenalty
	) {
		float noopWeaponModifer=0f;
		float invalidWeaponModifier=1f;
		for (WeaponAPI weaponEntry : ship.getAllWeapons()) {
			if(!KheUtilities.weaponMatches(weaponEntry,false,isBeam,false,validTypes,validSizes)){continue;}
			if (weaponEntry.isDecorative()) {continue;}
			if (weaponEntry.getSlot().isBuiltIn()) {continue;}
			if (!validSizes.contains(weaponEntry.getSize())) {continue;}
			if (!validTypes.contains(weaponEntry.getType())) {continue;}

			PersonAPI captain=ship.getCaptain();
			if (captain==null){captain=Global.getFactory().createPerson();}
			float currentOP=Math.max(0f,weaponEntry.getSpec().getOrdnancePointCost(captain.getStats(),ship.getMutableStats()));//CLAMP IT.
            if (currentOP > 0f) {continue;}

			float originalDP=Math.abs(weaponEntry.getOriginalSpec().getOrdnancePointCost(null));//because fuck you.
            if(weaponEntry.getFluxCostToFire()<=0f) {
                invalidWeaponModifier*=(1f-(fluxlesspenalty*originalDP/100f));
            }
            else{
                noopWeaponModifer+=originalDP*nooppenalty;
            }

        }
		if(invalidWeaponModifier<1f) {
			stats.getFluxDissipation().modifyMult(id,invalidWeaponModifier);
		}
		if(isBeam){
			KheUtilities.getFluxCostStatBonus(stats,true).modifyPercent(id, noopWeaponModifer);
		}
		else {
			StatBonus buffer=null;
			WeaponAPI.WeaponType wType=null;
			if((validTypes!=null)&&(!validTypes.isEmpty())){for(WeaponAPI.WeaponType wT:validTypes){wType=wT;break;}}
			if(wType!=null) {
                buffer = KheUtilities.getFluxCostStatBonus(stats, wType);
			}
			if(buffer!=null){
				buffer.modifyPercent(id, noopWeaponModifer);
			}
			else{
				stats.getFluxDissipation().modifyMult(id,Math.max(1f-(noopWeaponModifer/100f),0f));
			}
		}
	}

	public static String unapplicableResolve(ShipAPI ship, List<WeaponAPI.WeaponType> WEAPONTYPES, List<WeaponAPI.WeaponSize> VALIDSIZES,boolean isBeam){
		if(!applicableResolve(ship, WEAPONTYPES,VALIDSIZES,isBeam)) {
			return "Ship has no matching slots.";
		}
		return null;
	}

	public static boolean applicableResolve(ShipAPI ship, List<WeaponAPI.WeaponType> WEAPONTYPES, List<WeaponAPI.WeaponSize> VALIDSIZES,boolean isBeam){
		for (WeaponAPI.WeaponType w:WEAPONTYPES){
			if(KheUtilities.shipHasMatchingSlot(ship,KheUtilities.selectMountList(w,isBeam),VALIDSIZES)){return true;}
		}
		return false;
	}

	public static String descParamResolve(
		int index,
		List<WeaponAPI.WeaponType> WEAPONTYPES, List<WeaponAPI.WeaponSize> VALIDSIZES,
		float nooppenalty, float fluxpenaltymult, float rangebonus, float costreduction,float overloadthreshold,boolean isBeam
	) {
		if (index==0){return slotSizeListString(VALIDSIZES);}
		if (index==1){
			String buffer="";
			if(isBeam){buffer+="Beam";}else{buffer+=slotTypeListString(WEAPONTYPES);}
			return buffer;
		}
		if (index==2){return Math.floor(rangebonus)+"";}
		if (index==3){return Math.floor(costreduction)+"";}
		if(((!(overloadthreshold<=1f))&&index==4)||(index==5)){
			return "Flux Cost: "+nooppenalty+"% per Base OP. Fluxless: (1-((BaseOP*"+fluxpenaltymult+")/100))x, per weapon";
		}
		if (index==4){return Math.round(overloadthreshold*100f)+"%";}
		return "PIGEON";
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		super.advanceInCombat(ship,amount);
		if (getOverloadThreshold()<1f){
			if (!ship.isAlive()||ship.isPiece()||ship.getFluxTracker().isOverloaded()) return;

			if (ship.getFluxTracker().getFluxLevel()>=getOverloadThreshold()) {
				if (ship.isPhased()) {ship.setPhased(false);}
				ship.getFluxTracker().forceOverload(0f);
			}
		}
	}

	public static class ArtilleryModuleRangeModifier implements WeaponRangeModifier {
		public final float rangebonus;
		public final List<WeaponAPI.WeaponType> validtypes;
		public final List<WeaponAPI.WeaponSize> validsizes;
		public final boolean isBeam;

		public ArtilleryModuleRangeModifier(float rangebonus, List<WeaponAPI.WeaponType> validtypes, List<WeaponAPI.WeaponSize> validsizes,boolean isBeam) {
			this.rangebonus = rangebonus;
			this.validtypes = validtypes;
			this.validsizes = validsizes;
			this.isBeam = isBeam;
		}

		public float getWeaponRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
			return 0;
		}
		public float getWeaponRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
			return 1f;
		}
		public float getWeaponRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
			if (!KheUtilities.weaponMatches(weapon,false,isBeam,false,validtypes,validsizes)){return 0f;}
			return rangebonus;
		}
	}

	public static String slotSizeListString(List<WeaponAPI.WeaponSize> sizes) {
		StringJoiner sj = new StringJoiner(", ");
		for (WeaponAPI.WeaponSize s : sizes){sj.add(s.getDisplayName());}
		return sj.toString();
	}

	public static String slotTypeListString(List<WeaponAPI.WeaponType> types) {
		StringJoiner sj = new StringJoiner(", ");
		for (WeaponAPI.WeaponType s : types){sj.add(s.getDisplayName());}
		return sj.toString();
	}

	public float getOverloadThreshold() {
		return OVERLOADTHRESHOLD;
	}

	@Override
	public boolean affectsOPCosts() {
		return true;
	}

	public String addRemoveReasonResolve(
		ShipAPI ship, String myID,float costModifier,
		List<WeaponAPI.WeaponType> hullmodWeaponTypes, List<WeaponAPI.WeaponSize> weaponSizes,boolean beamMode,
		MarketAPI marketOrNull
	){
		if(KheUtilities.isThisRatsExoship(marketOrNull)){return KheUtilities.RATSEXOSHIPNOREMOVALSTRING;}
		if (preventRemoveOrAdd(ship,myID,costModifier,hullmodWeaponTypes,weaponSizes,beamMode,marketOrNull)){return ARTILLERYADDBLOCKREASON;}
		return null;
	}

	public boolean preventRemoveOrAdd(
		ShipAPI ship, String myID, float costModifier,
		List<WeaponAPI.WeaponType> hullmodWeaponTypes, List<WeaponAPI.WeaponSize> weaponSizes, boolean beamMode,
		MarketAPI marketOrNull
	) {
		if(ship==null){return false;}
		if(KheUtilities.isThisRatsExoship(marketOrNull)){return true;}
		if (KheUtilities.shipHasHullmod(ship,myID)){return false;}
		return KheUtilities.wouldAdditionPutOverLimit(myID,ship,hullmodWeaponTypes,weaponSizes,beamMode,false,costModifier);
	}

}