package me.benroberts.lib.scuba;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

/**
 * A class that represents a gas cylinder (or a manifolded set of cylinders)
 * @author Ben Roberts (ben@benroberts.me)
 */
public class Cylinder implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long id;

	// The total physical volume of the cylinder(s)
	// Internal volume is stored in standard "capacity units" for the defined
	// unit system. Some unit systems (i.e. Imperial) have special units that
	// are typically used for measuring internal volumes because capacity units
	// are too large to be convenient. It is up to the frontend to convert
	// capacity units returned by this class if it is desired to do so.
	private float mInternalVolume;
	// The service pressure
	private int mServicePressure;
	
	private String mName;
	
	private int type = 0;

	private String serialNumber;
	
	public static final int TYPE_GENERIC = 0;
	public static final int TYPE_SPECIFIC = 1;
	
	private Date lastHydro, lastVisual;
	private Integer hydroIntervalYears, visualIntervalMonths;
	
	private static int defHydroIntervalYears = 5, defVisualIntervalMonths = 12;

	private Units mUnits;

	/**
	 * Constructor is meant to take values as returned from a tank data model
	 * which stores internal volumes and service pressures (the metric way).
	 *
	 * @param internal_volume Internal volume of the cylinder in capacity units
	 * @param service_pressure Service pressure of the cylinder
	 */
	public Cylinder(Units units, float internal_volume, int service_pressure) {
		mUnits = units;
		mInternalVolume = internal_volume;
		mServicePressure = service_pressure;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public String getName() { return mName; }
	public Cylinder setName(String name) { mName = name; return this; }

	/**
	 * Build a Cylinder object with a capacity instead of an internal volume
	 * @param capacity The volume of gas the cylinder's contents would occupy at
	 * sea level pressure when the cylinder is filled to the service pressure
	 * @param service_pressure Service pressure of the cylinder
	 * @return A Cylinder object initialized with the given parameters
	 */
	public static Cylinder fromCapacityVdw(Units units, float capacity, int service_pressure) {
		Cylinder c = new Cylinder(units, 0, service_pressure);
		c.setVdwCapacity(capacity);
		return c;
	}
	
	public static Cylinder fromCapacityIdeal(Units units, float capacity, int service_pressure) {
		Cylinder c = new Cylinder(units, 0, service_pressure);
		c.setIdealCapacity(capacity);
		return c;
	}
	
	public Units getUnits() {
		return mUnits;
	}

	/** Returns the air capacity of the cylinder(s)
	 * @return The volume of gas the cylinder's contents would occupy at sea level
	 * pressure when the cylinder is filled with air to the service pressure, in
	 * capacity units
	 */
	public float getVdwCapacity() {
		return (float)getVdwCapacityAtPressure(mServicePressure, new Mix(0.21f, 0));
	}

	public float getIdealCapacity() {
		return (float)getIdealCapacityAtPressure(mServicePressure);
	}

	public Cylinder setIdealCapacity(float capacity) {
		mInternalVolume = capacity * mUnits.pressureAtm() / mServicePressure;
		return this;
	}

	public Cylinder setVdwCapacity(float capacity) {
		// This is quite similar to getVdwCapacityAtPressure, except
		// we are solving for V instead of n. The cubic
		// polynomial is the same, it's just that the
		// uncertainty is calculated differently.
		Mix m = new Mix(0.21f, 0);
		double a = m.getA(), b = m.getB();
		// TODO: at what temperature do the cylinder manufacturers determine
		// tank capacity?
		double RT = mUnits.absTempAmbient() * mUnits.gasConstant();
		// A bit of optimization to reduce number of calculations per iteration
		double PbRT = mServicePressure*b + RT, PbRT2 = 2 * PbRT, ab = a * b, P3 = 3 * mServicePressure;
		// Come up with a guess to seed Newton-Raphson. The equation is easily
		// solved if a and b were 0
		double v0, v1 = RT / mServicePressure;
		
		// We know what n is because we were given capacity:
		double n = mUnits.pressureAtm() * capacity / RT;
		
		// Uncertainty math (see below)
		// V = nv
		// dV/dv = n
		float uncertainty = (float)(n / Math.pow(10, mUnits.volumePrecision()) / 2f);
		
		do {
			v0 = v1;
			double f = mServicePressure * Math.pow(v0, 3) - PbRT * Math.pow(v0, 2) + a * v0 - ab;
			double fprime = P3 * Math.pow(v0, 2) - PbRT2 * v0 + a;
			v1 = v0 - f / fprime;
		} while(Math.abs(v0 - v1) >= uncertainty);

		mInternalVolume = (float)(v1 * n);
		return this;
	}

	/**
	 * Get the internal volume of this cylinder
	 * @return The internal volume in capacity units
	 */
	public float getInternalVolume() {
		return mInternalVolume;
	}

	public Cylinder setInternalVolume(float internal_volume) {
		mInternalVolume = internal_volume;
		return this;
	}

	public int getServicePressure() {
		return mServicePressure;
	}

	public Cylinder setServicePressure(int service_pressure) {
		mServicePressure = service_pressure;
		return this;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public void setLastHydro(Date lastHydro) {
		this.lastHydro = lastHydro;
	}

	public Date getLastHydro() {
		return lastHydro;
	}

	public Date getLastVisual() {
		return lastVisual;
	}

	public void setLastVisual(Date lastVisual) {
		this.lastVisual = lastVisual;
	}

	public double getIdealCapacityAtPressure(double pressure) {
		return mInternalVolume * pressure / (double)mUnits.pressureAtm();
	}

	public double getIdealPressureAtCapacity(double capacity) {
		return capacity * mUnits.pressureAtm() / (double)mInternalVolume;
	}

	/**
	 * Solves Van der Waals gas equation to get equivalent atmospheric volume at
	 * a given pressure
	 * @param P The pressure of the gas in the cylinder
	 * @param mix The mix in the cylinder, needed to determine a and b constants.
	 * @return The amount of gas in the cylinder to one decimal place
	 */
	public double getVdwCapacityAtPressure(double P, Mix m) {
		return getVdwCapacityAtPressure(P, m, mUnits.absTempAmbient());
	}

	public double getVdwCapacityAtPressure(double P, Mix m, float T) {
		// First, the trivial solution. This will cause a divide by 0 if we try to
		// solve.
		if(P == 0) {
			return 0;
		}
		// This is solved by finding the root of a cubic polynomial for the molar
		// volume v = V/n:
		// choose a reasonable value for T
		//   P * v^3 - (P*b + R*T) * v^2 + a * v - a * b = 0
		//   n = V/v
		// Then we can use ideal gas laws to convert n to V @ 1 ata
		double a = m.getA(), b = m.getB();
		double RT = T * mUnits.gasConstant();
		// A bit of optimization to reduce number of calculations per iteration
		double PbRT = P*b + RT, PbRT2 = 2 * PbRT, ab = a * b, P3 = 3 * P;
		// Come up with a guess to seed Newton-Raphson. The equation is easily
		// solved if a and b were 0 (ideal)
		double v0, v1 = RT / P;

		// First-order uncertainty propagation. This lets us know within what
		// tolerance we need to compute v to get the right volume.
		// The variable we are solving for is v.
		// The result we care about the uncertainty for is V0, the volume at 1 ata.
		//   V0 = n * R * T / P0 [ideal gas law] = V * R * T / (P0 * v)
		// To compute the uncertainty in V0, we use the Taylor series method for
		// v alone.
		//   deltaV0 = dV0/dv*deltav
		// ...where dV0/dv = - V*R*T / (P0 * v^2)
		// We want to make sure deltaV0 is less than 0.05, so...
		//   deltav < P0 * v^2 / (20 * V * R * T)
		double uncertainty_multiplier = mUnits.pressureAtm() / (20 * mInternalVolume * RT);

		do {
			v0 = v1;
			double f = P * Math.pow(v0, 3) - PbRT * Math.pow(v0, 2) + a * v0 - ab;
			double fprime = P3 * Math.pow(v0, 2) - PbRT2 * v0 + a;
			v1 = v0 - f / fprime;
		} while(Math.abs(v0 - v1) / uncertainty_multiplier >= v1 * v1);

		return mInternalVolume * RT / (mUnits.pressureAtm() * v1);
	}

	public double getVdwPressureAtCapacity(double capacity, Mix m) {
		return getVdwPressureAtCapacity(capacity, m, mUnits.absTempAmbient());
	}

	public double getVdwPressureAtCapacity(double capacity, Mix m, float T) {
		// This is given by the following:
		// choose a reasonable value for T
		// n = Patm*V/(R*T) (since volume is at atmospheric pressure, it's close enough to ideal)
		// v = V/n
		// P = R * T / (v - b) - a / v^2
		double RT = T * mUnits.gasConstant();
		double v = mInternalVolume * RT / (mUnits.pressureAtm() * capacity),
				a = m.getA(), b = m.getB();
		return RT / (v - b) - a / (v * v);
	}

	public static void setDefHydroInterval(int years) {
		defHydroIntervalYears = years;
	}

	public static void setDefVisualInterval(int months) {
		defVisualIntervalMonths = months;
	}

	public void setHydroInterval(Integer years) {
		hydroIntervalYears = years;
	}

	public Integer getHydroInterval() {
		return hydroIntervalYears;
	}

	public void setVisualInterval(Integer months) {
		visualIntervalMonths = months;
	}

	public Integer getVisualInterval() {
		return visualIntervalMonths;
	}

	public boolean isHydroExpired() {
		if(lastHydro == null) {
			return false;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(lastHydro);
		cal.add(Calendar.YEAR, hydroIntervalYears != null? hydroIntervalYears: defHydroIntervalYears);
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
		cal.set(Calendar.HOUR_OF_DAY, cal.getActualMaximum(Calendar.HOUR_OF_DAY));
		cal.set(Calendar.MINUTE, cal.getActualMaximum(Calendar.MINUTE));
		cal.set(Calendar.SECOND, cal.getActualMaximum(Calendar.SECOND));
		return new Date().after(cal.getTime());
	}

	public boolean isVisualExpired() {
		if(lastVisual == null) {
			return false;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(lastVisual);
		cal.add(Calendar.MONTH, visualIntervalMonths != null? visualIntervalMonths: defVisualIntervalMonths);
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
		cal.set(Calendar.HOUR_OF_DAY, cal.getActualMaximum(Calendar.HOUR_OF_DAY));
		cal.set(Calendar.MINUTE, cal.getActualMaximum(Calendar.MINUTE));
		cal.set(Calendar.SECOND, cal.getActualMaximum(Calendar.SECOND));
		return new Date().after(cal.getTime());
	}

	public boolean doesHydroExpireThisMonth() {
		if(lastHydro == null) {
			return false;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(lastHydro);
		cal.add(Calendar.YEAR, hydroIntervalYears != null? hydroIntervalYears: defHydroIntervalYears);
		return new Date().after(cal.getTime());
	}

	public boolean doesVisualExpireThisMonth() {
		if(lastVisual == null) {
			return false;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(lastVisual);
		cal.add(Calendar.MONTH, visualIntervalMonths != null? visualIntervalMonths: defVisualIntervalMonths);
		return new Date().after(cal.getTime());
	}
}
