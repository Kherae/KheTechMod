package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.impl.campaign.skills.NeuralLinkScript;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class KhePhaseAnchor extends BaseHullMod {
	//private static final Logger log = Logger.getLogger(KhePhaseAnchor.class);
	static String id = "khe_phase_anchor";

	private static final float CR_LOSS_MULT_FOR_EMERGENCY_DIVE = 2f;
	private static final float CLOAK_ACTIVATION_MULT = 0.0f;
	//public final float CLOAK_UPKEEP_PENALTY=2f;

	static void setNewHealth(ShipAPI ship, float amount) {
		ship.setHitpoints(amount);
		ship.setLowestHullLevelReached(amount);//ss v 0.98+ only function.
		//log.info("recorded hp: "+amount);
	}

	public static class KhePhaseAnchorScript implements AdvanceableListener, HullDamageAboutToBeTakenListener {
		public boolean emergencyDive = false;
		public boolean playedSound = false;
		public float diveProgress = 0f;
		public float recordCR = 1f;
		public final FaderUtil diveFader = new FaderUtil(1f, 1f);

		public final ShipAPI ship;

		public KhePhaseAnchorScript(ShipAPI ship) {
			this.ship = ship;
		}

		public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
			//be on the safe side.
			ShipSystemAPI cloak = ship.getPhaseCloak();
			if(cloak==null){return false;}
			if (!emergencyDive) {
				float depCost = 0f;
				if (ship.getFleetMember() != null) {
					depCost = ship.getFleetMember().getDeployCost();
				}
				float crLoss = CR_LOSS_MULT_FOR_EMERGENCY_DIVE * depCost;
				float currentCR = ship.getCurrentCR();
				float newCR = currentCR - crLoss;
				boolean canDive = newCR >= 0f;

				float hull = ship.getHitpoints();
				if (damageAmount >= hull && canDive) {
					recordCR = newCR * ship.getMaxHitpoints();
					setNewHealth(ship, recordCR);
					emergencyDive = true;
					if (!ship.isPhased()) {
						Global.getSoundPlayer().playSound("system_phase_cloak_activate", 1f, 1f, ship.getLocation(), ship.getVelocity());
					}
				}
			}

			return emergencyDive;
		}

		public void advance(float amount) {
			//be on the safe side.
			ShipSystemAPI cloak = ship.getPhaseCloak();
			if(cloak==null){return;}
			if (emergencyDive) {
				boolean lastForceFlag = false;
				Color c = cloak.getSpecAPI().getEffectColor2();
				c = Misc.setAlpha(c, 255);
				c = Misc.interpolateColor(c, Color.white, 0.5f);

				if (diveProgress == 0f) {
					if (ship.getFluxTracker().showFloaty()) {
						float timeMult = ship.getMutableStats().getTimeMult().getModifiedValue();
						Global.getCombatEngine().addFloatingTextAlways(ship.getLocation(),
								"Emergency dive!",
								NeuralLinkScript.getFloatySize(ship), c, ship,
								16f * timeMult, 3.2f / timeMult, 1f / timeMult, 0f, 0f,
								1f
						);
						setNewHealth(ship, recordCR);
					}
				}

				diveFader.advance(amount);
				ship.setRetreating(true, false);

				ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
				diveProgress += amount * cloak.getChargeUpDur();
				float curr = ship.getExtraAlphaMult();
				cloak.forceState(SystemState.IN, Math.min(1f, Math.max(curr, diveProgress)));
				ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 0f);

				if (diveProgress >= 1f) {
					if ((!playedSound) && (diveFader.isIdle())) {
						playedSound = true;
						Global.getSoundPlayer().playSound("phase_anchor_vanish", 1f, 1f, ship.getLocation(), ship.getVelocity());
					}
					diveFader.fadeOut();
					diveFader.advance(amount);
					float b = diveFader.getBrightness();
					ship.setExtraAlphaMult2(b);

					float r = ship.getCollisionRadius() * 5f;
					ship.setJitter(this, c, b, 20, r * (1f - b));

					if (diveFader.isFadedOut()) {
						setNewHealth(ship, recordCR);
						ship.getLocation().set(0, -1000000f);
						lastForceFlag = true;
					}
				}
				if (lastForceFlag) {
					setNewHealth(ship, recordCR);
				}
			}
		}
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "zero";
		if (index == 1) //return KheUtilities.lazyKheGetMultString(CLOAK_UPKEEP_PENALTY);
			/*if (index == 2)*/ return KheUtilities.lazyKheGetMultString(CR_LOSS_MULT_FOR_EMERGENCY_DIVE);
		return null;
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		if (!KheUtilities.isPhaseShip(ship, true, false, false)) {
			return false;
		}
		if (!KheUtilities.isAutomated(ship)) {
			return false;
		}
		return super.isApplicableToShip(ship);
	}

	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		if (!KheUtilities.isAutomated(ship)) {
			return "Can only be installed on automated ships.";
		}
		if (!KheUtilities.isPhaseShip(ship, true, false, false)) {
			return "Can only be installed on phase ships.";
		}
		return null;
	}

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getPhaseCloakActivationCostBonus().modifyMult(id, CLOAK_ACTIVATION_MULT);
		//stats.getPhaseCloakUpkeepCostBonus().modifyMult(id,CLOAK_UPKEEP_PENALTY);
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		if (KheUtilities.shipHasHullmod(ship, "phase_anchor")) {
			ship.getVariant().removeMod("khe_phase_anchor");
		}
		if (!KheUtilities.isAutomated(ship)) {
			ship.getVariant().removeMod("khe_phase_anchor");
		}
		ship.addListener(new KhePhaseAnchorScript(ship));
	}


	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		Color bad = Misc.getNegativeHighlightColor();
		Color good = Misc.getHighlightColor();
		// Color mid = Misc.getTextColor();
		//  Color darkBad = Misc.setAlpha(Misc.scaleColorOnly(bad, 0.4f), 175);
//        Color verygood=Misc.getStoryOptionColor();
//        Color verygood2=Misc.getStoryDarkColor();

		if (ship == null || ship.getMutableStats() == null) return;
		float opad = 10f;
		tooltip.addSectionHeading("Stats", Alignment.MID, opad);
		tooltip.addPara("Phase cloak activation cost: %s", opad, good, KheUtilities.lazyKheGetMultString(CLOAK_ACTIVATION_MULT));

		tooltip.addSectionHeading("Emergency Dive", Alignment.MID, opad);
		tooltip.addPara("Ship dives into phase space and retreats when it sustains critical damage. %s", opad, good, "No dive limits per battle.");
		tooltip.addPara("Requires %s Deployment CR to trigger, and consumes that much.", opad, bad, KheUtilities.lazyKheGetMultString(CR_LOSS_MULT_FOR_EMERGENCY_DIVE));
		tooltip.addPara("Ship will restore hull roughly equal to %s. (Somewhat less, actually) -Shut up Kevin", opad, good, "the CR level after diving");


	}
}

