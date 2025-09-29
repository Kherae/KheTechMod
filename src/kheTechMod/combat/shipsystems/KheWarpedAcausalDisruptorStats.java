package kheTechMod.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.impl.combat.AcausalDisruptorStats;
import com.fs.starfarer.api.input.InputEventAPI;

import java.awt.*;
import java.util.List;

public class KheWarpedAcausalDisruptorStats extends AcausalDisruptorStats {
	public static float DISRUPTION_DUR = 1f;
	public static final float SELF_CLOAK_COOLDOWN_MULT = 2f;
	public static final float TARGET_CLOAKED_OVERLOAD_MULT = 3f;

	public static final Color OVERLOAD_COLOR = new Color(255, 155, 255, 255);

	protected void applyEffectToTarget(final ShipAPI ship, final ShipAPI target) {
		if (target.getFluxTracker().isOverloadedOrVenting()) {
			return;
		}

		if (ship.isPhased()) {
			ShipSystemAPI shipCloak = ship.getPhaseCloak();
			if (shipCloak == null) {
				shipCloak = ship.getSystem();
			}
			if (shipCloak != null) {
				if (shipCloak.isOn()) {
					shipCloak.forceState(SystemState.COOLDOWN, SELF_CLOAK_COOLDOWN_MULT);
				}
			}
		}
		if (target == ship) return;

		boolean wasTargetCloaked = false;
		ShipSystemAPI targetCloak = target.getPhaseCloak();
		if (targetCloak == null) {
			targetCloak = target.getSystem();
		}
		if (targetCloak != null) {
			if (targetCloak.isOn()) {
				wasTargetCloaked = true;
				//targetCloak.forceState(SystemState.COOLDOWN,2f);//this doesnt seem to actually work.
			}
		}

		target.setOverloadColor(OVERLOAD_COLOR);
		target.getFluxTracker().beginOverloadWithTotalBaseDuration(wasTargetCloaked ? DISRUPTION_DUR * TARGET_CLOAKED_OVERLOAD_MULT : DISRUPTION_DUR);

		if (
				target.getFluxTracker().showFloaty() ||
						ship == Global.getCombatEngine().getPlayerShip() ||
						target == Global.getCombatEngine().getPlayerShip()
		) {
			target.getFluxTracker().playOverloadSound();
			target.getFluxTracker().showOverloadFloatyIfNeeded("System Disruption!", OVERLOAD_COLOR, 4f, true);
		}

		Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
			@Override
			public void advance(float amount, List<InputEventAPI> events) {
				if (!target.getFluxTracker().isOverloadedOrVenting()) {
					target.resetOverloadColor();
					Global.getCombatEngine().removePlugin(this);
				}
			}
		});
	}
}








