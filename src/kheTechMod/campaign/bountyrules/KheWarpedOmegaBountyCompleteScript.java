package kheTechMod.campaign.bountyrules;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
//import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
//import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
//import com.fs.starfarer.api.impl.campaign.rulecmd.AddShip;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
//import kheTechMod.combat.hullmods.KheUtilities;
//import org.lwjgl.util.vector.Vector2f;
//import kheTechMod.rules.KheWarpedCoreOfficerPlugin;
//import kheTechMod.combat.hullmods.KheEmptyHullmod;
//import org.magiclib.bounty.MagicBountyFleetEncounterContext;
//import static org.magiclib.util.MagicCampaign.createDerelict;
//import org.lazywizard.lazylib.MathUtils;
//import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Map;


import org.apache.log4j.Logger;

@SuppressWarnings("unused")//this is called in rules_csv so IDE flags as unused
public class KheWarpedOmegaBountyCompleteScript extends BaseCommandPlugin {
    private final Logger log = Logger.getLogger(KheWarpedOmegaBountyCompleteScript.class);
    public final String myKey="$khewarpedomegasbountykey";
    @Override


    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        //this...should always be true?
        boolean success = (boolean) Global.getSector().getMemoryWithoutUpdate().get(myKey+"_succeeded");
        //this will never be true, lol, it doesnt expire.
//        "_expired"
        //cant do this the way I want to.
//        "_failed"

        if(success){
            String variantIdT = "khe_tesseract_Hull";
            String variantIdF = "khe_facet_Hull";
            ShipVariantAPI variantT = Global.getSettings().getVariant(variantIdT).clone();
            ShipVariantAPI variantF = Global.getSettings().getVariant(variantIdF).clone();
            variantT.setVariantDisplayName("Unattuned");
            variantF.setVariantDisplayName("Unattuned");
            FleetMemberAPI memberT = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantT);
            FleetMemberAPI memberF = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantF);

            CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
            FleetDataAPI playerFleetData = playerFleet.getFleetData();
            LocationAPI playerloc = playerFleetData.getFleet().getContainingLocation();

            CustomCampaignEntityAPI shipF = (CustomCampaignEntityAPI) BaseThemeGenerator.addSalvageEntity(
                playerloc, Entities.WRECK, Factions.NEUTRAL, new DerelictShipEntityPlugin.DerelictShipData(
                    new ShipRecoverySpecial.PerShipData(
                        variantF, ShipRecoverySpecial.ShipCondition.WRECKED,"Fragment-010F0001","khewarpedomega",0
                    ), false
                )
            );
            shipF.getMemoryWithoutUpdate().set(MemFlags.ENTITY_MISSION_IMPORTANT, true);
            CustomCampaignEntityAPI shipT = (CustomCampaignEntityAPI) BaseThemeGenerator.addSalvageEntity(
                playerloc, Entities.WRECK, Factions.NEUTRAL,new DerelictShipEntityPlugin.DerelictShipData(
                    new ShipRecoverySpecial.PerShipData(
                        variantT, ShipRecoverySpecial.ShipCondition.WRECKED,"Fragment-010F0002","khewarpedomega",0
                    ), false
                )
            );
            shipF.getLocation().x = playerFleet.getLocation().x + (50f - (float) Math.random() * 100f);
            shipT.getLocation().x = playerFleet.getLocation().x + (50f - (float) Math.random() * 100f);
            shipF.getLocation().y = playerFleet.getLocation().y + (50f - (float) Math.random() * 100f);
            shipT.getLocation().y = playerFleet.getLocation().y + (50f - (float) Math.random() * 100f);
            shipF.setFacing((float)Math.random()*360f);
            shipT.setFacing((float)Math.random()*360f);
            Misc.makeImportant(shipF,"Reward fleet from a special bounty, the unyielding peanut.");
            Misc.makeImportant(shipT,"Reward fleet from a special bounty, the headache inducing ghost.");
            shipT.getMemoryWithoutUpdate().set(MemFlags.ENTITY_MISSION_IMPORTANT, true);
            shipF.getMemoryWithoutUpdate().set(MemFlags.ENTITY_MISSION_IMPORTANT, true);
            shipF.setDiscoverable(true);
            shipT.setDiscoverable(true);

//            SpecialItemData fart1=new SpecialItemData("item_modspec","khe_phase_anchor");
//            SpecialItemData fart2=new SpecialItemData("item_modspec","khephasestasiso");
//            shipT.getCargo().addSpecial(fart1,1000);
//            shipT.getCargo().addSpecial(fart2,1000);
        }

        return success;
    }
}
