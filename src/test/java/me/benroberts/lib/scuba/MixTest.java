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
	public void constructorGettersSetters() {
		Mix m = new Mix(0.21f, 0.35f);
		assertEquals(m.getO2(), 21, tolerance);
		assertEquals(m.getHe(), 35, tolerance);
		assertEquals(m.getfO2(), 0.21f, tolerance);
		assertEquals(m.getfHe(), 0.35f, tolerance);
		assertEquals(m.getfN2(), 0.44f, tolerance);

		m.setfO2(0.18f);
		m.setfHe(0.4f);
		assertEquals(m.getfO2(), 0.18f, tolerance);
		assertEquals(m.getfHe(), 0.4f, tolerance);

		// Make sure something strange isn't happening with 0% He
		m.setfHe(0);
		assertEquals(m.getfHe(), 0, tolerance);
	}

	@Test
	public void stringsWork() {
		Localizer.Engine e = Localizer.getEngine();
		// Air
		Mix m = new Mix(0.21f, 0);
		assertEquals(m.toString(), e.getString(Localizer.STRING_AIR));

		// Pure oxygen
		m.setfO2(1);
		assertEquals(m.toString(), e.getString(Localizer.STRING_OXYGEN));

		// Pure helium
		m.setfO2(0);
		m.setfHe(1);
		assertEquals(m.toString(), e.getString(Localizer.STRING_HELIUM));

		// Pure nitrogen
		m.setfHe(0);
		assertEquals(m.toString(), e.getString(Localizer.STRING_NITROGEN));

		// Nitrox
		m.setfO2(0.32f);
		assertEquals(m.toString(), "32%");

		// Trimix
		m.setfO2(0.1f);
		m.setfHe(0.5f);
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
		Mix m = new Mix(1, 0);
		assertEquals(m.getA(), Mix.A_OXYGEN, tolerance);
		assertEquals(m.getB(), Mix.B_OXYGEN, tolerance);
		m.setfO2(0);
		m.setfHe(1);
		assertEquals(m.getA(), Mix.A_HELIUM, tolerance);
		assertEquals(m.getB(), Mix.B_HELIUM, tolerance);

		// Because I could not find reliable published values for
		// a and b equivalents for trimixes, these results were
		// computed independently with the spreadsheet in doc/
		// They also match results at
		// http://www.nigelhewitt.co.uk/diving/maths/vdw.html
		Mix air = new Mix(0.21f, 0);
		assertEquals(air.getA(), 1.373, 0.001);
		assertEquals(air.getB(), 0.03727, 0.00001);
	}
}
