package dsProj1;

// Support libraries
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;

public class Options {
	static int NODE_COUNT;

	static double INITIAL_VIEW_PERC;
	
	static int VIEWS_SIZE;
	static int UN_SUBS_SIZE;
	static int SUBS_SIZE;
	static int EVENT_IDS_SIZE;
	static int EVENTS_SIZE;
	
	static int FANOUT_SIZE;
	
	static int OLD_TIME_RETRIEVE;
	static int REQUEST_TIMEOUT_ROUNDS;
	
	static double MEAN_LATENCY;
	static double VAR_LATENCY;
	
	static double GOSSIP_INTERVAL;
	static double DRIFT_PER_SECOND;
	static long LONG_AGO;
	
	
	static double EVENTS_RATE;
	static double EVENTS_VAR_RATE;
	
	static double DROPPED_RATE;
	static double DEATH_RATE;
	static double EXPECTED_STABLE_TIME;

	static double K;
	
	static String EVENTS_OPTIMIZATION_FIRST;
	static String EVENTS_OPTIMIZATION_SECOND;
	
	static String EVENT_IDS_OPTIMIZATION_FIRST;
	static String EVENT_IDS_OPTIMIZATION_SECOND;
	
	static boolean SUBS_OPTIMIZATION;
	
	static void load() {
		Parameters  params = RunEnvironment.getInstance().getParameters();
		
		NODE_COUNT                = params.getInteger("NODE_COUNT");

		INITIAL_VIEW_PERC         = params.getDouble("INITIAL_VIEW_PERC");
		
		VIEWS_SIZE                = params.getInteger("VIEWS_SIZE");
		UN_SUBS_SIZE              = params.getInteger("UN_SUBS_SIZE");
		SUBS_SIZE                 = params.getInteger("SUBS_SIZE");
		EVENT_IDS_SIZE            = params.getInteger("EVENT_IDS_SIZE");
		EVENTS_SIZE               = params.getInteger("EVENTS_SIZE");
		FANOUT_SIZE               = params.getInteger("FANOUT_SIZE");
		OLD_TIME_RETRIEVE         = params.getInteger("OLD_TIME_RETRIEVE");
		REQUEST_TIMEOUT_ROUNDS    = params.getInteger("REQUEST_TIMEOUT_ROUNDS");
		MEAN_LATENCY              = params.getDouble("MEAN_LATENCY");
		VAR_LATENCY               = params.getDouble("VAR_LATENCY");
		GOSSIP_INTERVAL           = params.getDouble("GOSSIP_INTERVAL");
		DRIFT_PER_SECOND          = params.getDouble("DRIFT_PER_SECOND");
		LONG_AGO                  = params.getLong("LONG_AGO");
		EVENTS_RATE               = params.getDouble("EVENTS_RATE");
		EVENTS_VAR_RATE           = params.getDouble("EVENTS_VAR_RATE");
		DROPPED_RATE              = params.getDouble("DROPPED_RATE");
		DEATH_RATE                = params.getDouble("DEATH_RATE");
		EXPECTED_STABLE_TIME 	  = params.getDouble("EXPECTED_STABLE_TIME");
		K                         = params.getDouble("K");
		
		EVENTS_OPTIMIZATION_FIRST  = params.getString("EVENTS_OPTIMIZATION_FIRST").toUpperCase();
		EVENTS_OPTIMIZATION_SECOND = params.getString("EVENTS_OPTIMIZATION_SECOND").toUpperCase();
		
		EVENT_IDS_OPTIMIZATION_FIRST  = params.getString("EVENT_IDS_OPTIMIZATION_FIRST").toUpperCase();
		EVENT_IDS_OPTIMIZATION_SECOND = params.getString("EVENT_IDS_OPTIMIZATION_SECOND").toUpperCase();
		
		SUBS_OPTIMIZATION = params.getBoolean("SUBS_OPTIMIZATION");
	}
}
