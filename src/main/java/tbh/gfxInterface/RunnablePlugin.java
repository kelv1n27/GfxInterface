package tbh.gfxInterface;

public abstract class RunnablePlugin {
	
	protected static GraphicsCardInterface gfx;
	
	public static void load(GraphicsCardInterface gfx) {
		RunnablePlugin.gfx = gfx;
	}
	
	public abstract void run(Object[] args);

}
