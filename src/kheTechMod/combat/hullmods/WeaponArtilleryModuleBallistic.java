package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.util.Collections;
import java.util.List;

public class WeaponArtilleryModuleBallistic extends WeaponArtilleryModuleUtil {
	public final String myID="kheartillerymoduleballistic";
	public final boolean ISBEAM=false;

	public final static List<WeaponAPI.WeaponType> WEAPONTYPES = Collections.singletonList(
			WeaponAPI.WeaponType.BALLISTIC
	);
	public final static List <WeaponAPI.WeaponSize> VALIDSIZES = Collections.singletonList(
			WeaponAPI.WeaponSize.LARGE
	);

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		ship.addListener(new ArtilleryModuleRangeModifier(RANGEBONUS,WEAPONTYPES,VALIDSIZES,ISBEAM));
		valueMath(id,ship.getMutableStats(),ship,WEAPONTYPES,VALIDSIZES,ISBEAM,NOOPPENALTY,FLUXPENALTYMULT);
	}

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		KheUtilities.applyCostModifiers(id,COSTREDUCTION,stats,VALIDSIZES,WEAPONTYPES,ISBEAM);
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
		return descParamResolve(index,WEAPONTYPES,VALIDSIZES,NOOPPENALTY,FLUXPENALTYMULT,RANGEBONUS,COSTREDUCTION,OVERLOADTHRESHOLD,ISBEAM);
	}

	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		return unapplicableResolve(ship,WEAPONTYPES,VALIDSIZES,false);
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return applicableResolve(ship, WEAPONTYPES,VALIDSIZES,false);
	}

	@Override
	public boolean canBeAddedOrRemovedNow(ShipAPI ship, MarketAPI marketOrNull, CampaignUIAPI.CoreUITradeMode mode) {
		if(preventRemoveOrAdd(ship, myID,COSTREDUCTION,WEAPONTYPES,VALIDSIZES,ISBEAM,marketOrNull)){return false;}
		else{return super.canBeAddedOrRemovedNow(ship,marketOrNull,mode);}
	}

	@Override
	public String getCanNotBeInstalledNowReason(ShipAPI ship, MarketAPI marketOrNull, CampaignUIAPI.CoreUITradeMode mode) {
		String buffer=addRemoveReasonResolve(ship,myID,COSTREDUCTION,WEAPONTYPES,VALIDSIZES,ISBEAM,marketOrNull);
		if(buffer==null){buffer=super.getCanNotBeInstalledNowReason(ship,marketOrNull,mode);}
		return buffer;
	}
}