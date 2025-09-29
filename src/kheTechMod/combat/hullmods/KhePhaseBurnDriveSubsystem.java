package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class KhePhaseBurnDriveSubsystem extends BaseHullMod {
	public final static float MAINT_PENALTY = 2f;
	public final static float SYSTEM_COOLDOWN_PENALTY = 2f;
	public final static float SYSTEM_FLUX_DISSIPATION_PENALTY = 0f;
	public final static float WEAPON_REPAIR_TIME_STALL = 100f;
	public final static float WEAPON_REPAIR_TIME_OVERLOAD = 0.01f;

	public final static float TIMEFLOW_MULT = 15f;
	public static final Color JITTER_COLOR = new Color(90, 165, 255, 55);
	public static final Color JITTER_UNDER_COLOR = new Color(90, 165, 255, 155);

	public final static String myID = "KhePhaseBurnDriveSystem";
	protected static Object STATUSKEYFLUXTIMEWARPA = new Object();
	public static final String[] acceptedSystemIDs = {
			"burndrive"
	};
	public static final String[] driveSubsystemList = {
			"khemeteordrivesubsystem",
			"khephasedrivesubsystem",
			"kheglaredrivesubsystem",
			"khelightwavedrivesubsystem"
	};

	public static class KhePhaseBurnDriveSystemListener implements AdvanceableListener, HullDamageAboutToBeTakenListener {
		private boolean wasOn = false;
		private boolean overloaded = false;
		private boolean invuln = false;

		public final ShipAPI ship;
		public final ShipSystemAPI shipSystem;
		public final boolean validShipSystem;
		public final int synergyLevel;

		public KhePhaseBurnDriveSystemListener(ShipAPI ship) {
			int synergyLevel = 0;
			for (String idToCheck : driveSubsystemList) {
				if (KheUtilities.shipHasHullmod(ship, idToCheck)) {
					synergyLevel++;
				}
			}
			this.synergyLevel = Math.max(synergyLevel, 1);
			this.ship = ship;
			this.shipSystem = ship.getSystem();
			this.validShipSystem = KheUtilities.stringArrayContains(acceptedSystemIDs, (this.shipSystem) != null ? this.shipSystem.getId() : "");
		}

		//black magic. I don't wanna remove it since for some reason everything falls apart if I do.
		public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
			return false;
		}

		@Override
		public void advance(float amount) {
			if (validShipSystem) {
				MutableShipStatsAPI stats = ship.getMutableStats();
				ShipSystemAPI.SystemState systemState = shipSystem.getState();

				float jitterLevel = 0f;
				float jitterRangeBonus = 0f;
				if (systemState.equals(ShipSystemAPI.SystemState.IN)) {
					jitterLevel = (float) Math.sqrt(1f);
				} else if (systemState.equals(ShipSystemAPI.SystemState.ACTIVE)) {
					jitterRangeBonus = 10f;
					jitterLevel = (float) Math.sqrt(1f);
				} else if (systemState.equals(ShipSystemAPI.SystemState.OUT)) {
					jitterLevel = (float) Math.sqrt(1f);
				}

				if (synergyLevel < 2) {
					ship.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, 25, 0f, 7f + jitterRangeBonus);
				}
				ship.setJitter(this, JITTER_COLOR, jitterLevel, 3, 0, 0 + jitterRangeBonus * synergyLevel);

				if (shipSystem.isOn()) {
					if (synergyLevel < 2) {
						stats.getFluxDissipation().modifyMult(myID, SYSTEM_FLUX_DISSIPATION_PENALTY);
					}
//                    if(systemState.equals(ShipSystemAPI.SystemState.IN)){
//
//                    }else
					if (systemState.equals(ShipSystemAPI.SystemState.ACTIVE)) {
						if (!wasOn) {
							wasOn = true;

							stats.getCombatWeaponRepairTimeMult().modifyMult(myID, WEAPON_REPAIR_TIME_STALL);
							for (WeaponAPI weapon : ship.getAllWeapons()) {
								weapon.disable();
							}

							invuln = KheUtilities.handleInvuln(myID, true, stats);
							ship.setPhased(true);

							ship.setOverloadColor(JITTER_UNDER_COLOR);

							if (synergyLevel < 2) {
								stats.getFluxDissipation().modifyMult(myID, SYSTEM_FLUX_DISSIPATION_PENALTY);
							}
						}
						//moved timeflow modification here due to effect cleanup safety, due to registry. was a do-once-per-use.
						//technically, because it applied 30x timeflow at base before, the extra synergy levels were just for 'fluff' and didn't really matter that much.
						//so...reduced.
						KheUtilities.handleTimeFlow(true, myID, ship, TIMEFLOW_MULT * synergyLevel);
						Global.getCombatEngine().maintainStatusForPlayerShip(
								STATUSKEYFLUXTIMEWARPA, shipSystem.getSpecAPI().getIconSpriteName(),
								"phase-burn", KheUtilities.lazyKheGetMultString(TIMEFLOW_MULT * synergyLevel) + " timeflow.", false
						);
					}
				} else if (wasOn) {
					wasOn = false;

					KheUtilities.handleTimeFlow(false, myID, ship, 1f);
					ship.setPhased(false);
					invuln = KheUtilities.handleInvuln(myID, false, stats);

					overloaded = true;
					if (synergyLevel < 2) {
						ship.getFluxTracker().forceOverload(0f);
					}
				} else if (invuln) {
					invuln = KheUtilities.handleInvuln(myID, false, stats);
				} else if (ship.getFluxTracker().isOverloaded()) {
					if (overloaded) {
						if (synergyLevel < 2) {
							stats.getFluxDissipation().modifyMult(myID, SYSTEM_FLUX_DISSIPATION_PENALTY);
						}

						stats.getCombatWeaponRepairTimeMult().modifyMult(myID, WEAPON_REPAIR_TIME_OVERLOAD);
					}
				} else if (ship.getFluxTracker().isVenting()) {
					stats.getFluxDissipation().unmodify(myID);
				} else if (overloaded) {
					if (synergyLevel >= 2) {
						for (WeaponAPI weapon : ship.getAllWeapons()) {
							if (weapon.isDisabled()) {
								weapon.repair();
							}
						}
					}

					stats.getCombatWeaponRepairTimeMult().unmodify(myID);

					stats.getFluxDissipation().unmodify(myID);

					ship.resetOverloadColor();
					overloaded = false;
				}
			}
		}
	}

	@Override
	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getSuppliesPerMonth().modifyMult(id, MAINT_PENALTY);
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		ship.addListener(new KhePhaseBurnDriveSubsystem.KhePhaseBurnDriveSystemListener(ship));
		int synergyLevel = 0;
		for (String idToCheck : driveSubsystemList) {
			if (KheUtilities.shipHasHullmod(ship, idToCheck)) {
				synergyLevel++;
			}
		}
		synergyLevel = Math.max(synergyLevel, 1);
		ship.getMutableStats().getSystemCooldownBonus().modifyMult(id, KheUtilities.multiModScalarHeadache(SYSTEM_COOLDOWN_PENALTY, synergyLevel));
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		if (ship == null || ship.getMutableStats() == null) return;

		Color bad = Misc.getNegativeHighlightColor();
		Color good = Misc.getHighlightColor();
		float opad = 10f;

		int synergyLevel = 0;
		for (String idToCheck : driveSubsystemList) {
			if (KheUtilities.shipHasHullmod(ship, idToCheck)) {
				synergyLevel++;
			}
		}
		synergyLevel = Math.max(synergyLevel, 1);

		tooltip.addSectionHeading("Stats", Alignment.MID, opad);
		tooltip.addPara("Ship System Cooldown: %s\nSupplies Per Month: %s",
				opad, bad,
				KheUtilities.lazyKheGetMultString(KheUtilities.multiModScalarHeadache(SYSTEM_COOLDOWN_PENALTY, synergyLevel)),
				KheUtilities.lazyKheGetMultString(MAINT_PENALTY)
		);
		tooltip.addSectionHeading("Burn Drive Stats", Alignment.MID, opad);
		tooltip.addPara("Ship Is %s\nTimeflow: %s",
				opad, good,
				"Phased", KheUtilities.lazyKheGetMultString(TIMEFLOW_MULT * synergyLevel)
		);
		if (synergyLevel >= 2) {
			tooltip.addPara("Weapon Repair Time: %s",
					opad, bad,
					KheUtilities.lazyKheGetMultString(WEAPON_REPAIR_TIME_STALL)
			);
		} else {
			tooltip.addPara("Flux Dissipation: %s\nWeapon Repair Time: %s",
					opad, bad,
					KheUtilities.lazyKheGetMultString(SYSTEM_FLUX_DISSIPATION_PENALTY),
					KheUtilities.lazyKheGetMultString(WEAPON_REPAIR_TIME_STALL)
			);
		}
		tooltip.addSectionHeading("Burn Drive Overload Stats", Alignment.MID, opad);
		if (synergyLevel > 1) {
			tooltip.addPara("Subsystem Harmonization: %s\nWeapon Repair Time: %s",
					opad, good, "No Overload", "Instant"
			);
		} else {
			tooltip.addPara("Weapon Repair Time: %s",
					opad, good,
					KheUtilities.lazyKheGetMultString(WEAPON_REPAIR_TIME_OVERLOAD)
			);
			tooltip.addPara("Flux Dissipation: %s",
					opad, bad,
					KheUtilities.lazyKheGetMultString(SYSTEM_FLUX_DISSIPATION_PENALTY)
			);
		}
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
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

	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		if (ship == null) {
			return null;
		}
		if (ship.getSystem() == null) {
			return "Ship has no shipsystem. READ THE MODDING GUIDELINES ON THE FORUM.";
		}
		if (!KheUtilities.stringArrayContains(acceptedSystemIDs, ship.getSystem().getId())) {
			return "Ship does not have Burn Drive.";
		}
		//too unbalanced otherwise...
		boolean isPhase = KheUtilities.isPhaseShip(ship, true, true, true);
		boolean isShielded = KheUtilities.isShielded(ship, true, false);
		if (isPhase) {
			return "Cannot be installed on phase ships or ships with a damper field.";
		} else if (isShielded) {
//			return "Cannot be installed on ships that natively have shields.";
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
