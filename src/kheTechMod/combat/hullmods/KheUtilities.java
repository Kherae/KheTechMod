package kheTechMod.combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
//import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import java.util.*;

import static com.fs.starfarer.api.impl.campaign.skills.FieldRepairsScript.restoreToNonDHull;

//import org.apache.log4j.Logger;

public class KheUtilities {
//    private static final Logger log = Logger.getLogger(KheUtilities.class);

    public enum statCalcMode{MULTIPLY,PERCENT,FLAT}

    public static boolean isPhaseShip(ShipAPI ship,boolean allowFuzz) {
        return
            (ship.getHullSpec().isPhase()) ||
            (allowFuzz&&(ship.getHullSpec().getDefenseType() == ShieldAPI.ShieldType.PHASE))
        ;
    }
    
    public static boolean isShielded(ShipAPI ship,boolean allowShunt,boolean considerBase){
        return
            (!(allowShunt && shipHasHullmod(ship,"shield_shunt"))) &&
            ((ship.getShield() != null) ||
            (considerBase && (ship.getHullSpec().getDefenseType() != ShieldAPI.ShieldType.NONE)))
        ;
    }

    //concurrent modification is bad.
    public static void removeDMods(ShipVariantAPI shipVar) {
        List<String> buffer = new ArrayList<>(Collections.emptyList());
        for (String id : shipVar.getHullMods()) {
            HullModSpecAPI spec =  Global.getSettings().getHullModSpec(id);
            if(spec.hasTag(Tags.HULLMOD_DMOD)){
                buffer.add(id);
            }
        }
        for (String id:buffer){
            DModManager.removeDMod(shipVar, id);
        }
        int dmods = DModManager.getNumDMods(shipVar);
        if (dmods <= 0) {
            restoreToNonDHull(shipVar);
        }
    }

    //copied from retrofitted bridge. for the tesseracts...they are not allowed to be nonautomated.
    public static void addMod(ShipVariantAPI variant, String mod) {
        variant.removeSuppressedMod(mod);
        if (variant.getHullSpec().isBuiltInMod(mod)) {
            variant.addMod(mod);
        } else {
            variant.addPermaMod(mod);
        }
    }
    public static void removeMod(ShipVariantAPI variant, String mod) {
        if (variant.getHullSpec().isBuiltInMod(mod)) {
            variant.addSuppressedMod(mod);
        }
        variant.removePermaMod(mod);
    }

    public static float getStack(HashMap<String, MutableStat.StatMod> someMods, statCalcMode calcMode, boolean excludeZero, List<String> excludeIDs){
        float stack=0f;
        if (calcMode==statCalcMode.MULTIPLY){stack=1f;}
        for (Map.Entry<String, MutableStat.StatMod> modEntry : someMods.entrySet()) {
            MutableStat.StatMod mod = modEntry.getValue();
            if((excludeIDs!=null)&&(excludeIDs.contains(mod.source))){continue;}

            float card=mod.getValue();
            if((excludeZero||(calcMode!=statCalcMode.MULTIPLY))&&(card==0f)){continue;}

            if(calcMode==statCalcMode.MULTIPLY){
                stack*=card;
            }
            else{
                stack+=card;
            }
        }
        if(calcMode==statCalcMode.PERCENT){stack/=100f;}
        return stack;
    }


    public static float processStack(MutableStat stat, boolean excludeZero, List<String> excludeIDs){
        //because of THIS line, DO NOT MAKE A LIST VERSION.
        float base=stat.base;
        float stackFlat=getStack(stat.getFlatMods(),statCalcMode.FLAT, excludeZero, excludeIDs);
        float stackPercent=getStack(stat.getPercentMods(),statCalcMode.PERCENT, excludeZero, excludeIDs);
        float stackMult=getStack(stat.getMultMods(),statCalcMode.MULTIPLY, excludeZero, excludeIDs);
        return(((base*(1f+stackPercent))+stackFlat)*stackMult);
    }
    public static float processStack(List<StatBonus> statBonuses, boolean excludeZero, List<String> excludeIDs, float base) {
        float stackFlat=0f;
        float stackPercent=0f;
        float stackMult=1f;//you forgot to set it to 1. AGAIN.
        for(StatBonus sB:statBonuses){
            float sFT=getStack(sB.getFlatBonuses(),statCalcMode.FLAT, excludeZero, excludeIDs);
            float sPT=getStack(sB.getPercentBonuses(),statCalcMode.PERCENT, excludeZero, excludeIDs);
            float sMT=getStack(sB.getMultBonuses(),statCalcMode.MULTIPLY, excludeZero, excludeIDs);
            if(sFT!=0f){stackFlat+=sFT;}
            if(sPT!=0f){stackPercent+=sPT;}
            if(sMT!=1f){stackMult*=sMT;}
        }
        return ((base * (1f + stackPercent)) + stackFlat) * stackMult;
    }

    public static String lazyKheGetMultString(float input){return lazyKheGetMultString(input,0f);}
    public static String lazyKheGetMultString(float input,float places){
        return (Math.round(input*100f*Math.pow(10f,places))/(100f*Math.pow(10f,places)))+"x";
    }

    public static PersonAPI clonePersonForFighter(PersonAPI oldPerson){
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

    public static boolean isAutomated(ShipAPI ship){
        return shipHasHullmod(ship,HullMods.AUTOMATED);
    }

    public static boolean personIsCore(PersonAPI person){return (person!=null)&&(person.isAICore());}

    public static boolean hasFighterBays(ShipAPI ship){
        return ((int) ship.getMutableStats().getNumFighterBays().getModifiedValue()) > 0;
    }

    public static float getAllFighterOPCost(ShipAPI ship){
        float currentOPBuffer=0f;
        MutableShipStatsAPI shipStats = ship.getMutableStats();
        for (String fighterID:getFighterWings(shipStats)){
            currentOPBuffer+=getFighterOPCost(shipStats,fighterID);
        }
        return currentOPBuffer;
    }

    public static float getHullModOPCost(ShipAPI ship){
        float currentOPBuffer=0f;
        ShipAPI.HullSize hullSize = ship.getHullSpec().getHullSize();
        for (String modId:ship.getVariant().getNonBuiltInHullmods()){
            HullModSpecAPI mod = Global.getSettings().getHullModSpec(modId);
            currentOPBuffer+=mod.getCostFor(hullSize);
        }
        return currentOPBuffer;
    }

    public static float getSumWeaponsCost(
        String myID,float costModifier,
        ShipAPI ship,List<WeaponAPI.WeaponType>weaponTypes, List<WeaponAPI.WeaponSize> weaponSizes,
        MutableCharacterStatsAPI captainStats,
        boolean isBeam,boolean isPD
    ){
        float currentOPBuffer=0f;
        StatBonus dummyStatCopy = new StatBonus();
        dummyStatCopy.modifyFlat(myID,costModifier);
        List<StatBonus>dSCList=Collections.singletonList(dummyStatCopy);
        MutableShipStatsAPI shipStats = ship.getMutableStats();

        for (WeaponAPI weaponEntry : ship.getAllWeapons()) {
            if(!weaponMatches(weaponEntry,true,isBeam,isPD,weaponTypes,weaponSizes)){continue;}
            float projectedOP=specialOPCostCalc(
                weaponEntry.getSpec(),captainStats,shipStats,
                Collections.singletonList(myID),dSCList,
                false
            );
            float currentOP=weaponEntry.getOriginalSpec().getOrdnancePointCost(captainStats, shipStats);

            if (weaponTypes.contains(weaponEntry.getType())) {
                currentOPBuffer+=Math.max(0f,projectedOP);
            }
            else{
                currentOPBuffer+=currentOP;
            }
        }
        return currentOPBuffer;
    }

    public static float getFighterOPCost(MutableShipStatsAPI stats,String wingId) {
        return Global.getSettings().getFighterWingSpec(wingId).getOpCost(stats);
    }

    public static boolean wouldAdditionPutOverLimit(
        String myID, ShipAPI ship,
        List<WeaponAPI.WeaponType> weaponTypes, List<WeaponAPI.WeaponSize> weaponSizes,
        boolean isBeam,boolean isPD,float costModifier
    ){
        PersonAPI captain=ship.getCaptain();
        if (captain==null){captain=Global.getFactory().createPerson();}
        MutableCharacterStatsAPI captainStats = captain.getStats();

        float currentOPBuffer=
            ship.getVariant().getNumFluxVents()+
            ship.getVariant().getNumFluxCapacitors()+
            getAllFighterOPCost(ship)+
            getHullModOPCost(ship)+
            getSumWeaponsCost(myID,costModifier,ship,weaponTypes,weaponSizes,captainStats,isBeam,isPD)
        ;

        float limit=ship.getHullSpec().getOrdnancePoints(captainStats);

        return currentOPBuffer > limit;
    }

    public static List<String> getFighterWings(MutableShipStatsAPI stats) {
        if (stats.getVariant() != null) {
            return stats.getVariant().getFittedWings();
        }
        return new ArrayList<>();
    }

    public static boolean shipHasHullmod(ShipAPI ship,String id){
        if(ship==null){return false;}
        ShipVariantAPI var=ship.getVariant();
        if(var==null){return false;}
        return var.hasHullMod(id);
    }

    public static boolean shipHasMatchingSlot(ShipAPI ship, List <WeaponAPI.WeaponType> validTypes, List<WeaponAPI.WeaponSize> validSizes) {
        if (ship == null) return false;
        for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
            if (slot.isDecorative()) continue;
            if (slot.isBuiltIn()) continue;
            if ((validTypes!=null) && (!validTypes.contains(slot.getWeaponType()))) continue;
            if ((validSizes!=null) && (!validSizes.contains(slot.getSlotSize()))) continue;
            return true;
        }
        return false;
    }

    public static StatBonus getOPCostStatBonus(MutableCharacterStatsAPI stats, WeaponAPI.WeaponSize size, WeaponAPI.WeaponType wType,boolean isBeam,boolean isPD){
        return stats.getDynamic().getMod(selectCostModString(wType,size,isBeam,isPD));
    }
    public static StatBonus getOPCostStatBonus(MutableShipStatsAPI stats, WeaponAPI.WeaponSize size, WeaponAPI.WeaponType wType,boolean isBeam,boolean isPD){
        return stats.getDynamic().getMod(selectCostModString(wType,size,isBeam,isPD));
    }

    public static String selectCostModString(WeaponAPI.WeaponType weapType, WeaponAPI.WeaponSize weapSize,boolean isBeam,boolean isPD){
        if(isBeam){
            if(weapSize==WeaponAPI.WeaponSize.LARGE){return Stats.LARGE_BEAM_MOD;}
            if(weapSize==WeaponAPI.WeaponSize.MEDIUM){return Stats.MEDIUM_BEAM_MOD;}
            if(weapSize==WeaponAPI.WeaponSize.SMALL){return Stats.SMALL_BEAM_MOD;}
        }
        else if(isPD){
            if(weapSize==WeaponAPI.WeaponSize.LARGE){return Stats.LARGE_PD_MOD;}
            if(weapSize==WeaponAPI.WeaponSize.MEDIUM){return Stats.MEDIUM_PD_MOD;}
            if(weapSize==WeaponAPI.WeaponSize.SMALL){return Stats.SMALL_PD_MOD;}
        }
        else{
            if(weapType==WeaponAPI.WeaponType.BALLISTIC){
                if(weapSize==WeaponAPI.WeaponSize.LARGE){return Stats.LARGE_BALLISTIC_MOD;}
                if(weapSize==WeaponAPI.WeaponSize.MEDIUM){return Stats.MEDIUM_BALLISTIC_MOD;}
                if(weapSize==WeaponAPI.WeaponSize.SMALL){return Stats.SMALL_BALLISTIC_MOD;}
            }
            if(weapType==WeaponAPI.WeaponType.ENERGY){
                if(weapSize==WeaponAPI.WeaponSize.LARGE){return Stats.LARGE_ENERGY_MOD;}
                if(weapSize==WeaponAPI.WeaponSize.MEDIUM){return Stats.MEDIUM_ENERGY_MOD;}
                if(weapSize==WeaponAPI.WeaponSize.SMALL){return Stats.SMALL_ENERGY_MOD;}
            }
            if(weapType==WeaponAPI.WeaponType.MISSILE){
                if(weapSize==WeaponAPI.WeaponSize.LARGE){return Stats.LARGE_MISSILE_MOD;}
                if(weapSize==WeaponAPI.WeaponSize.MEDIUM){return Stats.MEDIUM_MISSILE_MOD;}
                if(weapSize==WeaponAPI.WeaponSize.SMALL){return Stats.SMALL_MISSILE_MOD;}
            }
        }
        return null;
    }

    @SuppressWarnings("unused")//unused because it's an overload hack.
    public static MutableStat getFluxCostStatBonus(MutableShipStatsAPI stats, boolean beamMode){//beamMode boolean purely for overload purposes...
        return stats.getBeamWeaponFluxCostMult();
    }
    public static StatBonus getFluxCostStatBonus(MutableShipStatsAPI stats, WeaponAPI.WeaponType weaponType){
        if(weaponType==WeaponAPI.WeaponType.BALLISTIC){
            return stats.getBallisticWeaponFluxCostMod();
        }
        if(weaponType==WeaponAPI.WeaponType.ENERGY){
            return stats.getEnergyWeaponFluxCostMod();
        }
        if(weaponType==WeaponAPI.WeaponType.MISSILE){
            return stats.getMissileWeaponFluxCostMod();
        }
        return null;
    }

    public final static List <WeaponAPI.WeaponType> ALLCOSTMODWEAPONTYPES=Arrays.asList(WeaponAPI.WeaponType.BALLISTIC, WeaponAPI.WeaponType.ENERGY, WeaponAPI.WeaponType.MISSILE);
    public final static List <WeaponAPI.WeaponSize> ALLSIZES=Arrays.asList(WeaponAPI.WeaponSize.LARGE, WeaponAPI.WeaponSize.MEDIUM, WeaponAPI.WeaponSize.SMALL);

    public final static List <WeaponAPI.WeaponType> BALLISTICMOUNTTYPES = Arrays.asList(
        WeaponAPI.WeaponType.BALLISTIC,
        WeaponAPI.WeaponType.HYBRID,
        WeaponAPI.WeaponType.COMPOSITE,
        WeaponAPI.WeaponType.UNIVERSAL
    );
    public final static List <WeaponAPI.WeaponType> ENERGYMOUNTTYPES = Arrays.asList(
        WeaponAPI.WeaponType.ENERGY,
        WeaponAPI.WeaponType.HYBRID,
        WeaponAPI.WeaponType.SYNERGY,
        WeaponAPI.WeaponType.UNIVERSAL
    );
    public final static List <WeaponAPI.WeaponType> MISSILEMOUNTTYPES = Arrays.asList(
        WeaponAPI.WeaponType.MISSILE,
        WeaponAPI.WeaponType.SYNERGY,
        WeaponAPI.WeaponType.COMPOSITE,
        WeaponAPI.WeaponType.UNIVERSAL
    );
    public final static List <WeaponAPI.WeaponType> ALLMOUNTTYPES = Arrays.asList(
        WeaponAPI.WeaponType.BALLISTIC,
        WeaponAPI.WeaponType.ENERGY,
        WeaponAPI.WeaponType.MISSILE,
        WeaponAPI.WeaponType.HYBRID,
        WeaponAPI.WeaponType.SYNERGY,
        WeaponAPI.WeaponType.COMPOSITE,
        WeaponAPI.WeaponType.UNIVERSAL
    );

    public static List <WeaponAPI.WeaponType> selectMountList(WeaponAPI.WeaponType weapType,boolean isBeam) {
        if(!isBeam) {
            if (weapType == WeaponAPI.WeaponType.MISSILE) {
                return MISSILEMOUNTTYPES;
            }
            if (weapType == WeaponAPI.WeaponType.ENERGY) {
                return ENERGYMOUNTTYPES;
            }
            if (weapType == WeaponAPI.WeaponType.BALLISTIC) {
                return BALLISTICMOUNTTYPES;
            }
        }
        else{
            return ALLMOUNTTYPES;
        }
        return null;
    }

    public static List <StatBonus> getAllCostStatBonusesPart(
        WeaponAPI.WeaponSize weapSize,WeaponAPI.WeaponType weapType, MutableCharacterStatsAPI captainStats,boolean isPD,boolean isBeam
    ){
        List <StatBonus> statBonuses = new ArrayList<>(Collections.emptyList());
        statBonuses.add(getOPCostStatBonus(captainStats,weapSize,weapType,false,false));
        if(isBeam){
            statBonuses.add(getOPCostStatBonus(captainStats,weapSize,weapType,true,false));
        }
        if(isPD) {
            statBonuses.add(getOPCostStatBonus(captainStats,weapSize,weapType,false,true));
        }
        return statBonuses;
    }
    public static List <StatBonus> getAllCostStatBonusesPart(
        WeaponAPI.WeaponSize weapSize,WeaponAPI.WeaponType weapType, MutableShipStatsAPI shipStats,boolean isPD,boolean isBeam
    ){
        List <StatBonus> statBonuses = new ArrayList<>(Collections.emptyList());
        statBonuses.add(getOPCostStatBonus(shipStats,weapSize,weapType,false,false));
        if(isBeam){
            statBonuses.add(getOPCostStatBonus(shipStats,weapSize,weapType,true,false));
        }
        if(isPD) {
            statBonuses.add(getOPCostStatBonus(shipStats,weapSize,weapType,false,true));
        }
        return statBonuses;
    }

    public static List <StatBonus> getAllCostStatBonuses(WeaponSpecAPI weapSpec, MutableCharacterStatsAPI captainStats, MutableShipStatsAPI shipStats,List<StatBonus>toAppend){
        List <StatBonus> statBonuses = new ArrayList<>(Collections.emptyList());
        WeaponAPI.WeaponSize weapSize=weapSpec.getSize();
        WeaponAPI.WeaponType weapType=weapSpec.getType();
        boolean isBeam=weapSpec.isBeam();
        boolean isPD=weapSpec.getAIHints().contains(WeaponAPI.AIHints.PD);

        statBonuses.addAll(getAllCostStatBonusesPart(weapSize,weapType,captainStats,isPD,isBeam));
        statBonuses.addAll(getAllCostStatBonusesPart(weapSize,weapType,shipStats,isPD,isBeam));

        if(toAppend!=null){statBonuses.addAll(toAppend);}

        return statBonuses;
    }

    public static void applyCostModifiers(String id, float COSTREDUCTION, MutableShipStatsAPI stats, List<WeaponAPI.WeaponSize> validsizes, List<WeaponAPI.WeaponType> weapontypes,boolean isBeam){
        for (WeaponAPI.WeaponType weapType:weapontypes){
            for (WeaponAPI.WeaponSize weapSize:validsizes){
                getOPCostStatBonus(stats,weapSize,weapType,isBeam,false).modifyFlat(id,COSTREDUCTION);
            }
        }
    }

    public static boolean weaponIsBuiltIn(WeaponAPI weaponEntry){
        return (weaponEntry.getSlot().isBuiltIn()||weaponEntry.getSlot().isHidden());
    }
    @SuppressWarnings("unused")
    public static boolean weaponMatches(WeaponAPI weaponEntry, boolean noBuiltIns, boolean isBeam, boolean isPD, WeaponAPI.WeaponType weaponType, WeaponAPI.WeaponSize weaponSize){
        return weaponMatches(weaponEntry,noBuiltIns,isBeam,isPD,Collections.singletonList(weaponType),Collections.singletonList(weaponSize));
    }
    @SuppressWarnings("unused")
    public static boolean weaponMatches(WeaponAPI weaponEntry, boolean noBuiltIns, boolean isBeam, boolean isPD, List<WeaponAPI.WeaponType> weaponTypes, WeaponAPI.WeaponSize weaponSize){
        return weaponMatches(weaponEntry,noBuiltIns,isBeam,isPD,weaponTypes,Collections.singletonList(weaponSize));
    }
    @SuppressWarnings("unused")
    public static boolean weaponMatches(WeaponAPI weaponEntry, boolean noBuiltIns, boolean isBeam, boolean isPD, WeaponAPI.WeaponType weaponType, List<WeaponAPI.WeaponSize> weaponSizes){
        return weaponMatches(weaponEntry,noBuiltIns,isBeam,isPD,Collections.singletonList(weaponType),weaponSizes);
    }
    @SuppressWarnings("unused")
    public static boolean weaponMatches(WeaponAPI weaponEntry, boolean noBuiltIns, boolean isBeam, boolean isPD, List<WeaponAPI.WeaponType> weaponTypes, List<WeaponAPI.WeaponSize> weaponSizes){
        if (weaponEntry.isDecorative()||(weaponEntry.getType()==null)){return false;}
        if (noBuiltIns && weaponIsBuiltIn(weaponEntry)) {return false;}
        if(isBeam&&weaponEntry.isBeam()){return true;}
        if(isPD){
            WeaponSpecAPI spec = weaponEntry.getSpec();
            if(spec!=null){
                if(spec.getAIHints().contains(WeaponAPI.AIHints.PD)){
                    return true;
                }
            }
        }
        return (weaponTypes.contains(weaponEntry.getType())&&weaponSizes.contains(weaponEntry.getSize()));
    }

    public static float specialOPCostCalc(
        WeaponSpecAPI weapSpec, MutableCharacterStatsAPI captainStats, MutableShipStatsAPI shipStats, List<String> myIDs,List<StatBonus>statBonusAppend,
        boolean returnNegative
    ){
        //can copy statbonus. cant copy mutable*statsapi (easily). and since they pass by reference...i'm stuck with this implementation.
        //if it werent for that, I could just copy, unmodify, and run getOrdnancePointCost.
        float baseOP=weapSpec.getOrdnancePointCost(null);
        float procNum=processStack(getAllCostStatBonuses(weapSpec,captainStats,shipStats,statBonusAppend),false, myIDs,baseOP);
        if(!returnNegative){procNum=Math.max(0,procNum);}
        return procNum;
    }

    public static float lerp(float start, float stop, float progress) {
        return start+(progress*(stop-start));
    }

    public final static String RATSEXOSHIPNOREMOVALSTRING="Cannot be removed at an exoship.";
    public static boolean isThisRatsExoship(MarketAPI marketOrNull){
        return ((marketOrNull!=null)&&(marketOrNull.getId().startsWith("exoship_")||marketOrNull.getId().startsWith("exoship_broken_")));
    }
}
