package me.benroberts.lib.scuba;

/**
 * A class that represents a complete gas system: a {@link Cylinder}
 * containing a {@link Mix} filled to a given pressure. Provides methods to
 * add and remove gas from the tank.
 * @author Ben Roberts (ben@benroberts.me)
 */
public class GasSupply implements Cloneable {
	private Mix mMix;
	private Cylinder mCylinder;
	private double mPressure;
	private float mTemperature;

	/**
	 * A constant representing the ideal gas equation of state, PV = nRT
	 * @see #setState(int)
	 */
	public static final int STATE_IDEAL_GAS = 1;
	/**
	 * A constant representing the Van der Waals equation of state,
	 * (P + an^2 / V^2)(V - nb) = nRT
	 * @see #setState(int)
	 */
	public static final int STATE_VAN_DER_WAALS = 2;

	private int mState;

	/**
	 * Create a new, empty gas source for a given Cylinder. The constructor
	 * selects an air Mix, the Van der Waals equation of state, and a
	 * typical temperature.
	 * @param cylinder The {@link Cylinder} object to use for this supply
	 */
	public GasSupply(Cylinder cylinder) {
		this(cylinder, Mix.AIR, 0);
	}

	/**
	 * Create a new gas source from a Cylinder, an initial Mix, and a
	 * starting pressure. The constructor selects the Van der Waals
	 * equation of state and a typical temperature.
	 * @param cylinder The {@link Cylinder} object to use for this supply
	 * @param mix The initial {@link Mix} in this supply
	 * @param pressure The initial pressure of the contents, in
	 * the same units that were used for the Cylinder object
	 */
	public GasSupply(Cylinder cylinder, Mix mix, int pressure) {
		this(cylinder, mix, pressure, STATE_VAN_DER_WAALS);
	}

	/**
	 * Create a new gas source from a Cylinder, an initial Mix, and a
	 * starting pressure, while also specifying the state equation to use
	 * for later computations.
	 * @param cylinder The {@link Cylinder} object to use for this supply
	 * @param mix The initial {@link Mix} in this supply
	 * @param pressure The initial pressure of the cylinder's content, in
	 * the same units that were used for the Cylinder object
	 * @param state Pass one of the STATE_* global constants to pick one
	 */
	public GasSupply(Cylinder cylinder, Mix mix, int pressure, int state) {
		this(cylinder, mix, pressure, state, cylinder.getUnits().absTempAmbient());
	}

	/**
	 * Create a new gas source from a Cylinder, an initial Mix, a
	 * starting pressure, and temperature, while also specifying the state
	 * equation to use for later computations.
	 * @param cylinder The {@link Cylinder} object to use for this supply
	 * @param mix The initial {@link Mix} in this supply
	 * @param pressure The initial pressure of the Cylinder's content, in
	 * the same units that were used for the Cylinder object
	 * @param state Pass one of the STATE_* global constants to pick one
	 * @param temperature The ambient temperature, in the same units that
	 * were used for the Cylinder object
	 */
	public GasSupply(Cylinder cylinder, Mix mix, int pressure, int state, float temperature) {
		mMix = mix;
		mCylinder = cylinder;
		mPressure = pressure;
		mState = state;
		mTemperature = temperature;
	}

	@Override
	public GasSupply clone() {
		try {
			return (GasSupply)super.clone();
		} catch (CloneNotSupportedException e) {
			// Impossible since we implemented Cloneable
			return null;
		}
	}

	/**
	 * Get a value representing the equation of state in use on this
	 * supply.
	 * @return A constant corresponding to one of the STATE_* constants
	 */
	public int getState() {
		return mState;
	}

	/**
	 * Set the equation of state to use on this supply.
	 * @param state One of the STATE_* constants
	 */
	public void setState(int state) {
		mState = state;
	}

	@Deprecated
	public void useIdealGasLaws(boolean set) {
		if(set)
			setState(STATE_IDEAL_GAS);
		else {
			setState(STATE_VAN_DER_WAALS);
		}
	}

	/**
	 * Get the {@link Mix} contained in this supply.
	 * @return The supply's Mix
	 */
	public Mix getMix() {
		return mMix;
	}

	/**
	 * Set the Mix contained in this supply. No computations are done,
	 * rather the old Mix is simply replaced.
	 * @param mix The {@link Mix} to set this supply's contents to
	 */
	public void setMix(Mix mix) {
		mMix = mix;
	}

	/**
	 * Get the {@link Cylinder} that this supply is using.
	 * @return The Cylinder object
	 */
	public Cylinder getCylinder() {
		return mCylinder;
	}

	/**
	 * Set the Cylinder to use for this supply. No computations are done,
	 * rather the old Cylinder is simply replaced.
	 * @param cylinder The {@link Cylinder} object to use
	 */
	public void setCylinder(Cylinder cylinder) {
		mCylinder = cylinder;
	}

	/**
	 * Get the current pressure in this supply.
	 * @return The pressure
	 */
	public double getPressure() {
		return mPressure;
	}

	/**
	 * Set the current pressure in this supply. No computations are done,
	 * rather the current pressure is just altered.
	 * @param pressure The pressure to set
	 */
	public void setPressure(int pressure) {
		mPressure = pressure;
	}

	/**
	 * Get the temperature of this supply.
	 * @return The supply temperature
	 */
	public float getTemperature() {
		return mTemperature;
	}

	/**
	 * Set this supply's temperature.
	 * @param temperature The supply temperature to set
	 */
	public void setTemperature(float temperature) {
		mTemperature = temperature;
	}

	/**
	 * Get the nominal volume of gas in this supply.
	 * @return The amount of gas, in capacity units
	 */
	public double getGasAmount() {
		if(mState == STATE_IDEAL_GAS) {
			return mCylinder.getIdealCapacityAtPressure(mPressure);
		} else {
			return mCylinder.getVdwCapacityAtPressure(mPressure, mMix, mTemperature);
		}
	}

	/**
	 * Get the nominal volume of oxygen in this supply.
	 * @return The amount of oxygen, in capacity units
	 */
	public double getO2Amount() {
		return getGasAmount() * mMix.getfO2();
	}

	/**
	 * Get the nominal volume of nitrogen in this supply.
	 * @return The amount of nitrogen, in capacity units
	 */
	public double getN2Amount() {
		return getGasAmount() * mMix.getfN2();
	}

	/**
	 * Get the nominal volume of helium in this supply.
	 * @return The amount of helium, in capacity units
	 */
	public double getHeAmount() {
		return getGasAmount() * mMix.getfHe();
	}

	/**
	 * Adjust the pressure in this supply so there's the given nominal
	 * volume of gas. Theoretically this method can adjust pressure up or
	 * down to match the desired amount, but the typical real-world
	 * application is draining prior to starting a gas blend.
	 * @param gas_amount The new nominal volume of gas in this supply, in
	 * capacity units
	 * @return This object
	 */
	public GasSupply drainToGasAmount(double gas_amount) {
		if(mState == STATE_IDEAL_GAS) {
			mPressure = mCylinder.getIdealPressureAtCapacity(gas_amount);
		} else {
			mPressure = mCylinder.getVdwPressureAtCapacity(gas_amount, mMix, mTemperature);
		}
		return this;
	}

	/**
	 * Adjust the pressure in this supply downward so there's no more
	 * than the given nominal volume of oxygen.
	 * @param o2_amount The new nominal volume of oxygen in this supply,
	 * in capacity units
	 * @return This object
	 */
	public GasSupply drainToO2Amount(double o2_amount) {
		// A catch for the trivial solution; we're already there
		// (this is most important if amt is 0 and so is the amount in
		// this supply currently)
		if(o2_amount - getO2Amount() >= -0.0001) {
			return this;
		}
		return drainToGasAmount(o2_amount / mMix.getfO2());
	}

	/**
	 * Adjust the pressure in this supply downward so there's no more
	 * than the given nominal volume of nitrogen.
	 * @param n2_amount The new nominal volume of nitrogen in this supply,
	 * in capacity units
	 * @return This object
	 */
	public GasSupply drainToN2Amount(double n2_amount) {
		if(n2_amount - getN2Amount() >= -0.0001) {
			return this;
		}
		return drainToGasAmount(n2_amount / mMix.getfN2());
	}

	/**
	 * Adjust the pressure in this supply downward so there's no more
	 * than the given nominal volume of helium.
	 * @param he_amount The new nominal volume of helium in this supply,
	 * in capacity units
	 * @return This object
	 */
	public GasSupply drainToHeAmount(double he_amount) {
		if(he_amount - getHeAmount() >= -0.0001) {
			return this;
		}
		return drainToGasAmount(he_amount / mMix.getfHe());
	}

	/**
	 * Add a given nominal volume of oxygen.
	 * @param o2_amount The amount of oxygen to add, in capacity units
	 * @return This object
	 * @see addGas
	 */
	public GasSupply addO2(double o2_amount) {
		return addGas(new Mix(1, 0), o2_amount);
	}

	/**
	 * Add a given nominal volume of helium.
	 * @param he_amount The amount of helium to add, in capacity units
	 * @return This object
	 * @see addGas
	 */
	public GasSupply addHe(double he_amount) {
		return addGas(new Mix(0, 1), he_amount);
	}

	/**
	 * Add a given nominal volume of a Mix. This method updates the Mix
	 * and pressure according to this supply's current contents.
	 * @param mix The {@link Mix} to add
	 * @param amount The amount of gas to add, in capacity units
	 * @return This object
	 */
	public GasSupply addGas(Mix mix, double amount) {
		double current_amt = getGasAmount(),
				o2 = mMix.getfO2() * current_amt + mix.getfO2() * amount,
				he = mMix.getfHe() * current_amt + mix.getfHe() * amount,
				new_total_amt = current_amt + amount;
		mMix = new Mix(o2 / new_total_amt, he / new_total_amt);
		if(mState == STATE_IDEAL_GAS) {
			mPressure = mCylinder.getIdealPressureAtCapacity(new_total_amt);
		} else {
			mPressure = mCylinder.getVdwPressureAtCapacity(new_total_amt, mMix, mTemperature);
		}
		return this;
	}

	/**
	 * Add a gas to the current contents of this supply until reaching a
	 * given pressure. This method updates the Mix according to this
	 * supply's current contents, and updates the pressure.
	 * @param mix The {@link Mix} to add
	 * @param final_pressure The final pressure for this supply
	 * @return This object.
	 */
	public GasSupply topup(Mix mix, int final_pressure) {
		// Trivial solution: we're adding the same mix that's already
		// in this supply
		if(mMix.equals(mix)) {
			mPressure = final_pressure;
		} else {
			// Uses the Secant Method to numerically determine the
			// result to within 0.1% of each final mix. We do this
			// because writing out the single equation for the
			// system would be terrible, not to mention calculating
			// its derivative for N-R.

			double error = 0.005, d;
			Mix current_mix = mMix;
			GasSupply test = clone();

			// Start with two guesses for Secant Method
			// The first guess assumes ideal behavior as the gas is
			// added, and assumes the topup mix is close enough to
			// determine capacity.
			double vt_n = (1 - mPressure / (float)final_pressure) * mCylinder.getVdwCapacityAtPressure(final_pressure, mix, mTemperature);
			// The second guess assumes ideal behavior as the gas
			// is added, and assumes the starting mix is close
			// enough to determine capacity.
			double vt_n_1 = (1 - mPressure / (float)final_pressure) * mCylinder.getVdwCapacityAtPressure(final_pressure, current_mix, mTemperature);

			do {
				// Initialize a temporary GasSupply. Because
				// addGas acts on the object, we have to
				// re-instantiate it each time.
				test.mPressure = mPressure;
				test.setMix(current_mix);
				// Each computation evaluates the difference
				// between the actual pressure after adding a
				// certain amount of gas, and the desired
				// pressure.
				double f_n = test.addGas(mix, vt_n).getPressure() - final_pressure;
				test.mPressure = mPressure;
				test.setMix(current_mix);
				double f_n_1 = test.addGas(mix, vt_n_1).getPressure() - final_pressure;
				d = (vt_n - vt_n_1) / (f_n - f_n_1) * f_n;
				vt_n_1 = vt_n;
				vt_n -= d;
			} while(Math.abs(d) > error);

			// Now that we have our solution, run addGas on self.
			addGas(mix, vt_n);
			// Cheat! Set mPressure to what would be expected
			// since addGas may not have gotten it exactly.
			mPressure = final_pressure;
		}
		return this;
	}
}
