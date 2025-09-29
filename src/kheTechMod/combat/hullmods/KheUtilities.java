package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import kheTechMod.combat.plugins.KheTimeCleanup;
import org.apache.log4j.Logger;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.List;

import static com.fs.starfarer.api.impl.campaign.skills.FieldRepairsScript.restoreToNonDHull;

//import org.apache.log4j.Logger;

public class KheUtilities {
	private static final Logger log = Logger.getLogger(KheUtilities.class);
	// static final Logger log = Logger.getLogger(KheUtilities.class);

	public enum statCalcMode {MULTIPLY, PERCENT, FLAT}

	/*,boolean checkShipSystemIsPhase
	 * 				//||(checkShipSystemIsPhase&&((ship.getSystem()!=null) &&(ship.getSystem().)))
	 * *///cant as we can't (easily) check if a system is explicitly a phase system.
	public static boolean isPhaseShip(ShipAPI ship, boolean checkHullSpecIsPhase, boolean checkDefenseType, boolean checkPhaseFieldHullmod) {
		//a major problem here. ship.getHullSpec.isPhase() depends on the ship actually having the hint PHASE added to its entry in ship_data.csv
		//might just go with a more brute force option
		return
				(checkHullSpecIsPhase && (ship.getHullSpec().isPhase()))//this checks if the ship has the PHASE hint, basically. or using the vanilla phase cloak.
						|| (checkDefenseType && (ship.getHullSpec().getDefenseType() == ShieldAPI.ShieldType.PHASE))//generally more reliable?
						|| (checkPhaseFieldHullmod && ship.getVariant().hasHullMod("phasefield"))//check if the ship has the phase field hullmod which is usually on phase ships
				;
	}

	public static boolean isShielded(ShipAPI ship, boolean allowShunt, boolean considerBase) {
		return
				(!(allowShunt && shipHasHullmod(ship, "shield_shunt")))
						&& ((ship.getShield() != null)
						|| (considerBase && (ship.getHullSpec().getDefenseType() != ShieldAPI.ShieldType.NONE)))
				;
	}

	//concurrent modification is bad.
	public static void removeDMods(ShipVariantAPI shipVar) {
		List<String> buffer = new ArrayList<>(Collections.emptyList());
		for (String id : shipVar.getHullMods()) {
			HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
			if (spec.hasTag(Tags.HULLMOD_DMOD)) {
				buffer.add(id);
			}
		}
		for (String id : buffer) {
			DModManager.removeDMod(shipVar, id);
		}
		int dmods = DModManager.getNumDMods(shipVar);
		if (dmods <= 0) {
			restoreToNonDHull(shipVar);
		}
	}


	public static float getStack(HashMap<String, MutableStat.StatMod> someMods, statCalcMode calcMode, boolean excludeZero, List<String> excludeIDs) {
		float stack = 0f;
		if (calcMode == statCalcMode.MULTIPLY) {
			stack = 1f;
		}
		for (Map.Entry<String, MutableStat.StatMod> modEntry : someMods.entrySet()) {
			MutableStat.StatMod mod = modEntry.getValue();
			if ((excludeIDs != null) && (excludeIDs.contains(mod.source))) {
				continue;
			}
			float card = mod.getValue();
			if ((excludeZero || (calcMode != statCalcMode.MULTIPLY)) && (card == 0f)) {
				continue;
			}

			if (calcMode == statCalcMode.MULTIPLY) {
				stack *= card;
			} else {
				stack += card;
			}
		}
		if (calcMode == statCalcMode.PERCENT) {
			stack /= 100f;
		}
		return stack;
	}

	public static float processStack(MutableStat stat, boolean excludeZero, List<String> excludeIDs) {
		//because of THIS line, DO NOT MAKE A LIST VERSION.
		float base = stat.base;
		float stackFlat = getStack(stat.getFlatMods(), statCalcMode.FLAT, excludeZero, excludeIDs);
		float stackPercent = getStack(stat.getPercentMods(), statCalcMode.PERCENT, excludeZero, excludeIDs);
		float stackMult = getStack(stat.getMultMods(), statCalcMode.MULTIPLY, excludeZero, excludeIDs);
		return (((base * (1f + stackPercent)) + stackFlat) * stackMult);
	}

	public static float processStack(List<StatBonus> statBonuses, boolean excludeZero, List<String> excludeIDs, float base) {
		float stackFlat = 0f;
		float stackPercent = 0f;
		float stackMult = 1f;//you forgot to set it to 1. AGAIN.
		for (StatBonus sB : statBonuses) {
			float sFT = getStack(sB.getFlatBonuses(), statCalcMode.FLAT, excludeZero, excludeIDs);
			float sPT = getStack(sB.getPercentBonuses(), statCalcMode.PERCENT, excludeZero, excludeIDs);
			float sMT = getStack(sB.getMultBonuses(), statCalcMode.MULTIPLY, excludeZero, excludeIDs);
			if (sFT != 0f) {
				stackFlat += sFT;
			}
			if (sPT != 0f) {
				stackPercent += sPT;
			}
			if (sMT != 1f) {
				stackMult *= sMT;
			}
		}

		return ((base * (1f + stackPercent)) + stackFlat) * stackMult;
	}

	public static boolean stringArrayContains(String[] arrayToCheck, String stringToFind) {
		for (String stringToCheck : arrayToCheck) {
			if (Objects.equals(stringToCheck, stringToFind)) {
				return true;
			}
		}
		return false;
	}

	public static String lazyKheGetMultString(float input) {
		return lazyKheGetMultString(input, 2, false);
	}

	public static String lazyKheGetMultString(float input, int keepPlaces) {
		return lazyKheGetMultString(input, keepPlaces, false);
	}

	public static String lazyKheGetMultString(float input, int keepPlaces, boolean leftSide) {
		String buffer = lazyKheRounderS(input, keepPlaces);
		if (buffer.contains(".")) {
			while (buffer.endsWith("0")) {
				buffer = buffer.substring(0, buffer.length() - 1);
			}
		}
		if (buffer.endsWith(".")) {
			buffer = buffer.substring(0, buffer.length() - 1);
		}
		if (leftSide) {
			return "x" + buffer;
		}
		return buffer + "x";
	}

	public static String lazyKheGetPercentString(float input) {
		return lazyKheGetPercentString(input, 0);
	}

	public static String lazyKheGetPercentString(float input, int keepPlaces) {
		String buffer = lazyKheRounderS(input, keepPlaces);
		if (buffer.contains(".")) {
			while (buffer.endsWith("0")) {
				buffer = buffer.substring(0, buffer.length() - 1);
			}
		}
		if (buffer.endsWith(".")) {
			buffer = buffer.substring(0, buffer.length() - 1);
		}
		return buffer + "%";
	}

	public static String lazyKheRounderS(float input, int keepPlaces) {
		return (lazyKheRounder(input, keepPlaces)) + "";
	}

	public static float lazyKheRounder(float input, int keepPlaces) {
//        double tens = Math.pow(10f, keepPlaces);
//        return(float)(Math.round(input*tens)/tens);
//    }
// //https://stackoverflow.com/questions/8911356/whats-the-best-practice-to-round-a-float-to-2-decimals
//    public static float round(float d, int decimalPlace) {
		BigDecimal bd = new BigDecimal(Float.toString(input));
		bd = bd.setScale(keepPlaces, RoundingMode.HALF_UP);
		return bd.floatValue();
	}

	public static PersonAPI clonePersonForFighter(PersonAPI oldPerson) {
		PersonAPI newPerson = Global.getFactory().createPerson();
		newPerson.getStats().setLevel(oldPerson.getStats().getLevel());
		newPerson.setAICoreId(oldPerson.getAICoreId());
		newPerson.getName().setFirst(oldPerson.getName().getFirst());
		newPerson.getName().setLast(oldPerson.getName().getLast());
		newPerson.setGender(oldPerson.getGender());
		newPerson.setPortraitSprite(oldPerson.getPortraitSprite());
		List<MutableCharacterStatsAPI.SkillLevelAPI> skills = oldPerson.getStats().getSkillsCopy();
		for (MutableCharacterStatsAPI.SkillLevelAPI thing : skills) {
			newPerson.getStats().setSkillLevel(thing.getSkill().getId(), thing.getLevel());
		}
		newPerson.setPersonality(oldPerson.getPersonalityAPI().getId());
		return newPerson;
	}

	public static boolean isAutomated(ShipAPI ship) {
		return shipHasHullmod(ship, HullMods.AUTOMATED);
	}

	public static boolean personIsCore(PersonAPI person) {
		return (person != null) && (person.isAICore());
	}

	public static boolean hasFighterBays(ShipAPI ship) {
		return ((int) ship.getMutableStats().getNumFighterBays().getModifiedValue()) > 0;
	}

	public static float getNonBuiltInFighterOPCost(ShipAPI ship) {
		float currentOPBuffer = 0f;
		MutableShipStatsAPI shipStats = ship.getMutableStats();
		for (String fighterID : getNonBuiltInFighterWings(shipStats)) {
			currentOPBuffer += getFighterOPCost(shipStats, fighterID);
		}
		return currentOPBuffer;
	}

	public static float getHullModOPCost(ShipAPI ship) {
		float currentOPBuffer = 0f;
		ShipAPI.HullSize hullSize = ship.getHullSpec().getHullSize();
		for (String modId : ship.getVariant().getNonBuiltInHullmods()) {
			HullModSpecAPI mod = Global.getSettings().getHullModSpec(modId);
			currentOPBuffer += mod.getCostFor(hullSize);
		}
		return currentOPBuffer;
	}

	public static float getSumWeaponsCost(
			String myID, float costModifier,
			ShipAPI ship, List<WeaponAPI.WeaponType> weaponTypes, List<WeaponAPI.WeaponSize> weaponSizes,
			MutableCharacterStatsAPI captainStats,
			boolean isBeam, boolean isPD
	) {
		float currentOPBuffer = 0f;
		StatBonus dummyStatCopy = new StatBonus();
		dummyStatCopy.modifyFlat(myID, costModifier);
		List<StatBonus> dSCList = Collections.singletonList(dummyStatCopy);
		MutableShipStatsAPI shipStats = ship.getMutableStats();

		for (WeaponAPI weaponEntry : ship.getAllWeapons()) {
			if (weaponIsBuiltIn(weaponEntry)) {
				continue;
			}
			if (weaponMatches(weaponEntry, true, isBeam, isPD, weaponTypes, weaponSizes)) {
				currentOPBuffer += Math.max(0f, specialOPCostCalc(weaponEntry.getSpec(), captainStats, shipStats, null, dSCList, false));
			} else {
				currentOPBuffer += weaponEntry.getOriginalSpec().getOrdnancePointCost(captainStats, shipStats);
			}
		}
		return currentOPBuffer;
	}

	public static float getFighterOPCost(MutableShipStatsAPI stats, String wingId) {
		return Global.getSettings().getFighterWingSpec(wingId).getOpCost(stats);
	}

	public static float wouldAdditionPutOverLimit(
			String myID, ShipAPI ship,
			List<WeaponAPI.WeaponType> weaponTypes, List<WeaponAPI.WeaponSize> weaponSizes,
			boolean isBeam, boolean isPD, float costModifier
	) {
		PersonAPI captain = ship.getCaptain();
		if (captain == null) {
			captain = Global.getFactory().createPerson();
		}
		MutableCharacterStatsAPI captainStats = captain.getStats();

		float currentOPBuffer =
				ship.getVariant().getNumFluxVents() +
						ship.getVariant().getNumFluxCapacitors() +
						getNonBuiltInFighterOPCost(ship) +
						getHullModOPCost(ship) +
						getSumWeaponsCost(myID, costModifier, ship, weaponTypes, weaponSizes, captainStats, isBeam, isPD);

		float limit = ship.getHullSpec().getOrdnancePoints(captainStats);
		return currentOPBuffer - limit;
	}

	public static List<String> getFighterWings(MutableShipStatsAPI stats) {
		if (stats.getVariant() != null) {
			return stats.getVariant().getFittedWings();
		}
		return new ArrayList<>();
	}

	public static List<String> getNonBuiltInFighterWings(MutableShipStatsAPI stats) {
		if (stats.getVariant() != null) {
			return stats.getVariant().getNonBuiltInWings();
		}
		return new ArrayList<>();
	}

	public static boolean shipHasHullmod(ShipAPI ship, String id) {
		if (ship == null) {
			return false;
		}
		ShipVariantAPI var = ship.getVariant();
		if (var == null) {
			return false;
		}
		return var.hasHullMod(id);
	}

	public static boolean shipHasMatchingSlot(ShipAPI ship, List<WeaponAPI.WeaponType> validTypes, List<WeaponAPI.WeaponSize> validSizes) {
		if (ship == null) return false;
		for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
			if (slot.isDecorative()) continue;
			if (slot.isBuiltIn()) continue;
			if ((validTypes != null) && (!validTypes.contains(slot.getWeaponType()))) continue;
			if ((validSizes != null) && (!validSizes.contains(slot.getSlotSize()))) continue;
			return true;
		}
		return false;
	}

	public static StatBonus getOPCostStatBonus(MutableCharacterStatsAPI stats, WeaponAPI.WeaponSize size, WeaponAPI.WeaponType wType, boolean isBeam, boolean isPD) {
		return stats.getDynamic().getMod(selectCostModString(wType, size, isBeam, isPD));
	}

	public static StatBonus getOPCostStatBonus(MutableShipStatsAPI stats, WeaponAPI.WeaponSize size, WeaponAPI.WeaponType wType, boolean isBeam, boolean isPD) {
		return stats.getDynamic().getMod(selectCostModString(wType, size, isBeam, isPD));
	}

	public static String selectCostModString(WeaponAPI.WeaponType weapType, WeaponAPI.WeaponSize weapSize, boolean isBeam, boolean isPD) {
		if (isBeam) {
			if (weapSize == WeaponAPI.WeaponSize.LARGE) {
				return Stats.LARGE_BEAM_MOD;
			}
			if (weapSize == WeaponAPI.WeaponSize.MEDIUM) {
				return Stats.MEDIUM_BEAM_MOD;
			}
			if (weapSize == WeaponAPI.WeaponSize.SMALL) {
				return Stats.SMALL_BEAM_MOD;
			}
		} else if (isPD) {
			if (weapSize == WeaponAPI.WeaponSize.LARGE) {
				return Stats.LARGE_PD_MOD;
			}
			if (weapSize == WeaponAPI.WeaponSize.MEDIUM) {
				return Stats.MEDIUM_PD_MOD;
			}
			if (weapSize == WeaponAPI.WeaponSize.SMALL) {
				return Stats.SMALL_PD_MOD;
			}
		} else {
			if (weapType == WeaponAPI.WeaponType.BALLISTIC) {
				if (weapSize == WeaponAPI.WeaponSize.LARGE) {
					return Stats.LARGE_BALLISTIC_MOD;
				}
				if (weapSize == WeaponAPI.WeaponSize.MEDIUM) {
					return Stats.MEDIUM_BALLISTIC_MOD;
				}
				if (weapSize == WeaponAPI.WeaponSize.SMALL) {
					return Stats.SMALL_BALLISTIC_MOD;
				}
			}
			if (weapType == WeaponAPI.WeaponType.ENERGY) {
				if (weapSize == WeaponAPI.WeaponSize.LARGE) {
					return Stats.LARGE_ENERGY_MOD;
				}
				if (weapSize == WeaponAPI.WeaponSize.MEDIUM) {
					return Stats.MEDIUM_ENERGY_MOD;
				}
				if (weapSize == WeaponAPI.WeaponSize.SMALL) {
					return Stats.SMALL_ENERGY_MOD;
				}
			}
			if (weapType == WeaponAPI.WeaponType.MISSILE) {
				if (weapSize == WeaponAPI.WeaponSize.LARGE) {
					return Stats.LARGE_MISSILE_MOD;
				}
				if (weapSize == WeaponAPI.WeaponSize.MEDIUM) {
					return Stats.MEDIUM_MISSILE_MOD;
				}
				if (weapSize == WeaponAPI.WeaponSize.SMALL) {
					return Stats.SMALL_MISSILE_MOD;
				}
			}
		}
		return null;
	}

	@SuppressWarnings("unused")//unused because it's an overload and I want options.
	public static MutableStat getFluxCostStatBonus(MutableShipStatsAPI stats, boolean beamMode) {//beamMode boolean purely for overload purposes...
		return stats.getBeamWeaponFluxCostMult();
	}

	public static StatBonus getFluxCostStatBonus(MutableShipStatsAPI stats, WeaponAPI.WeaponType weaponType) {
		if (weaponType == WeaponAPI.WeaponType.BALLISTIC) {
			return stats.getBallisticWeaponFluxCostMod();
		}
		if (weaponType == WeaponAPI.WeaponType.ENERGY) {
			return stats.getEnergyWeaponFluxCostMod();
		}
		if (weaponType == WeaponAPI.WeaponType.MISSILE) {
			return stats.getMissileWeaponFluxCostMod();
		}
		return null;
	}

	public final static List<WeaponAPI.WeaponType> ALLCOSTMODWEAPONTYPES = Arrays.asList(WeaponAPI.WeaponType.BALLISTIC, WeaponAPI.WeaponType.ENERGY, WeaponAPI.WeaponType.MISSILE);
	public final static List<WeaponAPI.WeaponSize> ALLSIZES = Arrays.asList(WeaponAPI.WeaponSize.LARGE, WeaponAPI.WeaponSize.MEDIUM, WeaponAPI.WeaponSize.SMALL);

	public final static List<WeaponAPI.WeaponType> BALLISTICMOUNTTYPES = Arrays.asList(
			WeaponAPI.WeaponType.BALLISTIC,
			WeaponAPI.WeaponType.HYBRID,
			WeaponAPI.WeaponType.COMPOSITE,
			WeaponAPI.WeaponType.UNIVERSAL
	);
	public final static List<WeaponAPI.WeaponType> ENERGYMOUNTTYPES = Arrays.asList(
			WeaponAPI.WeaponType.ENERGY,
			WeaponAPI.WeaponType.HYBRID,
			WeaponAPI.WeaponType.SYNERGY,
			WeaponAPI.WeaponType.UNIVERSAL
	);
	public final static List<WeaponAPI.WeaponType> MISSILEMOUNTTYPES = Arrays.asList(
			WeaponAPI.WeaponType.MISSILE,
			WeaponAPI.WeaponType.SYNERGY,
			WeaponAPI.WeaponType.COMPOSITE,
			WeaponAPI.WeaponType.UNIVERSAL
	);
	public final static List<WeaponAPI.WeaponType> ALLMOUNTTYPES = Arrays.asList(
			WeaponAPI.WeaponType.BALLISTIC,
			WeaponAPI.WeaponType.ENERGY,
			WeaponAPI.WeaponType.MISSILE,
			WeaponAPI.WeaponType.HYBRID,
			WeaponAPI.WeaponType.SYNERGY,
			WeaponAPI.WeaponType.COMPOSITE,
			WeaponAPI.WeaponType.UNIVERSAL
	);

	public static List<WeaponAPI.WeaponType> selectMountList(WeaponAPI.WeaponType weapType, boolean isBeam) {
		if (!isBeam) {
			if (weapType == WeaponAPI.WeaponType.MISSILE) {
				return MISSILEMOUNTTYPES;
			}
			if (weapType == WeaponAPI.WeaponType.ENERGY) {
				return ENERGYMOUNTTYPES;
			}
			if (weapType == WeaponAPI.WeaponType.BALLISTIC) {
				return BALLISTICMOUNTTYPES;
			}
		} else {
			return ALLMOUNTTYPES;
		}
		return null;
	}

	public static List<StatBonus> getAllCostStatBonusesPart(
			WeaponAPI.WeaponSize weapSize, WeaponAPI.WeaponType weapType, MutableCharacterStatsAPI captainStats, boolean isPD, boolean isBeam
	) {
		List<StatBonus> statBonuses = new ArrayList<>(Collections.emptyList());
		statBonuses.add(getOPCostStatBonus(captainStats, weapSize, weapType, false, false));
		if (isBeam) {
			statBonuses.add(getOPCostStatBonus(captainStats, weapSize, weapType, true, false));
		}
		if (isPD) {
			statBonuses.add(getOPCostStatBonus(captainStats, weapSize, weapType, false, true));
		}
		return statBonuses;
	}

	public static List<StatBonus> getAllCostStatBonusesPart(
			WeaponAPI.WeaponSize weapSize, WeaponAPI.WeaponType weapType, MutableShipStatsAPI shipStats, boolean isPD, boolean isBeam
	) {
		List<StatBonus> statBonuses = new ArrayList<>(Collections.emptyList());
		statBonuses.add(getOPCostStatBonus(shipStats, weapSize, weapType, false, false));
		if (isBeam) {
			statBonuses.add(getOPCostStatBonus(shipStats, weapSize, weapType, true, false));
		}
		if (isPD) {
			statBonuses.add(getOPCostStatBonus(shipStats, weapSize, weapType, false, true));
		}
		return statBonuses;
	}

	public static List<StatBonus> getAllCostStatBonuses(WeaponSpecAPI weapSpec, MutableCharacterStatsAPI captainStats, MutableShipStatsAPI shipStats, List<StatBonus> toAppend) {
		List<StatBonus> statBonuses = new ArrayList<>(Collections.emptyList());
		WeaponAPI.WeaponSize weapSize = weapSpec.getSize();
		WeaponAPI.WeaponType weapType = weapSpec.getType();
		boolean isBeam = weapSpec.isBeam();
		boolean isPD = weapSpec.getAIHints().contains(WeaponAPI.AIHints.PD);

		statBonuses.addAll(getAllCostStatBonusesPart(weapSize, weapType, captainStats, isPD, isBeam));
		statBonuses.addAll(getAllCostStatBonusesPart(weapSize, weapType, shipStats, isPD, isBeam));

		if (toAppend != null) {
			statBonuses.addAll(toAppend);
		}

		return statBonuses;
	}

	public static void applyCostModifiers(String id, float COSTREDUCTION, MutableShipStatsAPI stats, List<WeaponAPI.WeaponSize> validsizes, List<WeaponAPI.WeaponType> weapontypes, boolean isBeam) {
		for (WeaponAPI.WeaponType weapType : weapontypes) {
			for (WeaponAPI.WeaponSize weapSize : validsizes) {
				getOPCostStatBonus(stats, weapSize, weapType, isBeam, false).modifyFlat(id, COSTREDUCTION);
			}
		}
	}

	public static boolean weaponIsBuiltIn(WeaponAPI weaponEntry) {
		return (weaponEntry.getSlot().isBuiltIn() || weaponEntry.getSlot().isHidden());
	}

	@SuppressWarnings("unused")
	public static boolean weaponMatches(WeaponAPI weaponEntry, boolean noBuiltIns, boolean isBeam, boolean isPD, WeaponAPI.WeaponType weaponType, WeaponAPI.WeaponSize weaponSize) {
		return weaponMatches(weaponEntry, noBuiltIns, isBeam, isPD, Collections.singletonList(weaponType), Collections.singletonList(weaponSize));
	}

	@SuppressWarnings("unused")
	public static boolean weaponMatches(WeaponAPI weaponEntry, boolean noBuiltIns, boolean isBeam, boolean isPD, List<WeaponAPI.WeaponType> weaponTypes, WeaponAPI.WeaponSize weaponSize) {
		return weaponMatches(weaponEntry, noBuiltIns, isBeam, isPD, weaponTypes, Collections.singletonList(weaponSize));
	}

	@SuppressWarnings("unused")
	public static boolean weaponMatches(WeaponAPI weaponEntry, boolean noBuiltIns, boolean isBeam, boolean isPD, WeaponAPI.WeaponType weaponType, List<WeaponAPI.WeaponSize> weaponSizes) {
		return weaponMatches(weaponEntry, noBuiltIns, isBeam, isPD, Collections.singletonList(weaponType), weaponSizes);
	}

	@SuppressWarnings("unused")
	public static boolean weaponMatches(WeaponAPI weaponEntry, boolean noBuiltIns, boolean isBeam, boolean isPD, List<WeaponAPI.WeaponType> weaponTypes, List<WeaponAPI.WeaponSize> weaponSizes) {
		if (weaponEntry.isDecorative() || (weaponEntry.getType() == null)) {
			return false;
		}
		if (noBuiltIns && weaponIsBuiltIn(weaponEntry)) {
			return false;
		}
		if (isBeam && weaponEntry.isBeam()) {
			return true;
		}
		if (isPD) {
			WeaponSpecAPI spec = weaponEntry.getSpec();
			if (spec != null) {
				if (spec.getAIHints().contains(WeaponAPI.AIHints.PD)) {
					return true;
				}
			}
		}
		return (weaponTypes.contains(weaponEntry.getType()) && weaponSizes.contains(weaponEntry.getSize()));
	}

	public static float specialOPCostCalc(
			WeaponSpecAPI weapSpec, MutableCharacterStatsAPI captainStats, MutableShipStatsAPI shipStats, List<String> excludeIDs, List<StatBonus> statBonusAppend,
			boolean returnNegative
	) {
		//can copy statbonus. cant copy mutable*statsapi (easily). and since they pass by reference...i'm stuck with this implementation.
		//if it werent for that, I could just copy, unmodify, and run getOrdnancePointCost.
		float baseOP = weapSpec.getOrdnancePointCost(null);
		float procNum = processStack(getAllCostStatBonuses(weapSpec, captainStats, shipStats, statBonusAppend), false, excludeIDs, baseOP);
		if (!returnNegative) {
			procNum = Math.max(0, procNum);
		}
		return procNum;
	}

	public static float clamp(float min, float max, float value) {
		return (Math.max(min, Math.min(max, value)));
	}

	public static float lerp(float start, float stop, float progress) {
		return start + (progress * (stop - start));
	}

	public static float inverseLerp(float left, float right, float value) {
		return (value - left) / (right - left);
	}

	public final static String RATSEXOSHIPNOREMOVALSTRING = "Cannot be removed at an exoship.";

	public static boolean isThisRatsExoship(MarketAPI marketOrNull) {
		return ((marketOrNull != null) && (marketOrNull.getId().startsWith("exoship_") || marketOrNull.getId().startsWith("exoship_broken_")));
	}

	public static String slotSizeListString(List<WeaponAPI.WeaponSize> sizes) {
		StringJoiner sj = new StringJoiner(", ");
		for (WeaponAPI.WeaponSize s : sizes) {
			sj.add(s.getDisplayName());
		}
		return sj.toString();
	}

	public static String slotTypeListString(List<WeaponAPI.WeaponType> types) {
		StringJoiner sj = new StringJoiner(", ");
		for (WeaponAPI.WeaponType s : types) {
			sj.add(s.getDisplayName());
		}
		return sj.toString();
	}

	public static boolean handleInvuln(String id, boolean on, MutableShipStatsAPI stats) {
		if (on) {
			stats.getEmpDamageTakenMult().modifyMult(id, 0f);
			stats.getArmorDamageTakenMult().modifyMult(id, 0f);
			stats.getHullDamageTakenMult().modifyMult(id, 0f);
			stats.getEngineDamageTakenMult().modifyMult(id, 0f);
		} else {
			stats.getEmpDamageTakenMult().unmodify(id);
			stats.getArmorDamageTakenMult().unmodify(id);
			stats.getHullDamageTakenMult().unmodify(id);
			stats.getEngineDamageTakenMult().unmodify(id);
		}
		return on;
	}

	public static void handleTimeFlow(boolean on, String effectID, ShipAPI ship, float timeMultNow) {
		handleTimeFlow(on, effectID, Global.getCombatEngine().getPlayerShip() == ship, ship, timeMultNow);
	}

	public static void handleTimeFlow(boolean on, String effectID, boolean isPlayer, ShipAPI ship, float timeMultNow) {
		if (on) {
			if (isPlayer) {
				Global.getCombatEngine().getTimeMult().modifyMult(effectID, 1f / timeMultNow);
				KheTimeCleanup.registerTimeEffect(effectID);
			}
			ship.getMutableStats().getTimeMult().modifyMult(effectID, timeMultNow);
		} else {
			Global.getCombatEngine().getTimeMult().unmodify(effectID);
			ship.getMutableStats().getTimeMult().unmodify(effectID);
		}
	}

	public static Color iArrayToColor(int[] color) {
		return new Color(color[0], color[1], color[2], color[3]);
	}

	//because returning a Color just adds more damn work, as you are forced to use getRed et al with that.
	public static int[] getAverageEngineColor(ShipAPI ship) {
		List<ShipEngineControllerAPI.ShipEngineAPI> engineList = ship.getEngineController().getShipEngines();
		int engineCount = engineList.size();
		if (engineCount == 0) {
			return new int[]{0, 0, 0, 0};
		}
		int[] reds = new int[engineCount];
		int[] greens = new int[engineCount];
		int[] blues = new int[engineCount];
		int[] alphas = new int[engineCount];
		int counter = 0;
		while (counter < engineCount) {
			Color engineColor = engineList.get(counter).getEngineColor();
			reds[counter] = engineColor.getRed();
			greens[counter] = engineColor.getGreen();
			blues[counter] = engineColor.getBlue();
			alphas[counter] = engineColor.getAlpha();
			//log.info("engine "+counter+":"+reds[counter]+","+blues[counter]+","+greens[counter]+","+alphas[counter]);
			counter++;
		}

		int red = 0;
		for (int r : reds) {
			red += r;
		}
		red /= engineCount;

		int green = 0;
		for (int r : greens) {
			green += r;
		}
		green /= engineCount;

		int blue = 0;
		for (int r : blues) {
			blue += r;
		}
		blue /= engineCount;

		int alpha = 0;
		for (int r : alphas) {
			alpha += r;
		}
		alpha /= engineCount;

		return new int[]{red, green, blue, alpha};
	}

	//https://www.wolframalpha.com/input?i=y%3D%281%2B%281%2Fsqrt%28abs%28x%29%29%29%29%2Cy%3D%281%2B%281%2Fsqrt%28abs%28x%29%29%29%29%5Eabs%28x%29
	//where input=2,scalar=x
	//f(1)=2,f(1)^1=2;f(2)=1.8ish, f(2)^2=3ish,f(3)=1.6ish,f(3)^3=4ish
	public static float multiModScalarHeadache(float input, int scalar) {
		if (scalar == 0) {
			return 0;
		}
		return 1 + ((input - 1) * (1 / (float) Math.sqrt(scalar)));
	}
}
