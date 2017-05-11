import static org.junit.Assert.*;
import me.benroberts.lib.scuba.Mix;
import me.benroberts.lib.scuba.Units;
import me.benroberts.lib.scuba.Localizer;
import org.junit.Test;

public class MixTest {
	private static final Units u_i = new Units(Units.IMPERIAL),
		u_m = new Units(Units.METRIC);

	private static double tolerance = 0.000001;

	@Test
	public void constructorGetters() {
		Mix m = new Mix(0.21f, 0.35f);
		assertEquals(m.getO2(), 21, tolerance);
		assertEquals(m.getHe(), 35, tolerance);
		assertEquals(m.getfO2(), 0.21f, tolerance);
		assertEquals(m.getfHe(), 0.35f, tolerance);
		assertEquals(m.getfN2(), 0.44f, tolerance);

		// Error cases
		try {
			m = new Mix(0.51f, 0.51f);
			fail("Constructor accepted >100% gas sum");
		} catch(Mix.MixException e) {
		}

		try {
			m = new Mix(1.2f, 0);
			fail("Constructor accepted >100% O2");
		} catch(Mix.MixException e) {
		}

		try {
			m = new Mix(-0.5f, 0.2f);
			fail("Constructor accepted negative O2");
		} catch(Mix.MixException e) {
		}
	}

	@Test
	public void testEquals() {
		Mix m = Mix.AIR, m2 = new Mix(0.21f, 0);
		assertFalse(m.equals(Mix.HELIUM));
		assertTrue(m.equals(m2));
	}

	@Test
	public void stringsWork() {
		Localizer.Engine e = Localizer.getEngine();
		// Air
		Mix m = Mix.AIR;
		assertEquals(m.toString(), e.getString(Localizer.STRING_AIR));

		// Pure oxygen
		m = Mix.OXYGEN;
		assertEquals(m.toString(), e.getString(Localizer.STRING_OXYGEN));

		// Pure helium
		m = Mix.HELIUM;
		assertEquals(m.toString(), e.getString(Localizer.STRING_HELIUM));

		// Pure nitrogen
		m = new Mix(0, 0);
		assertEquals(m.toString(), e.getString(Localizer.STRING_NITROGEN));

		// Nitrox
		m = new Mix(0.32f, 0);
		assertEquals(m.toString(), "32%");

		// Trimix
		m = new Mix(0.1f, 0.5f);
		assertEquals(m.toString(), "10/50");
	}

	@Test
	public void computesMOD() {
		Mix m = new Mix(0.32f, 0);
		assertEquals(m.MOD(u_i, 1.4f), 111, tolerance);
		assertEquals(m.MOD(u_i, 1.6f), 132, tolerance);

		assertEquals(m.MOD(u_m, 1.4f), 33, tolerance);
		assertEquals(m.MOD(u_m, 1.6f), 40, tolerance);
	}

	@Test
	public void computesBestMix() {
		assertEquals(Mix.best(106, 106, u_i, 1.4f, false).toString(), "33%");
		// I hand-checked these results. Online calculators are not
		// reliable for cross-checking because they sometimes round
		// in an unsafe direction
		assertEquals(Mix.best(80, 40, u_m, 1.4f, false).toString(), "15/42");
		assertEquals(Mix.best(80, 40, u_m, 1.4f, true).toString(), "15/45");
	}

	@Test
	public void computesAB() {
		// Simple cases, which also test cache expiration
		Mix m = Mix.OXYGEN;
		assertEquals(m.getA(), Mix.A_OXYGEN, tolerance);
		assertEquals(m.getB(), Mix.B_OXYGEN, tolerance);
		m = Mix.HELIUM;
		assertEquals(m.getA(), Mix.A_HELIUM, tolerance);
		assertEquals(m.getB(), Mix.B_HELIUM, tolerance);

		// Because I could not find reliable published values for
		// a and b equivalents for trimixes, these results were
		// computed independently with the spreadsheet in doc/
		// They also match results at
		// http://www.nigelhewitt.co.uk/diving/maths/vdw.html
		m = Mix.AIR;
		assertEquals(m.getA(), 1.373, 0.001);
		assertEquals(m.getB(), 0.03727, 0.00001);
	}
}
