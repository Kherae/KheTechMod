package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import java.awt.*;
import java.util.StringJoiner;

public class KheDriveSubsystemBase extends BaseHullMod {
	public final static float MAINT_PENALTY = 1.5f;
	public final static float SYSTEM_COOLDOWN_PENALTY = 2f;
	public final static float SYSTEM_FLUX_DISSIPATION_PENALTY = 0f;
	public static Object STATUS_KEY = new Object();

	public static final String[] acceptedSystemIDs = {
			"kheburndrive",
			"burndrive"
	};
	public static final String[] driveSubsystemList = {
			"khemeteordrivesubsystem",
			"khephasedrivesubsystem",
			"kheglaredrivesubsystem",
			"khelightwavedrivesubsystem"
	};

	@Override
	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getSuppliesPerMonth().modifyMult(id, MAINT_PENALTY);
	}

	public static int getSynergyLevel(ShipAPI ship) {
		if (ship == null) {
			return 0;
		}
		int synergyLevel = 0;
		for (String idToCheck : driveSubsystemList) {
			if (KheUtilities.shipHasHullmod(ship, idToCheck)) {
				synergyLevel++;
			}
		}
		return Math.max(synergyLevel, 1);
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		ship.getMutableStats().getSystemCooldownBonus().modifyMult(id, KheUtilities.multiModScalarHeadache(SYSTEM_COOLDOWN_PENALTY, getSynergyLevel(ship)));
	}

	public boolean applicableResolve(ShipAPI ship, String[] acceptedSystemIDs) {
		if (ship == null) {
			return false;
		}
		if (ship.getSystem() == null) {
			return false;
		}
		if (!KheUtilities.stringArrayContains(acceptedSystemIDs, ship.getSystem().getId())) {
			return false;
		}
		if (KheUtilities.isPhaseShip(ship, true, true, false)) {
			return false;
		}
		if (KheUtilities.isShielded(ship, true, false)) {
			return false;
		}
		return super.isApplicableToShip(ship);
	}

	public String getUnapplicableReasonResolve(ShipAPI ship, String[] acceptedSystemIDs) {
		if (ship == null) {
			return null;
		}
		if (ship.getSystem() == null) {
			return "Ship has no shipsystem. READ THE MODDING GUIDELINES ON THE FORUM.";
		}
		if (!KheUtilities.stringArrayContains(acceptedSystemIDs, ship.getSystem().getId())) {
			StringJoiner sj = new StringJoiner(", ");
			for (String sysID : acceptedSystemIDs) {
				sj.add(Global.getSettings().getShipSystemSpec(sysID).getName());
			}

			return "Ship does not have one of the following ship systems: " + sj;
		}
		//too unbalanced otherwise...
		boolean isPhase = KheUtilities.isPhaseShip(ship, true, true, true);
		boolean isShielded = KheUtilities.isShielded(ship, true, false);
		if (isPhase) {
			return "Cannot be installed on phase ships or ships with a damper field.";
		} else if (isShielded) {
			return "Cannot be installed on ships with shields.";
		}
		return null;
	}

	@Override
	public boolean canBeAddedOrRemovedNow(ShipAPI ship, MarketAPI marketOrNull, CampaignUIAPI.CoreUITradeMode mode) {
		if (KheUtilities.isThisRatsExoship(marketOrNull)) {
			return false;
		}
		return super.canBeAddedOrRemovedNow(ship, marketOrNull, mode);
	}

	@Override
	public String getCanNotBeInstalledNowReason(ShipAPI ship, MarketAPI marketOrNull, CampaignUIAPI.CoreUITradeMode mode) {
		if (KheUtilities.isThisRatsExoship(marketOrNull)) {
			return KheUtilities.RATSEXOSHIPNOREMOVALSTRING;
		}
		return super.getCanNotBeInstalledNowReason(ship, marketOrNull, mode);
	}
}
