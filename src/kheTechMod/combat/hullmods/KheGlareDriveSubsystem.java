package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

public class KheGlareDriveSubsystem extends KheDriveSubsystemBase {
	public final static float WEAPON_REPAIR_TIME_OVERLOAD = 0.01f;
	public static final int MIN_AMMO_RESTORE = 1;
	public static final float AMMO_RESTORE_PERCENT = 0.1f;
	//public static final float AMMO_REGEN_MULT = 10f;

	public final static String myID = "KheGlareDriveSubsystem";

	public static class KheGlareDriveDeepSystem implements AdvanceableListener {
		private boolean started = false;
		private boolean wasOn = false;
		private boolean overloaded = false;

		public final ShipAPI ship;
		public final ShipSystemAPI shipSystem;
		public final boolean validShipSystem;
		public final int synergyLevel;

		public KheGlareDriveDeepSystem(ShipAPI ship) {
			this.synergyLevel = getSynergyLevel(ship);
			this.ship = ship;
			this.shipSystem = ship.getSystem();
			this.validShipSystem = KheUtilities.stringArrayContains(acceptedSystemIDs, (this.shipSystem) != null ? this.shipSystem.getId() : "");
		}

		@Override
		public void advance(float amount) {
			if (validShipSystem) {
				MutableShipStatsAPI stats = ship.getMutableStats();
				ShipSystemAPI.SystemState systemState = shipSystem.getState();

				if (shipSystem.isOn()) {
					if (systemState.equals(ShipSystemAPI.SystemState.IN)) {
						if (!started) {
							if (synergyLevel < 2) {
								stats.getFluxDissipation().modifyMult(myID, SYSTEM_FLUX_DISSIPATION_PENALTY);
							}

							started = true;
						}
					}
					if (systemState.equals(ShipSystemAPI.SystemState.ACTIVE)) {
						if (!wasOn) {
							wasOn = true;

//							stats.getMissileAmmoRegenMult().modifyMult(myID,AMMO_REGEN_MULT);
//							stats.getEnergyAmmoRegenMult().modifyMult(myID,AMMO_REGEN_MULT);
//							stats.getBallisticAmmoRegenMult().modifyMult(myID,AMMO_REGEN_MULT);

							for (WeaponAPI weapon : ship.getAllWeapons()) {
								if (weapon.usesAmmo()) {
									int currentAmmo = weapon.getAmmo();
									int maxAmmo = weapon.getMaxAmmo();
									if (currentAmmo < maxAmmo) {
										int toRestore = Math.max((int) (maxAmmo * AMMO_RESTORE_PERCENT * synergyLevel), MIN_AMMO_RESTORE * synergyLevel);
										toRestore = Math.min(toRestore, maxAmmo - currentAmmo);
										Global.getCombatEngine().addFloatingText(
												weapon.getLocation(), "+" + toRestore, 25.0F, Color.GREEN, ship, 0.0F, 0.0F
										);
										weapon.setAmmo(Math.min(maxAmmo, currentAmmo + toRestore));
									}
									weapon.setRemainingCooldownTo(0);
								}
							}
						}

						Global.getCombatEngine().maintainStatusForPlayerShip(
								STATUS_KEY, shipSystem.getSpecAPI().getIconSpriteName(), "BURN DRIVE SUBSYSTEM", "WEAPONS DISABLED", true
						);
						KheUtilities.setForceNoFireOneFrame(ship);
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
					if (synergyLevel < 2) {
						stats.getFluxDissipation().unmodify(myID);
					}
				} else if (overloaded) {
					if (synergyLevel >= 2) {
						KheUtilities.safeRepairAllWeapons(ship);
					}
					stats.getCombatWeaponRepairTimeMult().unmodify(myID);

					if (synergyLevel < 2) {
						stats.getFluxDissipation().unmodify(myID);
					}

					started = false;

//					ship.getMutableStats().getBallisticAmmoRegenMult().unmodify(myID);
//					ship.getMutableStats().getEnergyAmmoRegenMult().unmodify(myID);
//					ship.getMutableStats().getMissileAmmoRegenMult().unmodify(myID);

					overloaded = false;
				} else if (started) {
					if (synergyLevel < 2) {
						stats.getFluxDissipation().unmodify(myID);
					}
					started = false;
				}
			}
		}
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		ship.addListener(new KheGlareDriveSubsystem.KheGlareDriveDeepSystem(ship));
		super.applyEffectsAfterShipCreation(ship, id);
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		if (ship == null || ship.getMutableStats() == null) return;

		Color bad = Misc.getNegativeHighlightColor();
		Color good = Misc.getHighlightColor();
		float opad = 10f;

		int synergyLevel = getSynergyLevel(ship);

		tooltip.addSectionHeading("Stats", Alignment.MID, opad);
		tooltip.addPara("Ship System Cooldown: %s\nSupplies Per Month: %s",
				opad, bad,
				KheUtilities.lazyKheGetMultString(KheUtilities.multiModScalarHeadache(SYSTEM_COOLDOWN_PENALTY, synergyLevel)),
				KheUtilities.lazyKheGetMultString(MAINT_PENALTY)
		);
		tooltip.addSectionHeading("Burn Drive Stats", Alignment.MID, opad);
		tooltip.addPara("On reaching full charge: Ship restores %s ammo for all equipped weapons, minimum %s",//\n"//+
				//"All Ammo Reload Speed: %s",
				opad, good,
				KheUtilities.lazyKheGetPercentString(AMMO_RESTORE_PERCENT * synergyLevel * 100), "" + MIN_AMMO_RESTORE * synergyLevel//,
				//KheUtilities.lazyKheGetMultString(AMMO_REGEN_MULT)
		);
		if (synergyLevel >= 2) {
			tooltip.addPara("Weapons: %s",
					opad, bad, "Disabled"
			);
		} else {
			tooltip.addPara("Flux Dissipation: %s\nWeapons: %s",
					opad, bad,
					KheUtilities.lazyKheGetMultString(SYSTEM_FLUX_DISSIPATION_PENALTY), "Disabled"
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
		return applicableResolve(ship, acceptedSystemIDs);
	}

	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		return getUnapplicableReasonResolve(ship, acceptedSystemIDs);
	}
}