package kheTechMod;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.*;
import kheTechMod.api.imp.campaign.KheWarpedCoreOfficerPlugin;

import java.util.Objects;

@SuppressWarnings("unused")
public class KheWarpedCoreCampaignPlugin extends BaseCampaignPlugin{
    public static final String WARPEDOMEGACOREID="";

    @Override
    public PluginPick<AICoreOfficerPlugin> pickAICoreOfficerPlugin(String commodityId) {
        if(Objects.equals(commodityId, WARPEDOMEGACOREID)){
            return new PluginPick<>(new KheWarpedCoreOfficerPlugin(), PickPriority.MOD_SET);
        }
        return super.pickAICoreOfficerPlugin(commodityId);
    }
}