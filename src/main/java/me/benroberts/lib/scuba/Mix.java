package me.benroberts.lib.scuba;

import java.text.NumberFormat;

/**
 * This is a class for a given gas mixture of oxygen, nitrogen, and helium.
 * Mix objects are immutable.
 * @author Ben Roberts (ben@benroberts.me)
 */
public class Mix extends GasSource {
	// The fraction of oxygen
	private double mO2;
	
	// The fraction of helium
	private double mHe;
	
	/**
	 * A Mix that represents Earth's surface atmosphere to an acceptable
	 * approximation.
	 */
	public static final Mix AIR = new Mix(0.21f, 0);

	/**
	 * A Mix that represents pure oxygen.
	 */
	public static final Mix OXYGEN = new Mix(1, 0);

	/**
	 * A Mix that represents pure helium.
	 */
	public static final Mix HELIUM = new Mix(0, 1);

	/**
	 * The value of Van der Waals' particle attraction factor <i>a</i> for
	 * molecular oxygen.
	 * Value is in metric (L^2 bar / mol^2) units.
	 * @see <a href="http://www2.ucdsb.on.ca/tiss/stretton/database/van_der_waals_constants.html">Van der Waals Constants</a>
	 */
	public static final float A_OXYGEN = 1.382f;
	/**
	 * The value of Van der Waals' particle attraction factor <i>a</i> for
	 * helium.
	 * Value is in metric (L^2 bar / mol^2) units.
	 * @see <a href="http://www2.ucdsb.on.ca/tiss/stretton/database/van_der_waals_constants.html">Van der Waals Constants</a>
	 */
	public static final float A_HELIUM = 0.0346f;
	/**
	 * The value of Van der Waals' particle attraction factor <i>a</i> for
	 * molecular nitrogen.
	 * Value is in metric (L^2 bar / mol^2) units.
	 * @see <a href="http://www2.ucdsb.on.ca/tiss/stretton/database/van_der_waals_constants.html">Van der Waals Constants</a>
	 */
	public static final float A_NITROGEN = 1.37f;
	/**
	 * The value of Van der Waals' particle volume factor <i>b</i> for
	 * molecular oxygen.
	 * Value is in metric (L / mol) units.
	 * @see <a href="http://www2.ucdsb.on.ca/tiss/stretton/database/van_der_waals_constants.html">Van der Waals Constants</a>
	 */
	public static final float B_OXYGEN = 0.03186f;
	/**
	 * The value of Van der Waals' particle volume factor <i>b</i> for
	 * helium.
	 * Value is in metric (L / mol) units.
	 * @see <a href="http://www2.ucdsb.on.ca/tiss/stretton/database/van_der_waals_constants.html">Van der Waals Constants</a>
	 */
	public static final float B_HELIUM = 0.0238f;
	/**
	 * The value of Van der Waals' particle volume factor <i>b</i> for
	 * molecular nitrogen.
	 * Value is in metric (L / mol) units.
	 * @see <a href="http://www2.ucdsb.on.ca/tiss/stretton/database/van_der_waals_constants.html">Van der Waals Constants</a>
	 */
	public static final float B_NITROGEN = 0.0387f;

	/**
	 * Create a gas mix of the given fractions of oxygen and helium.
	 * <p>
	 * Passing arguments that would produce an impossible gas will result
	 * in a MixException.
	 * @param o2 The fraction of oxygen
	 * @param he The fraction of helium
	 */
	public Mix(double o2, double he) {
		if(o2 < 0 || he < 0) {
			throw new MixException("A Mix cannot contain negative percentages of gases");
		}
		if(o2 + he > 1) {
			throw new MixException("A Mix cannot contain more than 100% oxygen and helium");
		}
		mO2 = o2;
		mHe = he;
	}
	
	/**
	 * Get the percentage of O2 in the mix, from 0 to 100.
	 * @return The O2 %
	 */
	public float getO2() {
		return (float)(mO2 * 100);
	}
	
	/**
	 * Get the fraction of O2 in the mix, from 0 to 1.
	 * @return The O2 fraction
	 */
	public double getfO2() {
		return mO2;
	}

	/**
	 * Get the percentage of helium in the mix, from 0 to 100.
	 * @return The helium %
	 */
	public float getHe() {
		return (float)(mHe * 100);
	}
	
	/**
	 * Get the fraction of helium in the mix, from 0 to 1.
	 * @return The helium fraction
	 */
	public double getfHe() {
		return mHe;
	}
	
	/**
	 * Get the fraction of nitrogen in the mix, from 0 to 1.
	 * @return The nitrogen fraction
	 */
	public double getfN2() {
		return 1 - mHe - mO2;
	}

	@Override
	public boolean equals(Object o2) {
		if(super.equals(o2)) {
			return true;
		}
		if(! (o2 instanceof Mix)) {
			return false;
		}
		Mix m2 = (Mix)o2;
		return Math.abs(getfO2() - m2.getfO2()) < 0.0005 &&
			Math.abs(getfHe() - m2.getfHe()) < 0.0005;
	}

	@Override
	public String toString() {
		NumberFormat nf = NumberFormat.getIntegerInstance();
		Localizer.Engine e = Localizer.getEngine();
		if(getO2() == 100) {
			return e.getString(Localizer.STRING_OXYGEN);
		}
		if(getHe() == 100) {
			return e.getString(Localizer.STRING_HELIUM);
		}
		if(getO2() + getHe() == 0) {
			return e.getString(Localizer.STRING_NITROGEN);
		}
		if((getO2() == 21) && (getHe() == 0)) {
			return e.getString(Localizer.STRING_AIR);
		}
		if(getHe() == 0) {
			// A Nitrox mix
			return nf.format(getO2())+"%";
		}
		// After all of the above, we have a trimix
		return nf.format(getO2())+"/"+nf.format(getHe());
	}

	public double pHeAtDepth(int depth, float surfacePressure, Units units) {
		return (depth / units.depthPerAtm() + surfacePressure) * getfHe();
	}

	public double pN2AtDepth(int depth, float surfacePressure, Units units) {
		return (depth / units.depthPerAtm() + surfacePressure) * getfN2();
	}

	public double pO2AtDepth(int depth, float surfacePressure, Units units) {
		return (depth / units.depthPerAtm() + surfacePressure) * getfO2();
	}
	
	/**
	 *  Return the Maximum Operating Depth of this mix.
	 * @param maxpO2 The maximum desired partial pressure of oxygen (usually 1.4 or 1.6)
	 * @return The maximum operating depth in the current system of units, rounded down to the nearest standard depth increment
	 */
	public float MOD(Units units, float maxpO2) {
		return ((int) Math.floor((maxpO2 / mO2 - 1) * units.depthPerAtm() + 0.01) / units.depthIncrement()) * units.depthIncrement();
	}
	
	/**
	 * Get the minimum depth at which this mix may be breathed.
	 * @param minpO2 The minimum desired pO2 to have when breathing this
	 * mix, in ata (usually .16 or .17)
	 * @param units A {@link Units} object set to the desired measurement
	 * system for the result
	 * @return The minimum depth in the given units, rounded up to the nearest standard increment
	 */
	public float ceiling(float minpO2, Units units) {
		// This function is nearly identical to MOD except we round up instead of
		// down
		return ((int) Math.ceil((minpO2 / mO2 - 1) * units.depthPerAtm() - 0.01) / units.depthIncrement()) * units.depthIncrement();
	}
	
	/**
	 * Determine the best mix to use for a given depth based on the desired
	 * MOD and END at that MOD.
	 * @param depth The maximum desired depth for this mix in the current
	 * system of units
	 * @param maxEND The desired END. If you don't want a helium mix, pass
	 * the same value for mod and end.
	 * @param units A {@link Units} object set to the desired measurement
	 * system for the result
	 * @param maxpO2 The maximum desired partial pressure of oxygen in ata
	 * (usually 1.4 or 1.6)
	 * @param oxygenIsNarcotic Whether or not to consider the narcotic
	 * effects of oxygen in the calculation
	 * @return The Mix containing the highest possible percentage of
	 * oxygen and the lowest possible percentage of helium for the given
	 * parameters, rounded down/up to whole percentages. Returns null if
	 * it is impossible to satisfy the given requirements.
	 */
	public static Mix best(int depth, int maxEND, Units units, float maxpO2, boolean oxygenIsNarcotic) {
		float dpa = units.depthPerAtm();
		float pAbs = depth / dpa + 1;	// absolute pressure in ata
		float fO2Best = maxpO2 / pAbs;
		if(fO2Best > 1) {
			fO2Best = 1;
		} else {
			fO2Best = (float) (Math.floor(fO2Best * 100 + 0.0001) / 100);
		}
		maxEND = Math.min(depth, maxEND);
		float pNarc0 = oxygenIsNarcotic? 1: 0.79f;
		float fNarcBest = (float) (Math.floor((maxEND / dpa + 1) / pAbs * pNarc0 * 100 + 0.0001) / 100);
		float fHeBest = Math.max(1 - (oxygenIsNarcotic? fNarcBest: fNarcBest + fO2Best), 0);
		try {
			return new Mix(fO2Best, fHeBest);
		} catch(MixException e) {
			return null;
		}
	}

	// Internal variables used for caching the computed a and b values for
	// this mix
	private float mCacheA = -1, mCacheB = -1;

	// This private method does our a and b calculations and caches the
	// results in private variables. This way the computation only has to
	// be done once on a given mix.
	// See Kwak and Mansoori, 1985 http://trl.lab.uic.edu/KwakvdwMRs.pdf
	private void computeAB() {
		double x[] = { mO2, getfN2(), mHe };
		float a[] = { A_OXYGEN, A_NITROGEN, A_HELIUM };
		float b[] = { B_OXYGEN, B_NITROGEN, B_HELIUM };
		double aij, bij;
		mCacheA = 0;
		mCacheB = 0;
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 3; j++) {
				// Eq 4 from Kwak and Mansoori, zero coupling parameter
				aij = Math.sqrt(a[i] * a[j]);
				mCacheA += Double.valueOf(aij * x[i] * x[j]).floatValue();
			}
			// Eq 3.1 from Kwak and Mansoori. As much as I'd like
			// to use their results, it would require coupling
			// parameters which vary depending on the gas and
			// would not work for trivial cases like pure O2.
			mCacheB += Double.valueOf(b[i] * x[i]).floatValue();
		}
	}
	
	/**
	 * Returns the particle attraction factor a for a theoretical
	 * homogeneous gas equivalent in behavior to the given gas mixture.
	 * @return The value of a, in bar L^2 / mol^2.
	 */
	public float getA() {
		if(mCacheA == -1) {
			computeAB();
		}
		return mCacheA;
	}
	
	/**
	 * Returns the particle volume factor b for a theoretical homogeneous
	 * gas equivalent in behavior to the given gas mixture.
	 * @return The value of b, in L / mol.
	 */
	public float getB() {
		if(mCacheB == -1) {
			computeAB();
		}
		return mCacheB;
	}

	/**
	 * Thrown when a Mix is instantiated or modified in an impossible way
	 */
	public class MixException extends RuntimeException {

		public MixException(String message) {
			super(message);
		}
	}
}
