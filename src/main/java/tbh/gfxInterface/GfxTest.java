package tbh.gfxInterface;

import memoryPlugins.IntArrayImage;

public class GfxTest
{
    public static void main( String[] args )
    {
        GraphicsCardInterface gfx = new GraphicsCardInterface((short)0, "/");
        int test = gfx.loadMemory(new IntArrayImage("/Capture.png"));
        int test2 = gfx.loadMemory(new IntArrayImage(300, 300));
        int test3 = gfx.loadMemory(new IntArrayImage(600, 600));
        gfx.runPlugin("FillColor", new Object[] {test2, 0xff00ff00});
        gfx.runPlugin("DrawLine", new Object[] {300, 0, 0, 300, 0xffff0000, test2});
        gfx.runPlugin("AdvRender", new Object[] {test2, test, 50, 50, 0, 0, 100, 100, 1f, 1f, false, false, 0, 0, 0, 0, 0, 0, 1f});
        //gfx.runPlugin("Render", new Object[] {test2, test, 50, 50, 0, 0, 100, 100, 1f, 1f, false, false, 1f});
        gfx.runPlugin("Upscale", new Object[] {test2, test3});
        gfx.runPlugin("DrawLine", new Object[] {0, 0, 600, 600, 0xffff0000, test3});
        gfx.showDebug();
        
        try {
        	Thread.sleep(10000);
        } catch (Exception e) {
        	
        }
        gfx.Release();
    }
}
