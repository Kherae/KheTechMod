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

public class KhePhaseBurnDriveSubsystem extends KheDriveSubsystemBase {
	public final static float TIMEFLOW_MULT = 15f;
	public static final Color JITTER_COLOR = new Color(90, 165, 255, 55);
	public static final Color JITTER_UNDER_COLOR = new Color(90, 165, 255, 155);

	public final static String myID = "KhePhaseBurnDriveSystem";
	protected static Object STATUSKEYFLUXTIMEWARPA = new Object();
	public static final String[] acceptedSystemIDs = {
			"burndrive"
	};

	public static class KhePhaseBurnDriveSystemListener implements AdvanceableListener {
		private boolean started = false;
		private boolean wasOn = false;
		private boolean overloaded = false;
		private boolean invuln = false;

		public final ShipAPI ship;
		public final ShipSystemAPI shipSystem;
		public final boolean validShipSystem;
		public final int synergyLevel;

		public KhePhaseBurnDriveSystemListener(ShipAPI ship) {
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
					if (systemState.equals(ShipSystemAPI.SystemState.IN)) {
						if (!started) {
							if (synergyLevel < 2) {
								stats.getFluxDissipation().modifyMult(myID, SYSTEM_FLUX_DISSIPATION_PENALTY);
							}

							started = true;
						}
					} else if (systemState.equals(ShipSystemAPI.SystemState.ACTIVE)) {
						if (!wasOn) {
							wasOn = true;

							invuln = KheUtilities.handleInvuln(myID, true, stats);
							ship.setPhased(true);

							ship.setOverloadColor(JITTER_UNDER_COLOR);

							if (synergyLevel < 2) {
								stats.getFluxDissipation().modifyMult(myID, SYSTEM_FLUX_DISSIPATION_PENALTY);
							}
						}

						KheUtilities.handleTimeFlow(true, myID, ship, TIMEFLOW_MULT * synergyLevel);
						Global.getCombatEngine().maintainStatusForPlayerShip(
								STATUSKEYFLUXTIMEWARPA, shipSystem.getSpecAPI().getIconSpriteName(),
								"phase-burn", KheUtilities.lazyKheGetMultString(TIMEFLOW_MULT * synergyLevel) + " timeflow.", false
						);

						Global.getCombatEngine().maintainStatusForPlayerShip(
								STATUS_KEY, shipSystem.getSpecAPI().getIconSpriteName(), "BURN DRIVE SUBSYSTEM", "WEAPONS DISABLED", true
						);
						KheUtilities.setForceNoFireOneFrame(ship);
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
					}
				} else if (ship.getFluxTracker().isVenting()) {
					if (synergyLevel < 2) {
						stats.getFluxDissipation().unmodify(myID);
					}
				} else if (overloaded) {
					if (synergyLevel < 2) {
						stats.getFluxDissipation().unmodify(myID);
					}

					started = false;

					ship.resetOverloadColor();
					overloaded = false;
				} else if (started) {
					started = false;

					if (synergyLevel < 2) {
						stats.getFluxDissipation().unmodify(myID);
					}
				}
			}
		}
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		ship.addListener(new KhePhaseBurnDriveSubsystem.KhePhaseBurnDriveSystemListener(ship));
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
		tooltip.addPara("Ship Is %s\nTimeflow: %s", opad, good, "Phased", KheUtilities.lazyKheGetMultString(TIMEFLOW_MULT * synergyLevel));
		if (synergyLevel >= 2) {
			tooltip.addPara("Weapons: %s", opad, bad, "Disabled");
		} else {
			tooltip.addPara("Flux Dissipation: %s\nWeapons: %s", opad, bad, KheUtilities.lazyKheGetMultString(SYSTEM_FLUX_DISSIPATION_PENALTY), "Disabled");
		}
		tooltip.addSectionHeading("Burn Drive Overload Stats", Alignment.MID, opad);
		if (synergyLevel > 1) {
			tooltip.addPara("Subsystem Harmonization: %s", opad, good, "No Overload");
		} else {
			tooltip.addPara("Flux Dissipation: %s", opad, bad, KheUtilities.lazyKheGetMultString(SYSTEM_FLUX_DISSIPATION_PENALTY));
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
