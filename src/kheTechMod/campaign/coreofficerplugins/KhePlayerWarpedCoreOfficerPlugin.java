package kheTechMod.campaign.coreofficerplugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.BaseAICoreOfficerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

//there's one for player owned cores and one for the bounty cores, in case I need to adjust the cores that players get without touching the bounty fleet.
public class KhePlayerWarpedCoreOfficerPlugin extends BaseAICoreOfficerPluginImpl implements AICoreOfficerPlugin {
	public static float WARPED_OMEGA_MULT = 9f;

	public PersonAPI createPerson(String aiCoreId, String factionId, Random random) {
		if (random == null) {
			new Random();
		}
		PersonAPI person = Global.getFactory().createPerson();
		person.setFaction(factionId);
		person.setAICoreId(aiCoreId);
		CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(aiCoreId);

		person.getStats().setSkipRefresh(true);

		person.setName(new FullName(spec.getName(), "", FullName.Gender.ANY));
		person.getMemoryWithoutUpdate().set("$autoPointsMult", WARPED_OMEGA_MULT);

		person.setPersonality(Personalities.RECKLESS);
		person.setPostId(null);
		person.setRankId(Ranks.SPACE_CAPTAIN);

		person.setPortraitSprite("graphics/portraits/characters/khe_warped_omega.png");
		ArrayList<String> skillsToAdd = getStrings();
		person.getStats().setLevel(skillsToAdd.size());
		for (String skill : skillsToAdd) {
			person.getStats().setSkillLevel(skill, 2);
		}
		person.getStats().setSkipRefresh(false);
		return person;
	}

	private static ArrayList<String> getStrings() {
		return new ArrayList<>(Arrays.asList(
				Skills.BALLISTIC_MASTERY,
				Skills.COMBAT_ENDURANCE,
				Skills.DAMAGE_CONTROL,
				Skills.ENERGY_WEAPON_MASTERY,
				Skills.FIELD_MODULATION,
				Skills.GUNNERY_IMPLANTS,
				Skills.HELMSMANSHIP,
				Skills.IMPACT_MITIGATION,
				Skills.MISSILE_SPECIALIZATION,
				Skills.OMEGA_ECM,
				Skills.ORDNANCE_EXPERTISE,
				Skills.POINT_DEFENSE,
				Skills.POLARIZED_ARMOR,
				Skills.SYSTEMS_EXPERTISE,
				Skills.TARGET_ANALYSIS
		));
	}

	@Override
	public void createPersonalitySection(PersonAPI person, TooltipMakerAPI tooltip) {
		float opad = 10f;
		Color text = person.getFaction().getBaseUIColor();
		Color bg = person.getFaction().getDarkUIColor();
		CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(person.getAICoreId());

		tooltip.addSectionHeading("Personality: fearless", text, bg, Alignment.MID, 20);
		tooltip.addPara("In combat, the " + spec.getName() + " is single-minded and determined. " +
				"In a human captain, its traits might be considered reckless. In a machine, they're terrifying.", opad);
	}
}

