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

public class KheMeteorDriveSubsystem extends KheDriveSubsystemBase {
	//100 was way, way too strong. oneshot a radiant, 90% of an onslaught. 10 is too weak, doesnt even touch armor.
	// 33 is a nice good number, but still too strong. oneshot an astral. 20 is big chunky..but...eh guess too much.
	public final static float MASS_MULT = 13f;

	public final static String myID = "KheMeteorDriveSubsystem";

	public static class KheMeteorDriveDeepSystem implements AdvanceableListener {
		private boolean wasOn = false;
		private boolean overloaded = false;
		private boolean invuln = false;
		private float massStart;

		public final ShipAPI ship;
		public final ShipSystemAPI shipSystem;
		public final boolean validShipSystem;
		public final int synergyLevel;

		public KheMeteorDriveDeepSystem(ShipAPI ship) {
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
				float darkenMult = 0.5f;
				if (systemState.equals(ShipSystemAPI.SystemState.IN)) {
					jitterLevel = 1f;
				} else if (systemState.equals(ShipSystemAPI.SystemState.ACTIVE)) {
					jitterRangeBonus = 10f;
					jitterLevel = 1f;
					darkenMult = 1f;
				} else if (systemState.equals(ShipSystemAPI.SystemState.OUT)) {
					jitterLevel = 1f;
				}

				int[] engineColor = KheUtilities.getAverageEngineColor(ship);
				engineColor[3] = (int) Math.floor(engineColor[3] * darkenMult);
				Color testJitterUnder = KheUtilities.iArrayToColor(engineColor);
				engineColor[3] = (int) Math.floor((float) engineColor[3] / 2f);
				Color testJitterOver = KheUtilities.iArrayToColor(engineColor);
				jitterLevel = (float) Math.sqrt(jitterLevel);

				if (synergyLevel < 2) {
					ship.setJitter(this, testJitterOver, jitterLevel, 3, 0, 0 + jitterRangeBonus);
				}
				ship.setJitterUnder(this, testJitterUnder, jitterLevel, 25, 0f, 7f + jitterRangeBonus * synergyLevel);

				if (shipSystem.isOn()) {
					if (systemState.equals(ShipSystemAPI.SystemState.IN)) {
						if (!invuln) {
							if (synergyLevel < 2) {
								stats.getFluxDissipation().modifyMult(myID, SYSTEM_FLUX_DISSIPATION_PENALTY);
							}

							invuln = KheUtilities.handleInvuln(myID, true, stats);
						}
					} else if (systemState.equals(ShipSystemAPI.SystemState.ACTIVE)) {
						if (!wasOn) {
							wasOn = true;

							massStart = ship.getMass();
							ship.setMass(massStart * MASS_MULT);

							ship.setOverloadColor(testJitterUnder);
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

					ship.setMass(massStart);

					if (invuln) {
						invuln = KheUtilities.handleInvuln(myID, false, stats);
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
					stats.getFluxDissipation().unmodify(myID);
				} else if (overloaded) {
					stats.getFluxDissipation().unmodify(myID);

					if (synergyLevel >= 2) {
						KheUtilities.safeRepairAllEngines(ship);
					}

					ship.resetOverloadColor();
					overloaded = false;
				}
			}
		}
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		ship.addListener(new KheMeteorDriveSubsystem.KheMeteorDriveDeepSystem(ship));
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
		tooltip.addPara("Ship Is %s\nMass: %s",
				opad, good, "Invulnerable",
				KheUtilities.lazyKheGetMultString(MASS_MULT)
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
		if (synergyLevel >= 2) {
			tooltip.addPara("Subsystem Harmonization: %s\nEngine Repair Time: %s",
					opad, good, "No Overload", "Instant"
			);
		} else {
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
