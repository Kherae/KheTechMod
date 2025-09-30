package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

public class KheLightwaveDriveSubsystem extends KheDriveSubsystemBase {
	public final static float ENGINE_REPAIR_TIME_OVERLOAD = 0.01f;
	public static final float ACCELERATION_MULT = 2f;
	public static final float TOP_SPEED_MULT = 2f;
	public static final float ACCELERATION_MULT_PER_SYNERGY = 1f;
	public static final float TOP_SPEED_MULT_PER_SYNERGY = 1f;

	public final static String myID = "KheLightwaveDriveSubsystem";
	public static final String[] acceptedSystemIDs = {
			"burndrive"
	};

	public static class KheLightwaveDriveDeepSystem implements AdvanceableListener {
		private boolean started = false;
		private boolean wasOn = false;
		private boolean overloaded = false;

		public final ShipAPI ship;
		public final ShipSystemAPI shipSystem;
		public final boolean validShipSystem;
		public final int synergyLevel;

		public KheLightwaveDriveDeepSystem(ShipAPI ship) {
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

							stats.getAllowZeroFluxAtAnyLevel().modifyFlat(myID, 1);
							stats.getMaxSpeed().modifyMult(myID, TOP_SPEED_MULT + (TOP_SPEED_MULT_PER_SYNERGY * synergyLevel));
							stats.getAcceleration().modifyMult(myID, ACCELERATION_MULT + (ACCELERATION_MULT_PER_SYNERGY * synergyLevel));

							started = true;
						}
					}
					ship.getEngineController().extendFlame(this, 4, 5, 3);

					if (systemState.equals(ShipSystemAPI.SystemState.ACTIVE)) {
						if (!wasOn) {
							wasOn = true;
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
						stats.getCombatEngineRepairTimeMult().modifyMult(myID, ENGINE_REPAIR_TIME_OVERLOAD);
					}
				} else if (ship.getFluxTracker().isVenting()) {
					if (synergyLevel < 2) {
						stats.getFluxDissipation().unmodify(myID);
					}
				} else if (overloaded) {
					if (synergyLevel < 2) {
						stats.getFluxDissipation().unmodify(myID);
					}

					stats.getAllowZeroFluxAtAnyLevel().unmodify(myID);
					stats.getMaxSpeed().unmodify(myID);
					stats.getAcceleration().unmodify(myID);
					stats.getCombatEngineRepairTimeMult().unmodify(myID);

					started = false;

					if (synergyLevel >= 2) {
						KheUtilities.safeRepairAllEngines(ship);
					}

					overloaded = false;
				} else if (started) {
					started = false;

					stats.getAllowZeroFluxAtAnyLevel().unmodify(myID);
					stats.getMaxSpeed().unmodify(myID);
					stats.getAcceleration().unmodify(myID);

					if (synergyLevel < 2) {
						stats.getFluxDissipation().unmodify(myID);
					}
				}
			}
		}
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		ship.addListener(new KheLightwaveDriveSubsystem.KheLightwaveDriveDeepSystem(ship));
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
		tooltip.addPara("Top speed: %s\n" +
						"Acceleration: %s",
				opad, good,
				KheUtilities.lazyKheGetMultString(TOP_SPEED_MULT + (TOP_SPEED_MULT_PER_SYNERGY * synergyLevel)),
				KheUtilities.lazyKheGetMultString(ACCELERATION_MULT + (ACCELERATION_MULT_PER_SYNERGY * synergyLevel))
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
			tooltip.addPara("Subsystem Harmonization: %s\nEngine Repair Time: %s",
					opad, good, "No Overload", "Instant"
			);
		} else {
			tooltip.addPara("Engine Repair Time: %s",
					opad, good,
					KheUtilities.lazyKheGetMultString(ENGINE_REPAIR_TIME_OVERLOAD)
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
