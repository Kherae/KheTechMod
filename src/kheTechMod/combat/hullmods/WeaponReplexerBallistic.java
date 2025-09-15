package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.util.Collections;

public class WeaponReplexerBallistic extends WeaponReplexerUtil {
	final String myID="khereplexerballistic";
	@Override
	public WeaponAPI.WeaponType getWeaponType(){return WeaponAPI.WeaponType.BALLISTIC;}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		valueMath(id,ship, OPMULT_SMALL,OPMULT_MEDIUM,OPMULT_LARGE, FLUXPENALTYMULT_SMALL,OPMULT_MEDIUM,OPMULT_LARGE);
	}

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		applyBeforeCreationModifiers(stats,id,COSTREDUCTION,OVERLOADPENALTYMULT);
	}

//	public String getDescriptionParam(int index, HullSize hullSize) {
//		return descParamResolve(index,COSTREDUCTION,OPMULT,FLUXPENALTYMULT,OVERLOADPENALTYMULT);
//	}

	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		return unapplicableResolve(ship);
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return applicableResolve(ship);
	}

	@Override
	public boolean canBeAddedOrRemovedNow(ShipAPI ship, MarketAPI marketOrNull, CampaignUIAPI.CoreUITradeMode mode) {
		if(preventRemoveOrAdd(ship,myID,marketOrNull)){return false;}
		return super.canBeAddedOrRemovedNow(ship,marketOrNull,mode);
	}

	@Override
	public String getCanNotBeInstalledNowReason(ShipAPI ship, MarketAPI marketOrNull, CampaignUIAPI.CoreUITradeMode mode) {
		String buffer=addRemoveReasonResolve(ship,myID,marketOrNull);
		if(buffer==null){buffer=super.getCanNotBeInstalledNowReason(ship,marketOrNull,mode);}
		return buffer;
	}

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltipHandler(tooltip,hullSize,ship,width,isForModSpec, Collections.singletonList(getWeaponType()),VALIDSIZES,COSTREDUCTION,OPMULT_SMALL,OPMULT_MEDIUM,OPMULT_LARGE,OVERLOADPENALTYMULT,FLUXPENALTYMULT_SMALL,FLUXPENALTYMULT_MEDIUM,FLUXPENALTYMULT_LARGE);
    }
}