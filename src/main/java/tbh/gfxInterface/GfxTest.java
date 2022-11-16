package tbh.gfxInterface;

public class GfxTest
{
    public static void main( String[] args )
    { 
        GraphicsCardInterface gfx = new GraphicsCardInterface((short)1, "/");
        int test = gfx.loadMemory(gfx.buildMemoryObject("IntArrayImage", new Object[] {"/Capture.png"}));
        int oopstest = gfx.loadMemory(gfx.buildMemoryObject("IntArrayImage", new Object[] {"/generic_alphabet.png"}));
        int test2 = gfx.loadMemory(gfx.buildMemoryObject("IntArrayImage", new Object[] {300, 300}));
        int test3 = gfx.loadMemory(gfx.buildMemoryObject("IntArrayImage", new Object[] {600, 600}));
        gfx.runPlugin("FillColor", new Object[] {test2, 0xff00ff00});
        gfx.runPlugin("DrawLine", new Object[] {300, 0, 0, 300, 0xffff0000, test2});
        gfx.runPlugin("AdvRender", new Object[] {test2, test, 50, 50, 0, 0, 100, 100, 1f, 1f, false, false, 0, 0, 0, 0, 0, 0, 1f});
        //gfx.runPlugin("Render", new Object[] {test2, test, 50, 50, 0, 0, 100, 100, 1f, 1f, false, false, 1f});
        gfx.runPlugin("Upscale", new Object[] {test2, test3});
        gfx.runPlugin("DrawLine", new Object[] {0, 0, 600, 600, 0xffff0000, test3});
        
        int font = gfx.loadMemory(gfx.buildMemoryObject("BasicFont", new Object[] {"/generic_alphabet.png", 10, 10, "abcdefghijklmnopqrstuvwxyz .!?0123456789"}));
        gfx.runPlugin("RenderFont", new Object[] {test3, font, 100, 400, 4f, "hi there"});
        //System.out.println(MemoryPlugin.class.getSuperclass().getSuperclass().getSuperclass().getSuperclass().getSuperclass().getSuperclass().getSuperclass());
        gfx.showDebug();
        try {
        	Thread.sleep(10000);
        } catch (Exception e) {
        	
        }
        gfx.Release();
    }
}