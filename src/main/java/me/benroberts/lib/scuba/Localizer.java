package me.benroberts.lib.scuba;

/**
 * @author Ben Roberts (ben@benroberts.me)
 */
public class Localizer {

	// Named gases
	public static final int STRING_OXYGEN = 1;
	public static final int STRING_HELIUM = 2;
	public static final int STRING_NITROGEN = 3;
	public static final int STRING_AIR = 4;

	public interface Engine {
		public String getString(int resource);
	}

	private static Engine mEngine = new DummyEngine();
	
	public static Engine getEngine() { return mEngine; }
	public static void setEngine(Engine e) { mEngine = e; }

	// This lets classes work without worrying having to worry about
	// localization if you don't care
	private static class DummyEngine implements Engine {
		public String getString(int resource) {
			switch(resource) {
				case STRING_OXYGEN:
					return "oxygen";
				case STRING_HELIUM:
					return "helium";
				case STRING_NITROGEN:
					return "nitrogen";
				case STRING_AIR:
					return "air";
				default:
					return "";
			}
		}
	}
}
