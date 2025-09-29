package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.util.Collections;
import java.util.List;

public class WeaponArtilleryModuleEnergy extends WeaponArtilleryModuleUtil {
	public final String myID = "kheartillerymoduleenergy";
	public final boolean ISBEAM = false;
	//public final float FLUXCAPACITYPENALTY=1f;

	public final static List<WeaponAPI.WeaponType> WEAPONTYPES = Collections.singletonList(
			WeaponAPI.WeaponType.ENERGY
	);
	public final static List<WeaponAPI.WeaponSize> VALIDSIZES = Collections.singletonList(
			WeaponAPI.WeaponSize.LARGE
	);

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		ship.addListener(new ArtilleryModuleRangeModifier(RANGEBONUS, WEAPONTYPES, VALIDSIZES, ISBEAM));
		valueMath(id, ship.getMutableStats(), ship, WEAPONTYPES, VALIDSIZES, ISBEAM, NOOPPENALTY, FLUXPENALTYMULT);
	}

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		KheUtilities.applyCostModifiers(id, COSTREDUCTION, stats, VALIDSIZES, WEAPONTYPES, ISBEAM);
		//stats.getFluxCapacity().modifyMult(id,FLUXCAPACITYPENALTY);
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
		return descParamResolve(index, WEAPONTYPES, VALIDSIZES, NOOPPENALTY, FLUXPENALTYMULT, RANGEBONUS, COSTREDUCTION, OVERLOADTHRESHOLD, ISBEAM);
	}

	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		return unapplicableResolve(ship, WEAPONTYPES, VALIDSIZES, ISBEAM);
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return applicableResolve(ship, WEAPONTYPES, VALIDSIZES, ISBEAM);
	}

	@Override
	public boolean canBeAddedOrRemovedNow(ShipAPI ship, MarketAPI marketOrNull, CampaignUIAPI.CoreUITradeMode mode) {
		if (preventRemoveOrAdd(ship, myID, COSTREDUCTION, WEAPONTYPES, VALIDSIZES, ISBEAM, marketOrNull)) {
			return false;
		} else {
			return super.canBeAddedOrRemovedNow(ship, marketOrNull, mode);
		}
	}

	@Override
	public String getCanNotBeInstalledNowReason(ShipAPI ship, MarketAPI marketOrNull, CampaignUIAPI.CoreUITradeMode mode) {
		String buffer = addRemoveReasonResolve(ship, myID, COSTREDUCTION, WEAPONTYPES, VALIDSIZES, ISBEAM, marketOrNull);
		if (buffer == null) {
			buffer = super.getCanNotBeInstalledNowReason(ship, marketOrNull, mode);
		}
		return buffer;
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		tooltipHandler(tooltip, hullSize, ship, width, isForModSpec, WEAPONTYPES, VALIDSIZES, ISBEAM, COSTREDUCTION, RANGEBONUS, OVERLOADTHRESHOLD, NOOPPENALTY, FLUXPENALTYMULT);
	}
}