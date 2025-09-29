package kheTechMod.combat.hullmods;

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

public class KheGlareDriveSubsystem extends BaseHullMod {
	public final static float MAINT_PENALTY = 1.5f;
	public final static float SYSTEM_COOLDOWN_PENALTY = 2f;
	public final static float SYSTEM_FLUX_DISSIPATION_PENALTY = 0f;
	public final static float WEAPON_REPAIR_TIME_STALL = 100f;
	public final static float WEAPON_REPAIR_TIME_OVERLOAD = 0.01f;

	public static final int MIN_AMMO_RESTORE = 1;
	public static final float AMMO_RESTORE_PERCENT = 0.1f;
	public static final float AMMO_REGEN_MULT = 2f;

	public final static String myID = "KheGlareDriveSubsystem";
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

	public static class KheGlareDriveDeepSystem implements AdvanceableListener, HullDamageAboutToBeTakenListener {
		private boolean wasOn = false;
		private boolean overloaded = false;

		public final ShipAPI ship;
		public final ShipSystemAPI shipSystem;
		public final boolean validShipSystem;
		public final int synergyLevel;

		public KheGlareDriveDeepSystem(ShipAPI ship) {
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

				if (shipSystem.isOn()) {
					if (synergyLevel < 2) {
						stats.getFluxDissipation().modifyMult(myID, SYSTEM_FLUX_DISSIPATION_PENALTY);
					}

					if (systemState.equals(ShipSystemAPI.SystemState.ACTIVE)) {
						if (!wasOn) {
							wasOn = true;

							stats.getCombatWeaponRepairTimeMult().modifyMult(myID, WEAPON_REPAIR_TIME_STALL);
							for (WeaponAPI weapon : ship.getAllWeapons()) {
								weapon.disable();
								if (weapon.usesAmmo()) {
									int currentAmmo = weapon.getAmmo();
									int maxAmmo = weapon.getMaxAmmo();
									if (currentAmmo < maxAmmo) {
										int toRestore = Math.max((int) (maxAmmo * AMMO_RESTORE_PERCENT * synergyLevel), MIN_AMMO_RESTORE * synergyLevel);
										currentAmmo = Math.min(maxAmmo, currentAmmo + toRestore);
										weapon.setAmmo(currentAmmo);
									}
									weapon.setRemainingCooldownTo(0);
								}
							}
						}
					}
				} else if (wasOn) {
					wasOn = false;

					if (synergyLevel < 2) {
						ship.getFluxTracker().forceOverload(0f);
					}
					overloaded = true;

					if (synergyLevel < 2) {
						ship.getEngineController().forceFlameout();
					}
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

					ship.getMutableStats().getBallisticAmmoRegenMult().unmodify(myID);
					ship.getMutableStats().getEnergyAmmoRegenMult().unmodify(myID);
					ship.getMutableStats().getMissileAmmoRegenMult().unmodify(myID);

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
		ship.addListener(new KheGlareDriveSubsystem.KheGlareDriveDeepSystem(ship));
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
		tooltip.addPara("On Activation: Ship restores %s ammo for all equipped weapons, minimum %s\n" +
						"All Ammo Reload Speed: %s",
				opad, good,
				KheUtilities.lazyKheGetPercentString(AMMO_RESTORE_PERCENT * synergyLevel * 100), "" + MIN_AMMO_RESTORE * synergyLevel,
				KheUtilities.lazyKheGetMultString(AMMO_REGEN_MULT)
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
