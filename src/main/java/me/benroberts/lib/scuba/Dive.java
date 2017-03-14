package me.benroberts.lib.scuba;

import java.util.List;

// TODO add Type support
/**
 * @author Ben Roberts (ben@benroberts.me)
 */
public class Dive {

	private Long id;
	protected String mName;
	protected long mMissionID;
	protected Mission mMission;
	protected int mMissionOrder;
	protected int mAltitude = 0;
	protected int mAcclimatizationTime = 0;
	protected long mDecosetID;
	protected Decoset mDecoset;
	protected List<ProfileItem> mProfile = null;
	protected Dive mPreviousDive = null;
	protected int mSurfaceInterval = 0;
	protected byte[] mDecoConfig;
	protected byte[] mFinalDecoState;
	protected float mFinalCnsState, mFinalOtuState;
	
	protected Units mUnits;
	
	public Dive(Units units, long mission_id, int mission_order, String name, long decoset_id) {
		mUnits = units;
		mMissionID = mission_id;
		mMissionOrder = mission_order;
		mName = name;
		mDecosetID = decoset_id;
	}
	
	public void setId(long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}
	public String getName() { return mName; }
	public Dive setName(String name) { mName = name; return this; }
	public long getMissionID() { return mMissionID; }
	public long getDecosetID() { return mDecosetID; }
	public int getAltitude() { return mAltitude; }
	public Dive setAltitude(int altitude) { mAltitude = altitude; return this; }
	public int getAcclimatizationTime() { return mAcclimatizationTime; }
	public Dive setAcclimatizationTime(int acclimatizationTime) { mAcclimatizationTime = acclimatizationTime; return this; }
	public int getSurfaceInterval() { return mSurfaceInterval; }
	public Dive setSurfaceInterval(int surfaceInterval) { mSurfaceInterval = surfaceInterval; return this; }

	public Mission getMission() {
		return mMission;
	}

	public Decoset getDecoset() {
		return mDecoset;
	}
	public Dive setDecoset(Decoset decoset) {
		if(decoset.getId() != mDecosetID) {
			mDecoset = decoset;
			mDecosetID = decoset.getId();
		}
		return this;
	}
	
	public List<ProfileItem> getProfile() {
		return mProfile;
	}

	/**
	 * Construct the CnsOtu state object initialized to the beginning of the dive.
	 * This object must have a DiveFetcher defined or this
	 * method will throw a NullPointerException. 
	 * @return The initialized CnsOtu object
	 */
	public CnsOtu buildCnsOtu() {
		float cns = 0, otu = 0;
		if(mPreviousDive != null) {
			cns = mPreviousDive.mFinalCnsState;
			otu = mPreviousDive.mFinalOtuState;
		}
		return new CnsOtu(mAltitude, mUnits, cns, otu);
	}
	
	public void saveCnsOtu(CnsOtu state) {
		float cns = state.getCns();
		mFinalCnsState = cns;
		float otu = state.getOtu();
		mFinalOtuState = otu;
	}

	public void initializeDeco(DecoAlgorithm alg) {
		alg.loadConfig(mDecoConfig);
		Dive previous = mPreviousDive;
		if(previous != null) {
			alg.loadState(previous.mFinalDecoState);
		}
		alg.setDecoset(getDecoset());

		Mix air = new Mix(0.21f, 0);
		if(mSurfaceInterval > mAcclimatizationTime && previous != null) {
			// The diver ascended from the last dive before
			// changing altitude. Account for that time first.
			alg.surfaceInterval(previous.getAltitude(), mSurfaceInterval - mAcclimatizationTime, air);
		}
		alg.surfaceInterval(mAltitude, mAcclimatizationTime, air);
	}
	
	public void saveDeco(DecoAlgorithm alg) {
		byte[] finalDecoState = alg.getState();
		mFinalDecoState = finalDecoState;
	}
}
