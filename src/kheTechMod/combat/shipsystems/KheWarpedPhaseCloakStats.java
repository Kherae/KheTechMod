package kheTechMod.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.PhaseCloakStats;
//import org.apache.log4j.Logger;

public class KheWarpedPhaseCloakStats extends PhaseCloakStats {
    //private final Logger log = Logger.getLogger(KheWarpedPhaseCloakStats.class);
    public static float PHASE_COOLDOWN_DISSIPATION_MULT=1.2f;
    public static float UNCLOAK_FLUX_DISSIPATE_FRACTION=0.1f;
    public static boolean UNCLOAK_DISSIPATES_HARDFLUX=true;

    protected Object STATUSKEYFLUXMULT = new Object();//flux dissipation notice

    public boolean phaseToggledOn;
	
	protected void maintainStatus(ShipAPI playerShip, State state, float effectLevel) {
        super.maintainStatus(playerShip, state, effectLevel);

        ShipSystemAPI cloak = playerShip.getPhaseCloak();
        if (cloak == null) cloak = playerShip.getSystem();
        if (cloak == null) return;
        if(state==State.COOLDOWN){
            Global.getCombatEngine().maintainStatusForPlayerShip(
                STATUSKEYFLUXMULT,cloak.getSpecAPI().getIconSpriteName(),
                "phase system venting","flux dissipation x"+PHASE_COOLDOWN_DISSIPATION_MULT+": "+Math.round(cloak.getCooldownRemaining()*10f)/10f+"s remaining.", true
            );
        }
    }

	public void apply(MutableShipStatsAPI stats, String id, State state, float level) {
        super.apply(stats, id, state, level);

        ShipAPI ship;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            id = id + "_" + ship.getId();
        } else {
            return;
        }
        if (state == State.ACTIVE || state == State.IN) {
            if(!phaseToggledOn){
                phaseToggledOn=true;
            }
        }else if (phaseToggledOn) {
            FluxTrackerAPI fluxTracker = ship.getFluxTracker();

            if(UNCLOAK_DISSIPATES_HARDFLUX){fluxTracker.setHardFlux(Math.max(0,fluxTracker.getHardFlux()-(fluxTracker.getMaxFlux()*UNCLOAK_FLUX_DISSIPATE_FRACTION)));}
            fluxTracker.setCurrFlux(Math.max(0,fluxTracker.getCurrFlux()-(fluxTracker.getMaxFlux()*UNCLOAK_FLUX_DISSIPATE_FRACTION)));
            phaseToggledOn=false;
            //StatBonus thing = ship.getMutableStats().getSystemCooldownBonus();
            //float otherThing = thing.computeEffective(1f);
            //log.info("other thing "+otherThing);
        }

        if (state == State.COOLDOWN){
            stats.getFluxDissipation().modifyMult(id+"_PCDM",PHASE_COOLDOWN_DISSIPATION_MULT);
        }else{
            stats.getFluxDissipation().unmodify(id+"_PCDM");
        }
	}

}
