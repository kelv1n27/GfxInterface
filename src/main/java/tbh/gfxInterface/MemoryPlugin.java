package tbh.gfxInterface;

import javax.swing.JFrame;

public abstract class MemoryPlugin extends JFrame{

	private int stakes;
	private String designation;
	protected boolean forceCollision = false;
	protected static GraphicsCardInterface gfx;
	
	public static void load(GraphicsCardInterface gfx) {
		MemoryPlugin.gfx = gfx;
	}
	
	public MemoryPlugin() {
		stakes = 1;
	}
	
	public int stake() {
		return ++stakes;
	}
	
	public int destake() {
		return --stakes;
	}
	
	public int getStakes() {
		return stakes;
	}
	
	public void setDesignation(String designation) {
		this.designation = designation;
	}
	
	public String getCurrentDesignation() {
		return designation;
	}
	
	@Override
	public String toString() {
		return "[" + stakes + "] [" + getClass().getCanonicalName().replaceAll("[a-zA-Z0-9\\-_]*\\.", "") + "] " + designation;
	}
	
	public abstract void updateDebug(int index);
	
	public abstract void addMemoryObject(int index);
	
	public abstract void removeMemoryObject(int index);
	
	public abstract Object retrieveMemoryObject(int index);
	
	public abstract String getDefaultDesignation();
}
