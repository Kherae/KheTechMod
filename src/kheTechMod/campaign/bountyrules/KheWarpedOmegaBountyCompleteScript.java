package kheTechMod.campaign.bountyrules;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
//import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddShip;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
//import kheTechMod.rules.KheWarpedCoreOfficerPlugin;

import java.util.List;
import java.util.Map;

public class KheWarpedOmegaBountyCompleteScript extends BaseCommandPlugin {
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
            FleetDataAPI playerFleet = Global.getSector().getPlayerFleet().getFleetData();
            String variantIdT = "khe_tesseract_player_Hull";
            String variantIdF = "khe_facet_player_Hull";
            ShipVariantAPI variantT = Global.getSettings().getVariant(variantIdT).clone();
            ShipVariantAPI variantF = Global.getSettings().getVariant(variantIdF).clone();
            variantT.setVariantDisplayName("Unattuned");
            variantF.setVariantDisplayName("Unattuned");
            FleetMemberAPI memberT = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantT);
            FleetMemberAPI memberF = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantF);
            //three is too much with how powerful they are
            //KheWarpedCoreOfficerPlugin offPlug = new KheWarpedCoreOfficerPlugin();
            //PersonAPI newCaptainT = offPlug.createPerson("khe_warped_omega_core", "khewarpedomega", null);
            //PersonAPI newCaptainF = offPlug.createPerson("khe_warped_omega_core", "khewarpedomega", null);
            //memberF.setCaptain(newCaptainF);
            //memberT.setCaptain(newCaptainT);
            memberF.setShipName("Fragment-010F0001");
            memberT.setShipName("Fragment-010F0002");
            playerFleet.addFleetMember(memberT);
            playerFleet.addFleetMember(memberF);
            AddShip.addShipGainText(memberT, dialog.getTextPanel());
            AddShip.addShipGainText(memberF, dialog.getTextPanel());
        }

        return success;
    }
}
