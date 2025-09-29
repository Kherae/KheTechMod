package kheTechMod.combat.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

//same as Quality Captains' implementation; tracking whether the player has beaten an omega ship, the requirement for the warped omega bounty to show.
@SuppressWarnings("unused")
public class KheCombatListener implements FleetEventListener {
	@Override
	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {}

	@Override
	public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
		if (primaryWinner.getFaction().equals(Global.getSector().getPlayerFaction())) {
			for (CampaignFleetAPI loser : battle.getNonPlayerSide()) {
				if (loser.getFaction().getId().equals(Factions.OMEGA)) {
					Global.getSector().getMemoryWithoutUpdate().set("$defeated_omega", true);
				}
                if (loser.getFaction().getId().equals(Factions.REMNANTS)) {
                    Global.getSector().getMemoryWithoutUpdate().set("$defeated_remnant", true);
                }
			}
		}
	}
}
