package kheTechMod;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import kheTechMod.campaign.coreofficerplugins.KhePlayerWarpedCoreOfficerPlugin;

public class KheWarpedCoreCampaignPlugin extends BaseCampaignPlugin {
	public static final String WARPEDOMEGACOREID = "khe_warped_omega_core";

	@Override
	public PluginPick<AICoreOfficerPlugin> pickAICoreOfficerPlugin(String commodityId) {
		if (WARPEDOMEGACOREID.equals(commodityId)) {
			return new PluginPick<>(new KhePlayerWarpedCoreOfficerPlugin(), PickPriority.MOD_SET);
		}
		return super.pickAICoreOfficerPlugin(commodityId);
	}
}