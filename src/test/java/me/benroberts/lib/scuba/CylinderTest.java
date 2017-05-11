import static org.junit.Assert.*;
import org.junit.Test;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import me.benroberts.lib.scuba.Cylinder;
import me.benroberts.lib.scuba.Mix;
import me.benroberts.lib.scuba.Units;

public class CylinderTest {
	private static final Units u_i = new Units(Units.IMPERIAL),
		u_m = new Units(Units.METRIC);

	private static final float tolerance = 0.0001f;

	@Test
	public void constructorGettersSetters() {
		Cylinder c = new Cylinder(u_i, 0.33f, 3000);

		assertNull(c.getName());
		c.setName("test name");
		assertEquals(c.getName(), "test name");
		assertNull(c.getId());
		c.setId(123);
		assertEquals(c.getId(), Long.valueOf(123));

		assertEquals(c.getUnits(), u_i);
		assertEquals(c.getInternalVolume(), 0.33f, tolerance);
		c.setInternalVolume(0.3f);
		assertEquals(c.getInternalVolume(), 0.3f, tolerance);

		assertEquals(c.getServicePressure(), 3000);
		c.setServicePressure(3300);
		assertEquals(c.getServicePressure(), 3300);

		c.setHydroInterval(5);
		assertEquals(c.getHydroInterval(), Integer.valueOf(5));
		c.setVisualInterval(12);
		assertEquals(c.getVisualInterval(), Integer.valueOf(12));

		Date now = new Date(), earlier = new Date();
		earlier.setTime(now.getTime() - 10000);
		c.setLastHydro(now);
		c.setLastVisual(earlier);
		assertEquals(c.getLastHydro(), now);
		assertEquals(c.getLastVisual(), earlier);
	}

	@Test
	public void factories() {
		// All we have to do is check consistency with capacity
		// calculations, as those methods have their own tests
		Cylinder c = Cylinder.fromCapacityIdeal(u_i, 77, 3000);
		assertEquals(c.getIdealCapacity(), 77, tolerance);
		c = Cylinder.fromCapacityVdw(u_i, 100, 3442);
		assertEquals(c.getVdwCapacity(), 100, tolerance);
	}

	@Test
	public void hydroViz() {
		hydroVizTests(new Date());

		Calendar cal = new GregorianCalendar();
		// Check if it works on the 1st of a month
		cal.set(Calendar.DAY_OF_MONTH, 1);
		Cylinder.setTestDate(cal.getTime());
		hydroVizTests(cal.getTime());
		// Check if it works on the 31st of a month
		cal.set(Calendar.MONTH, Calendar.MARCH);
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
		Cylinder.setTestDate(cal.getTime());
		hydroVizTests(cal.getTime());
		// Check if it works on Feb 28th of a leap year
		cal.set(Calendar.YEAR, 2012);
		cal.set(Calendar.MONTH, Calendar.FEBRUARY);
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
		Cylinder.setTestDate(cal.getTime());
		hydroVizTests(cal.getTime());
	}

	private void hydroVizTests(Date testDate) {
		Cylinder c = new Cylinder(u_i, 0.4f, 3000);
		Calendar cal = Calendar.getInstance();
		cal.setTime(testDate);

		// trivial case of unset dates should return false
		assertFalse(c.isVisualExpired());
		assertFalse(c.doesVisualExpireThisMonth());
		assertFalse(c.isHydroExpired());
		assertFalse(c.doesHydroExpireThisMonth());

		// nonstandard intervals
		int hydroInterval = 3, visualInterval = 8;
		c.setHydroInterval(hydroInterval);
		c.setVisualInterval(visualInterval);
		// Check very old dates (10 years ago)
		cal.add(Calendar.YEAR, -10);
		c.setLastHydro(cal.getTime());
		c.setLastVisual(cal.getTime());
		assertTrue(c.isHydroExpired());
		assertTrue(c.isVisualExpired());
		assertFalse(c.doesHydroExpireThisMonth());
		assertFalse(c.doesVisualExpireThisMonth());

		// Check for hydro expiring this month
		cal.setTime(testDate);
		// Prevent issues with running this test on the 31st of a
		// month, a leap year, etc.
		cal.set(Calendar.DAY_OF_MONTH, 10);
		cal.add(Calendar.YEAR, -1 * hydroInterval);
		c.setLastHydro(cal.getTime());
		assertFalse(c.isHydroExpired());
		assertTrue(c.doesHydroExpireThisMonth());

		// Check for visual expiring this month
		cal.setTime(testDate);
		cal.add(Calendar.MONTH, -1 * visualInterval);
		c.setLastVisual(cal.getTime());
		assertFalse(c.isVisualExpired());
		assertTrue(c.doesVisualExpireThisMonth());

		// Check for current hyro and viz
		cal.setTime(testDate);
		cal.add(Calendar.MONTH, -1 * visualInterval + 2);
		c.setLastVisual(cal.getTime());
		cal.add(Calendar.YEAR, -1);
		c.setLastHydro(cal.getTime());
		assertFalse(c.isVisualExpired());
		assertFalse(c.doesVisualExpireThisMonth());
		assertFalse(c.isHydroExpired());
		assertFalse(c.doesHydroExpireThisMonth());
	}

	@Test
	public void computesCapacities() {
		float vol = 0.4051f;
		int pressure = 2400;
		Cylinder c = new Cylinder(u_i, vol, pressure);

		float capacity = c.getIdealCapacity();
		assertEquals(capacity, vol * pressure / (double)u_i.pressureAtm(), tolerance);
		// Increase tolerance because errors propagate
		assertEquals(c.getIdealPressureAtCapacity(capacity), pressure, tolerance * 10);

		c.setIdealCapacity(95);
		assertEquals(c.getInternalVolume(), 0.58187f, tolerance);

		// Check Van der Waals math using metric units
		Cylinder c_m = new Cylinder(u_m, 11, 230);
		Mix air = new Mix(0.21f, 0);
		capacity = c_m.getVdwCapacity();
		c_m.setVdwCapacity(capacity);
		assertEquals(c_m.getVdwPressureAtCapacity(capacity, air), 230, 0.1f);

		// Handles case where pressure is 0
		assertEquals(c_m.getVdwCapacityAtPressure(0, air), 0, tolerance);

		// actual steel cylinder specs from
		// http://www.bluesteelscuba.com/cylinder-specifications.html
		// I stick to the largest cylinders because the relative
		// precision in the specs is higher
		Cylinder l95dvb = new Cylinder(u_i, 0.5295f, 2640); // 915 cuin == 0.5295 cuft
		assertEquals(l95dvb.getVdwCapacity(), 98, 0.05f); // blue steel says 95...

		// turn a L95DVB into a L120DVB
		l95dvb.setVdwCapacity(120);
		assertEquals(l95dvb.getInternalVolume(), 0.6707f, 0.05f); // 1159 cuin == 0.6707 cuft

		// A high pressure test, in metric
		// Blue Steel's numbers don't work out under multiple tests
		// I've done here, and reimplemented elsewhere. They may be
		// using higher-order equations of state to compute them.
		// For the below FX100DV:
		// - Blue Steel says it holds 2832 liters of gas (100 cuft / 0.03531).
		// - The ideal gas law says it holds 3057 liters.
		// - The Cylinder class with Van der Waals math, at ambient
		//   temp, says it holds 2970 liters, confirmed from solving
		//   the original cubic polynomial.
		// The below tests ensure the results do not deviate from
		// the verified solutions.
		Cylinder fx100dv = new Cylinder(u_m, 12.9f, 237);
		assertEquals(fx100dv.getVdwCapacity(), 2970, 0.5f);

		// turn it into a FX117DVB
		// 117 cuft == 3313 liters
		// should be 15 liters but by my independent calculations,
		// 14.39 is more like it
		fx100dv.setVdwCapacity(3313);
		assertEquals(fx100dv.getInternalVolume(), 14.39, 0.01f);
	}
}
