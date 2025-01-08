package kheTechMod.combat.hullmods;

import java.awt.Color;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.impl.campaign.skills.NeuralLinkScript;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;

public class KhePhaseAnchor extends BaseHullMod {
	private static final float CR_LOSS_MULT_FOR_EMERGENCY_DIVE = 1f;

	public static class KhePhaseAnchorScript implements AdvanceableListener, HullDamageAboutToBeTakenListener {
		public final ShipAPI ship;
		public boolean emergencyDive = false;
		public float diveProgress = 0f;
		public final FaderUtil diveFader = new FaderUtil(1f, 1f);
		public KhePhaseAnchorScript(ShipAPI ship) {
			this.ship = ship;
		}
		
		public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
			
			if (!emergencyDive) {
				float depCost = 0f;
				if (ship.getFleetMember() != null) {
					depCost = ship.getFleetMember().getDeployCost();
				}
				float crLoss = CR_LOSS_MULT_FOR_EMERGENCY_DIVE * depCost;
				boolean canDive = ship.getCurrentCR() >= crLoss;

				float hull = ship.getHitpoints();
				if (damageAmount >= hull && canDive) {
					ship.setHitpoints(1f);

					if (ship.getFleetMember() != null) {
						ship.getFleetMember().getRepairTracker().applyCREvent(-crLoss, "Emergency phase dive");
					}
					emergencyDive = true;
					if (!ship.isPhased()) {
						Global.getSoundPlayer().playSound("system_phase_cloak_activate", 1f, 1f, ship.getLocation(), ship.getVelocity());
					}
				}
			}

            return emergencyDive;
        }

		public void advance(float amount) {
			String id = "phase_anchor_modifier";
			if (emergencyDive) {
				Color c = ship.getPhaseCloak().getSpecAPI().getEffectColor2();
				c = Misc.setAlpha(c, 255);
				c = Misc.interpolateColor(c, Color.white, 0.5f);
				
				if (diveProgress == 0f) {
					if (ship.getFluxTracker().showFloaty()) {
						float timeMult = ship.getMutableStats().getTimeMult().getModifiedValue();
						Global.getCombatEngine().addFloatingTextAlways(ship.getLocation(),
								"Emergency dive!",
								NeuralLinkScript.getFloatySize(ship), c, ship, 16f * timeMult, 3.2f/timeMult, 1f/timeMult, 0f, 0f,
								1f);
					}
				}
				
				diveFader.advance(amount);
				ship.setRetreating(true, false);
				
				ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
				diveProgress += amount * ship.getPhaseCloak().getChargeUpDur();
				float curr = ship.getExtraAlphaMult();
				ship.getPhaseCloak().forceState(SystemState.IN, Math.min(1f, Math.max(curr, diveProgress)));
				ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 0f);
				
				if (diveProgress >= 1f) {
					if (diveFader.isIdle()) {
						Global.getSoundPlayer().playSound("phase_anchor_vanish", 1f, 1f, ship.getLocation(), ship.getVelocity());
					}
					diveFader.fadeOut();
					diveFader.advance(amount);
					float b = diveFader.getBrightness();
					ship.setExtraAlphaMult2(b);
					
					float r = ship.getCollisionRadius() * 5f;
					ship.setJitter(this, c, b, 20, r * (1f - b));
					
					if (diveFader.isFadedOut()) {
						ship.getLocation().set(0, -1000000f);
					}
				}
			}
		}
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "zero";
		if (index == 1) return KheUtilities.lazyKheGetMultString(CR_LOSS_MULT_FOR_EMERGENCY_DIVE);
		return null;
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		if(!KheUtilities.isPhaseShip(ship,false)){return false;}
		if(!KheUtilities.isAutomated(ship)){return false;}
		return super.isApplicableToShip(ship);
	}

	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		if (!KheUtilities.isAutomated(ship)) {
			return "Can only be installed on automated ships.";
		}
		if (!KheUtilities.isPhaseShip(ship,false)) {
			return "Can only be installed on phase ships.";
		}
		return null;
	}

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getPhaseCloakActivationCostBonus().modifyMult(id, 0f);
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		if(KheUtilities.shipHasHullmod(ship,"phase_anchor")) {
			ship.getVariant().removeMod("khe_phase_anchor");
		}
		if(!KheUtilities.isAutomated(ship)) {
			ship.getVariant().removeMod("khe_phase_anchor");
		}
		ship.addListener(new KhePhaseAnchorScript(ship));
	}
}

