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
	// Internal volume is stored in standard "capacity units" for the
	// defined unit system. Some unit systems (i.e. Imperial) have special
	// units that are typically used for measuring internal volumes because
	// capacity units are too large to be convenient. It is up to the
	// frontend to convert capacity units returned by this class if it is
	// desired to do so.
	private float mInternalVolume;
	// The service pressure
	private int mServicePressure;
	
	private String mName;
	
	private int type = 0;

	private String serialNumber;

	/**
	 * A constant for classifying a Cylinder object as "generic," meaning
	 * that it represents a class of cylinders rather than a specific
	 * physical one.
	 *
	 * @see #setType(int)
	 */
	public static final int TYPE_GENERIC = 0;
	/**
	 * A constant for classifying a Cylinder object as "specific," meaning
	 * that it is a specific physical cylinder with unique characteristics
	 * like a serial number and inspection dates.
	 *
	 * @see #setType(int)
	 */
	public static final int TYPE_SPECIFIC = 1;
	
	private Date lastHydro, lastVisual;
	private Integer hydroIntervalYears, visualIntervalMonths;
	
	private static int defHydroIntervalYears = 5, defVisualIntervalMonths = 12;
	private static Date testDate = null;

	private Units mUnits;

	/**
	 * Create a Cylinder with the given unit system, internal volume, and
	 * service pressure. The constructor is meant to take values as
	 * returned from a tank data model which stores internal volumes and
	 * service pressures (the metric way).
	 *
	 * @param units A Units object set to the desired measurement system
	 * @param internal_volume Internal volume of the cylinder in capacity units
	 * @param service_pressure Service pressure of the cylinder
	 */
	public Cylinder(Units units, float internal_volume, int service_pressure) {
		mUnits = units;
		mInternalVolume = internal_volume;
		mServicePressure = service_pressure;
	}

	/**
	 * Set this Cylinder object's unique ID. This is typically used in
	 * combination with an ORM library.
	 *
	 * @param id The ID to assign
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * Get this Cylinder object's unique ID. This is typically used in
	 * combination with an ORM library.
	 *
	 * @return The Cylinder's ID, or null if it does not have one
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Get this Cylinder's assigned name.
	 *
	 * @return The Cylinder's name
	 */
	public String getName() { return mName; }
	/**
	 * Set this Cylinder's name.
	 * @param name The name to assign
	 */
	public Cylinder setName(String name) { mName = name; return this; }

	/**
	 * Build a Cylinder object with a nominal capacity instead of an
	 * internal volume. This method uses the Van der Waals equation of
	 * state to compute the correct internal volume.
	 *
	 * @param units A Units object set to the desired measurement system
	 * @param capacity The nominal volume of the cylinder's contents
	 * at sea level when the cylinder is filled to the service pressure
	 * @param service_pressure Service pressure of the cylinder
	 * @return A Cylinder object initialized with the given parameters
	 */
	public static Cylinder fromCapacityVdw(Units units, float capacity, int service_pressure) {
		Cylinder c = new Cylinder(units, 0, service_pressure);
		c.setVdwCapacity(capacity);
		return c;
	}

	/**
	 * Build a Cylinder object with a nominal capacity instead of an
	 * internal volume. This method uses the ideal gas equation of
	 * state to compute the correct internal volume.
	 *
	 * @param units A Units object set to the desired measurement system
	 * @param capacity The nominal volume of the cylinder's contents
	 * at sea level when the cylinder is filled to the service pressure
	 * @param service_pressure Service pressure of the cylinder
	 * @return A Cylinder object initialized with the given parameters
	 */
	public static Cylinder fromCapacityIdeal(Units units, float capacity, int service_pressure) {
		Cylinder c = new Cylinder(units, 0, service_pressure);
		c.setIdealCapacity(capacity);
		return c;
	}

	/**
	 * Get the {@link Units} object for this Cylinder.
	 *
	 * @return The Units object
	 */
	public Units getUnits() {
		return mUnits;
	}

	/**
	 * Get the air capacity of the Cylinder, computed with the Van der
	 * Waals state equation.
	 *
	 * @return The nominal volume of the cylinder's contents at sea level
	 * when the cylinder is filled with air to the service pressure, in
	 * capacity units
	 */
	public float getVdwCapacity() {
		return (float)getVdwCapacityAtPressure(mServicePressure, Mix.AIR);
	}

	/**
	 * Get the air capacity of the Cylinder, computed with the ideal gas
	 * state equation.
	 *
	 * @return The nominal volume of the cylinder's contents at sea level
	 * when the cylinder is filled with air to the service pressure, in
	 * capacity units
	 */
	public float getIdealCapacity() {
		return (float)getIdealCapacityAtPressure(mServicePressure);
	}

	/**
	 * Set the internal volume of the Cylinder based on a nominal capacity,
	 * computed with the ideal gas state equation.
	 *
	 * @param capacity The desired nominal volume of the cylinder's
	 * contents at sea level when the cylinder is filled with air to the
	 * service pressure, in capacity units
	 * @return This Cylinder object
	 */
	public Cylinder setIdealCapacity(float capacity) {
		mInternalVolume = capacity * mUnits.pressureAtm() / mServicePressure;
		return this;
	}

	/**
	 * Set the internal volume of the Cylinder based on a nominal capacity,
	 * computed with the Van der Waals state equation.
	 *
	 * @param capacity The desired nominal volume of the cylinder's
	 * contents at sea level when the cylinder is filled with air to the
	 * service pressure, in capacity units
	 * @return This Cylinder object
	 */
	public Cylinder setVdwCapacity(float capacity) {
		// TODO: at what temperature do the cylinder manufacturers
		// determine tank capacity?

		double RT = mUnits.absTempAmbient() * mUnits.gasConstant();
		// We know what n is because we were given capacity:
		double n = mUnits.pressureAtm() * capacity / RT;

		// Uncertainty math (see below)
		// V = nv
		// dV/dv = n
		float uncertainty = new Float(0.1 / n).floatValue();
		double v = vRoot(mServicePressure, Mix.AIR, mUnits.absTempAmbient(), uncertainty);
		mInternalVolume = (float)(v * n);
		return this;
	}

	/**
	 * Get the internal volume of this Cylinder
	 *
	 * @return The internal volume in capacity units
	 */
	public float getInternalVolume() {
		return mInternalVolume;
	}

	/**
	 * Set the internal volume of this Cylinder
	 *
	 * @param internal_volume The internal volume to apply
	 * @return This Cylinder object
	 */
	public Cylinder setInternalVolume(float internal_volume) {
		mInternalVolume = internal_volume;
		return this;
	}

	/**
	 * Get the service pressure of this Cylinder
	 *
	 * @return The service pressure
	 */
	public int getServicePressure() {
		return mServicePressure;
	}

	/**
	 * Set the service pressure of this Cylinder
	 *
	 * @param service_pressure The service pressure to apply
	 * @return This Cylinder object
	 */
	public Cylinder setServicePressure(int service_pressure) {
		mServicePressure = service_pressure;
		return this;
	}

	/**
	 * Set the type of cylinder object. A Cylinder can be either
	 * {@link #TYPE_GENERIC generic}, meaning it is only meant to
	 * be representative of a class of Cylinders of the same
	 * characteristics, or {@link #TYPE_SPECIFIC specific}, meaning it
	 * represents one physical Cylinder with unique features like a serial
	 * number and test dates.
	 *
	 * @param type The type to assign. See the above constants.
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * Get this Cylinder's {@link #setType(int) type}.
	 *
	 * @return The type as an int. Compare with the type constants to
	 * interpret it.
	 */
	public int getType() {
		return type;
	}

	/**
	 * Set this Cyliner's serial number.
	 *
	 * @param serial_number The serial number to apply
	 */
	public void setSerialNumber(String serial_number) {
		this.serialNumber = serial_number;
	}

	/**
	 * Get this Cylinder's serial number.
	 *
	 * @return The serial number
	 */
	public String getSerialNumber() {
		return serialNumber;
	}

	/**
	 * Set this Cylinder's last hydro test date.
	 *
	 * @param last_hydro The date to apply
	 */
	public void setLastHydro(Date last_hydro) {
		lastHydro = last_hydro;
	}

	/**
	 * Get this Cylinder's last hydro test date.
	 *
	 * @return The hydro test date
	 */
	public Date getLastHydro() {
		return lastHydro;
	}

	/**
	 * Get this Cylinder's last visual inspection date.
	 *
	 * @return The last visual date
	 */
	public Date getLastVisual() {
		return lastVisual;
	}

	/**
	 * Set this Cylinder's last visual inspection date.
	 *
	 * @param last_visual The date to apply
	 */
	public void setLastVisual(Date last_visual) {
		lastVisual = last_visual;
	}

	/**
	 * Compute the nominal volume of gas this Cylinder holds at the
	 * given pressure. This method uses the ideal gas law to compute the
	 * result.
	 *
	 * @param pressure The pressure at which to determine capacity
	 * @return The amount of gas, in capacity units
	 */
	public double getIdealCapacityAtPressure(double pressure) {
		return mInternalVolume * pressure / (double)mUnits.pressureAtm();
	}

	/**
	 * Compute the gas pressure in this Cylinder if it contained the given
	 * nominal volume of gas. This method uses the ideal gas law to
	 * compute the result.
	 *
	 * @param capacity The nominal volume at which to determine pressure
	 * @return The pressure
	 */
	public double getIdealPressureAtCapacity(double capacity) {
		return capacity * mUnits.pressureAtm() / mInternalVolume;
	}

	/**
	 * Compute the nominal volume of gas this Cylinder holds at the given
	 * pressure, using the Van der Waals gas equation.
	 *
	 * @param pressure The pressure of the gas in the cylinder
	 * @param mix The mix in the cylinder, needed to determine a and b constants.
	 * @return The amount of gas in the cylinder to one decimal place
	 */
	public double getVdwCapacityAtPressure(double pressure, Mix mix) {
		return getVdwCapacityAtPressure(pressure, mix, mUnits.absTempAmbient());
	}

	/**
	 * Compute the nominal volume of gas this Cylinder holds at the given
	 * pressure, using the Van der Waals gas equation.
	 *
	 * @param pressure The pressure of the gas in the cylinder
	 * @param mix The mix in the cylinder, needed to determine a and b constants.
	 * @param temperature The absolute temperature of the Cylinder
	 * @return The amount of gas in the cylinder to one decimal place
	 */
	public double getVdwCapacityAtPressure(double pressure, Mix mix, float temperature) {
		// First, the trivial solution. This will cause a divide by 0
		// if we try to solve.
		if(pressure == 0) {
			return 0;
		}
		// First-order uncertainty propagation. This lets us know
		// within what tolerance we need to compute v to get the right
		// volume.
		// The variable we are solving for is v.
		// The result we care about the uncertainty for is V_a, the
		// volume at 1 ata.
		//   V_a = n * R * T / P_a [ideal gas law] = V * R * T / (P_a * v)
		// To compute the uncertainty in V0, we use the Taylor series
		// method for v alone.
		//   deltaV_a = dV_a/dv*deltav
		// ...where dV_a/dv = - V * R * T / (P_a * v^2)
		// We want to make sure deltaV_a is less than 0.05, so...
		//   deltav < P_a * v^2 / (20 * V * R * T)
		// Our best guess for v is from ideal gas law, which will be
		// close enough for estimating uncertainty.
		// v0 = RT / P
		// so deltav < P_a * R * T / (20 * P^2 * V)
		float RT = mUnits.gasConstant() * temperature,
		      uncertainty = new Float(mUnits.pressureAtm() * RT / (20 * pressure * pressure * mInternalVolume)).floatValue();
		double v = vRoot(pressure, mix, temperature, uncertainty);
		return mInternalVolume * RT / (mUnits.pressureAtm() * v);
	}

	// Finds the root of a cubic polynomial for the molar volume v = V/n:
	// choose a reasonable value for T
	//   P * v^3 - (P*b + R*T) * v^2 + a * v - a * b = 0
	//   n = V/v
	// Once v is known, 1-ata capacities can be computed accurately
	// enough with the ideal gas law.
	private double vRoot(double P, Mix mix, float T, float uncertainty) {
		double a = mUnits.convertA(mix.getA(), Units.METRIC),
		       b = mUnits.convertCapacity(mix.getB(), Units.METRIC),
		       RT = T * mUnits.gasConstant();
		// A bit of optimization to reduce number of calculations per
		// iteration
		double PbRT = P*b + RT, PbRT2 = 2 * PbRT, ab = a * b, P3 = 3 * P;
		// Come up with a guess to seed Newton-Raphson. The equation is
		// easily solved if a and b were 0 (ideal)
		double v0, v1 = RT / P, range = -1, last_f;
		do {
			v0 = v1;
			double f = P * Math.pow(v0, 3) - PbRT * Math.pow(v0, 2) + a * v0 - ab,
			       fprime = P3 * Math.pow(v0, 2) - PbRT2 * v0 + a;
			v1 = v0 - f / fprime;
		} while(Math.abs(v0 - v1) / v1 >= uncertainty);
		return v1;
	}

	/**
	 * Compute the gas pressure in this Cylinder if it contained the given
	 * nominal volume of gas, using the Van der Waals gas equation.
	 *
	 * @param capacity The nominal volume at which to determine pressure
	 * @param mix The gas in the Cylinder
	 * @return The pressure
	 */
	public double getVdwPressureAtCapacity(double capacity, Mix mix) {
		return getVdwPressureAtCapacity(capacity, mix, mUnits.absTempAmbient());
	}

	/**
	 * Compute the gas pressure in this Cylinder if it contained the given
	 * nominal volume of gas, using the Van der Waals gas equation.
	 *
	 * @param capacity The nominal volume at which to determine pressure
	 * @param mix The gas in the Cylinder
	 * @param temperature The absolute temperature of the Cylinder
	 * @return The pressure
	 */
	public double getVdwPressureAtCapacity(double capacity, Mix mix, float temperature) {
		// This is given by the following:
		// choose a reasonable value for T
		// n = Patm*V/(R*T) (since volume is at atmospheric pressure, it's close enough to ideal)
		// v = V/n
		// P = R * T / (v - b) - a / v^2
		double RT = temperature * mUnits.gasConstant();
		double v = mInternalVolume * RT / (mUnits.pressureAtm() * capacity),
				a = mUnits.convertA(mix.getA(), Units.METRIC), b = mUnits.convertCapacity(mix.getB(), Units.METRIC);
		return RT / (v - b) - a / (v * v);
	}

	/**
	 * Set the default number of years between hydro tests for all
	 * cylinders. This can be overridden on individual Cylinder objects
	 * with {@link #setHydroInterval(Integer)}.
	 *
	 * @param years The number of years allowed between hydro tests
	 */
	public static void setDefHydroInterval(int years) {
		defHydroIntervalYears = years;
	}

	/**
	 * Set the default number of years between visual inspections for all
	 * cylinders. This can be overridden on individual Cylinder objects
	 * with {@link #setVisualInterval(Integer)}.
	 *
	 * @param months The number of months allowed between visual
	 * inspections
	 */
	public static void setDefVisualInterval(int months) {
		defVisualIntervalMonths = months;
	}

	/**
	 * Set the allowed number of years between hydro tests for this
	 * Cylinder.
	 *
	 * @param years The number of years allowed between hydro tests
	 */
	public void setHydroInterval(Integer years) {
		hydroIntervalYears = years;
	}

	/**
	 * Get the allowed number of years between hydro tests for this
	 * Cylinder.
	 *
	 * @return The number of years allowed between hydro tests
	 */
	public Integer getHydroInterval() {
		return hydroIntervalYears;
	}

	/**
	 * Set the allowed number of months between visual inspections for
	 * this Cylinder.
	 *
	 * @param months The number of months allowed between visual
	 * inspections
	 */
	public void setVisualInterval(Integer months) {
		visualIntervalMonths = months;
	}

	/**
	 * Get the allowed number of months between visual inspections for
	 * this Cylinder.
	 *
	 * @return The number of months allowed between visual inspections
	 */
	public Integer getVisualInterval() {
		return visualIntervalMonths;
	}

	/**
	 * Override the date used to check if hydro or visual tests have
	 * expired or are about to expire. By default,
	 * {@link #isHydroExpired()}, {@link #doesHydroExpireThisMonth()},
	 * etc. use the current date/time to reach their results. This
	 * method allows running these methods against some other date,
	 * mainly for testing purposes.
	 *
	 * @param date The date relative to which all Cylinder tests run
	 */
	public static void setTestDate(Date date) {
		testDate = date;
	}

	private Date now() {
		return (testDate == null)? new Date(): testDate;
	}

	private void makeEndOfMonth(Calendar cal) {
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
		cal.set(Calendar.HOUR_OF_DAY, cal.getActualMaximum(Calendar.HOUR_OF_DAY));
		cal.set(Calendar.MINUTE, cal.getActualMaximum(Calendar.MINUTE));
		cal.set(Calendar.SECOND, cal.getActualMaximum(Calendar.SECOND));
	}

	private void makeStartOfMonth(Calendar cal) {
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
		cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
		cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
		cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
	}

	private Calendar getOneMonthLater(Calendar cal) {
		Calendar l = (Calendar)cal.clone();
		l.add(Calendar.MONTH, 1);
		return l;
	}

	/**
	 * Check if this Cylinder's {@link #setLastHydro(Date) last hydro test}
	 * has expired.
	 *
	 * @return True if the last hydro is set and has expired, false
	 * otherwise
	 */
	public boolean isHydroExpired() {
		if(lastHydro == null) {
			return false;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(lastHydro);
		cal.add(Calendar.YEAR, hydroIntervalYears != null? hydroIntervalYears: defHydroIntervalYears);
		makeEndOfMonth(cal);
		return now().after(cal.getTime());
	}

	/**
	 * Check if this Cylinder's
	 * {@link #setLastVisual(Date) last visual inspection} has expired.
	 *
	 * @return True if the last visual is set and has expired, false
	 * otherwise
	 */
	public boolean isVisualExpired() {
		if(lastVisual == null) {
			return false;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(lastVisual);
		cal.add(Calendar.MONTH, visualIntervalMonths != null? visualIntervalMonths: defVisualIntervalMonths);
		makeEndOfMonth(cal);
		return now().after(cal.getTime());
	}

	/**
	 * Check if this Cylinder's {@link #setLastHydro(Date) last hydro test}
	 * expires this month.
	 *
	 * @return True if the last hydro is set and expires this month, false
	 * otherwise
	 */
	public boolean doesHydroExpireThisMonth() {
		if(lastHydro == null) {
			return false;
		}
		// lastMonth == the start of the last valid month of the
		// test.
		// firstExpiredMonth == the start of the first month that
		// the test would be considered expired (one month after
		// lastMonth)
		Calendar lastMonth = Calendar.getInstance(),
			 firstExpiredMonth;
		Date now = now();
		lastMonth.setTime(lastHydro);
		lastMonth.add(Calendar.YEAR, hydroIntervalYears != null? hydroIntervalYears: defHydroIntervalYears);
		makeStartOfMonth(lastMonth);
		firstExpiredMonth = getOneMonthLater(lastMonth);
		return now.after(lastMonth.getTime()) && now.before(firstExpiredMonth.getTime());
	}

	/**
	 * Check if this Cylinder's
	 * {@link #setLastVisual(Date) last visual inspection} expires this
	 * month.
	 *
	 * @return True if the last visual is set and expires this month,
	 * false otherwise
	 */
	public boolean doesVisualExpireThisMonth() {
		if(lastVisual == null) {
			return false;
		}
		Calendar lastMonth = Calendar.getInstance(),
			 firstExpiredMonth;
		Date now = now();
		lastMonth.setTime(lastVisual);
		lastMonth.add(Calendar.MONTH, visualIntervalMonths != null? visualIntervalMonths: defVisualIntervalMonths);
		makeStartOfMonth(lastMonth);
		firstExpiredMonth = getOneMonthLater(lastMonth);
		return now.after(lastMonth.getTime()) && now.before(firstExpiredMonth.getTime());
	}
}
