package kheTechMod;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import kheTechMod.combat.plugins.KheCombatListener;

import java.util.List;

@SuppressWarnings("unused")
public class KheTechMain extends BaseModPlugin {

	@Override
	public void onGameLoad(boolean newGame) {
		super.onGameLoad(newGame);
		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
		if (playerFleet != null) {
			List<FleetEventListener> listeners = playerFleet.getEventListeners();
			boolean hasListener = false;
			for (FleetEventListener listener : listeners) {
				if (listener.getClass().equals(KheCombatListener.class)) {
					hasListener = true;
					break;
				}
			}
			if (!hasListener) {
				playerFleet.addEventListener(new KheCombatListener());
			}
		}
		Global.getSector().registerPlugin(new KheWarpedCoreCampaignPlugin());
	}

}
