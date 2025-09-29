package kheTechMod.combat.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
//import org.apache.log4j.Logger;

public class KheTimeCleanup extends BaseEveryFrameCombatPlugin {
	//private static final Logger log = Logger.getLogger(KheTimeCleanup.class);
	private static final float baseGrace = 3f;
	private static final float tickRate = 0.1f;
	protected final IntervalUtil interval = new IntervalUtil(tickRate, tickRate);

	public final static Map<String, Float> myMap = new HashMap<>();

	private CombatEngineAPI engine;

	public void init(CombatEngineAPI engine) {
		this.engine = engine;
	}

	public void advance(float amount, List<InputEventAPI> events) {
		if (engine == null) return;
		if (engine.isPaused()) return;

		interval.advance(amount);
		if (interval.intervalElapsed()) {
			myMap.replaceAll((k, v) -> v - tickRate);
			for (Iterator<Map.Entry<String, Float>> it = myMap.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<String, Float> entry = it.next();
				//log.info("KheTimeCleanup:"+entry.getKey()+","+entry.getValue());
				if (entry.getValue() <= 0f) {
					Global.getCombatEngine().getTimeMult().unmodify(entry.getKey());
					it.remove();
				}
			}
		}
	}

	public static void registerTimeEffect(String id) {
		myMap.put(id, baseGrace);
	}
}
