package kheTechMod.combat.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
//copied straight from Quality Captains, basically, as this part does the exact same thing; tracking whether the player has beaten an omega ship.
// this is a requirement for the warped omega bounty. and I do it this way because...well...it's the same shit.
@SuppressWarnings("unused")
public class KheCombatListener implements FleetEventListener {
	public static final String QC_DEFEATED_OMEGA = "$defeated_omega";
	
	@Override
	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {

	}

	@Override
	public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
		if (primaryWinner.getFaction().equals(Global.getSector().getPlayerFaction())) {
			for (CampaignFleetAPI loser : battle.getNonPlayerSide()) {
				if (loser.getFaction().getId().equals(Factions.OMEGA)) {
					Global.getSector().getMemoryWithoutUpdate().set(QC_DEFEATED_OMEGA, true);
				}
			}
		}
	}
}
