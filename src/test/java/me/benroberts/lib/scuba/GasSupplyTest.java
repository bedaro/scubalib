import static org.junit.Assert.*;
import me.benroberts.lib.scuba.Cylinder;
import me.benroberts.lib.scuba.Mix;
import me.benroberts.lib.scuba.Units;
import me.benroberts.lib.scuba.GasSupply;
import org.junit.Test;

public class GasSupplyTest {
	private static final Units u_i = new Units(Units.IMPERIAL),
		u_m = new Units(Units.METRIC);

	private static double tolerance = 0.0001;

	@Test
	public void constructorGettersSetters() {
		Cylinder c = new Cylinder(u_i, 572, 3000),
			 c2 = new Cylinder(u_i, 1000, 2400);
		Mix air = new Mix(0.21f, 0), nx = new Mix(0.32f, 0),
		    tmx = new Mix(0.18f, 0.4f);
		GasSupply partial = new GasSupply(c, air, 1200),
			  partial_ideal = new GasSupply(c, air, 1200, true),
			  partial_hot = new GasSupply(c, air, 1200, false, 570);

		assertEquals(partial.getMix(), air);
		assertEquals(partial.getCylinder(), c);
		assertEquals(partial.getPressure(), 1200, tolerance);
		assertEquals(partial_hot.getTemperature(), 570, tolerance);
		assertFalse(partial.getUseIdealGasLaws());
		assertTrue(partial_ideal.getUseIdealGasLaws());

		partial.useIdealGasLaws(false);
		assertFalse(partial.getUseIdealGasLaws());
		partial.useIdealGasLaws(true);

		partial.setCylinder(c2);
		assertEquals(partial.getCylinder(), c2);
		partial.setCylinder(c);

		partial.setMix(nx);
		assertEquals(partial.getMix(), nx);
		partial.setMix(air);

		partial.setPressure(1600);
		assertEquals(partial.getPressure(), 1600, tolerance);
		partial.setPressure(1200);

		partial.setTemperature(500);
		assertEquals(partial.getTemperature(), 500, tolerance);
		partial.setTemperature(partial_ideal.getTemperature());
	}

	@Test
	public void canClone() {
		Cylinder c = new Cylinder(u_i, 572, 3000);
		Mix air = new Mix(0.21f, 0);
		GasSupply partial = new GasSupply(c, air, 1200),
			  clone = partial.clone();
		assertNotSame(partial, clone);
		assertSame(partial.getMix(), clone.getMix());
		assertSame(partial.getCylinder(), clone.getCylinder());
		assertEquals(partial.getPressure(), clone.getPressure(), tolerance);
		assertEquals(partial.getUseIdealGasLaws(), clone.getUseIdealGasLaws());
		assertEquals(partial.getTemperature(), clone.getTemperature(), tolerance);

		// Now make sure mutable operations do not affect the clone
		// through child objects
		partial.drainToGasAmount(100).addGas(new Mix(0.32f, 0), 300);
		assertNotSame(partial.getMix(), clone.getMix());
		assertNotSame(partial.getPressure(), clone.getPressure());
	}

	@Test
	public void computeGasAmounts() {
		int cylCapacity = 572, servPressure = 3000;
		Cylinder c = new Cylinder(u_i, cylCapacity, servPressure);
		Mix air = new Mix(0.21f, 0);
		GasSupply real = new GasSupply(c, air, servPressure / 2),
			  ideal = new GasSupply(c, air, servPressure / 2, true);

		// A lot of the computation itself is tested in CylinderTest.
		// Here we just have to make sure the right Cylinder methods
		// are being called.
		double realAmount = real.getGasAmount();
		assertEquals(ideal.getGasAmount(), c.getIdealCapacityAtPressure(servPressure / 2), tolerance);
		assertEquals(realAmount, c.getVdwCapacityAtPressure(servPressure / 2, air, real.getTemperature()), tolerance);

		assertEquals(real.getO2Amount(), realAmount * air.getfO2(), tolerance);
		assertEquals(real.getN2Amount(), realAmount * air.getfN2(), tolerance);
		assertEquals(real.getHeAmount(), realAmount * air.getfHe(), tolerance);
	}

	@Test
	public void drain() {
		int cylVolume = 572, servPressure = 3000;
		Cylinder c = new Cylinder(u_i, cylVolume, servPressure);
		Mix air = new Mix(0.21f, 0);
		GasSupply real = new GasSupply(c, air, servPressure / 2),
			  ideal = new GasSupply(c, air, servPressure / 2, true);
		// Trivial cases
		// supplies are at 50% already. Drain to half capacity on the
		// ideal supply should not change anything.
		GasSupply notDrained = ideal.clone().drainToGasAmount(c.getIdealCapacity() / 2);
		assertEquals(ideal.getGasAmount(), notDrained.getGasAmount(), tolerance);
	}
}
