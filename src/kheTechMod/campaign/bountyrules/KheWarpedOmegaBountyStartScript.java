package kheTechMod.campaign.bountyrules;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;
import kheTechMod.campaign.coreofficerplugins.KheWarpedCoreOfficerPlugin;

import org.apache.log4j.Logger;

import org.magiclib.bounty.ActiveBounty;
import org.magiclib.bounty.MagicBountyCoordinator;
import org.magiclib.bounty.rulecmd.BountyScriptExample;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")//this is called in rules_csv so IDE flags as unused
public class KheWarpedOmegaBountyStartScript extends BaseCommandPlugin {
    private static final Logger log = Logger.getLogger(KheWarpedOmegaBountyStartScript.class);
    public static void fixVariant(FleetMemberAPI member) {
        if (member.getVariant().getSource() != VariantSource.REFIT) {
            ShipVariantAPI variant = member.getVariant().clone();
            variant.setOriginalVariant(null);
            variant.setHullVariantId(Misc.genUID());
            variant.setSource(VariantSource.REFIT);
            member.setVariant(variant, false, true);
        }
        member.updateStats();
    }
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        String your_bounty_id = "khewarpedomegas";
        ActiveBounty bounty;
        try {
            bounty = MagicBountyCoordinator.getInstance().getActiveBounty(your_bounty_id);

            if (bounty == null) {
                throw new NullPointerException();
            }
        } catch (Exception ex) {
            Global.getLogger(BountyScriptExample.class).error("Unable to get MagicBounty: " + your_bounty_id, ex);
            return true;
        }

        FactionAPI khewarpedomegasfaction = Global.getSector().getFaction("khewarpedomega");
        if (khewarpedomegasfaction != null) {
            khewarpedomegasfaction.setRelationship(Global.getSector().getPlayerFaction().getId(), -1f);
            Global.getSector().getPlayerFaction().setRelationship(khewarpedomegasfaction.getId(), -1f);
        }
        else {
            log.warn("why does the warped omega faction not exist?");
        }

        CampaignFleetAPI bountyFleet = bounty.getFleet();
        //later need to see about modifying the fleet commander to add the fleet CR stuff.
//        PersonAPI commander = bountyFleet.getCommander();
        List<FleetMemberAPI> fleetMemberList = bountyFleet.getFleetData().getMembersListCopy();
        for (FleetMemberAPI member : fleetMemberList) {
            KheWarpedCoreOfficerPlugin offPlug = new KheWarpedCoreOfficerPlugin();
            PersonAPI newCaptain = offPlug.createPerson("khe_warped_omega_core", "khewarpedomega", null);
            member.setCaptain(newCaptain);
            fixVariant(member);
            member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
            member.getVariant().removeTag(Tags.VARIANT_ALWAYS_RECOVERABLE);
            member.getVariant().removeTag(Tags.AUTOMATED_RECOVERABLE);
            member.getVariant().getHints().add(ShipHullSpecAPI.ShipTypeHints.UNBOARDABLE);
            member.getVariant().addTag(Tags.VARIANT_UNBOARDABLE);
            member.getVariant().addTag(Tags.VARIANT_UNRESTORABLE);
        }
        bountyFleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_FIGHT_TO_THE_LAST, true);
        return true;
    }
}
