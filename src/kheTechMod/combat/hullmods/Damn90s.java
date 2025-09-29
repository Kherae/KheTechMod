package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

public class Damn90s extends BaseHullMod {

	final static float maxOverload = 9f;
	final static float overloadPenalty = 1.9f;
	final static float autoVentThreshold = 0.9f;
	final static float ventBonus = 9f;

	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getVentRateMult().modifyPercent(id, ventBonus);
		stats.getOverloadTimeMod().modifyMult(id, overloadPenalty);
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		if (!ship.isAlive() || ship.isPiece() || ship.getFluxTracker().isVenting() || ship.isPhased()) return;

		if (ship.getFluxTracker().getOverloadTimeRemaining() > maxOverload) {
			ship.getFluxTracker().setOverloadDuration(maxOverload);
			return;
		}

		if (ship.getFluxTracker().isOverloaded() || (ship.getMutableStats().getVentRateMult().computeMultMod() <= 0) || (ship.getMutableStats().getFluxDissipation().computeMultMod() <= 0)) {
			return;
		}

		if (ship.getFluxTracker().getFluxLevel() >= autoVentThreshold) {
			//ship.giveCommand(ShipCommand.VENT_FLUX, true, 1);//do not use this.
			ship.getFluxTracker().ventFlux();
		}

	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		Color bad = Misc.getNegativeHighlightColor();
		Color good = Misc.getHighlightColor();

		if (ship == null || ship.getMutableStats() == null) return;
		float opad = 10f;
		tooltip.addSectionHeading("Stats", Alignment.MID, opad);
		tooltip.addPara("Vent rate bonus: %s\nOverload duration cap: %s", opad, good, "+" + ventBonus + "%", maxOverload + "s");
		tooltip.addPara("Overload duration: %s", opad, bad, KheUtilities.lazyKheGetMultString(overloadPenalty));
		tooltip.addSectionHeading("Automated Venting System", Alignment.MID, opad);
		tooltip.addPara("Ship automatically vents at %s flux.", opad, bad, KheUtilities.lazyKheGetPercentString(autoVentThreshold * 100f));
	}

}
