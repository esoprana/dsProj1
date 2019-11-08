package dsProj1;

public interface Options {
	static final int VIEWS_SIZE = 5;
	static final int UN_SUBS_SIZE = 5;
	static final int SUBS_SIZE = 5;
	static final int EVENT_IDS_SIZE = 5;
	static final int RETRIEVE_SIZE = 5;
	static final int EVENTS_SIZE = 5; // TODO: Check if needed	
	
	
	static final int FANOUT_SIZE = 3;
	
	static final int OLD_TIME_RETRIEVE = 5;
	static final int REQUEST_TIMEOUT_ROUNDS = 5;
	
	static final double MEAN_LATENCY = 0.013;
	static final double VAR_LATENCY = 0.003;
	
	static final double GOSSIP_INTERVAL = 30;
	static final double DRIFT_PER_SECOND = 1e-6;
	
	
	static final int NODE_COUNT = 35;
}
