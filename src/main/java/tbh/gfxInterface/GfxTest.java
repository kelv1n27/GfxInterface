package tbh.gfxInterface;
import java.awt.image.BufferedImage;

public class GfxTest
{
    public static void main( String[] args )
    { 
        GraphicsCardInterface gfx = new GraphicsCardInterface();
        gfx.showDebug();

        int output = gfx.loadMemory(gfx.buildMemoryObject("IntArrayImage", new Object[] {300, 300}));
        int output2 = gfx.loadMemory(gfx.buildMemoryObject("IntArrayImage", new Object[] {300, 300}));
        int texture = gfx.loadMemory(gfx.buildMemoryObject("IntArrayImage", new Object[] {"/img.png"}));
        int norms = gfx.loadMemory(gfx.buildMemoryObject("IntArrayImage", new Object[] {"/norms.png"}));
        int norms2 = gfx.loadMemory(gfx.buildMemoryObject("IntArrayImage", new Object[] {"/norms2.png"}));
        gfx.runPlugin("FillColor", new Object[] {output, 0xff000000});
        //gfx.runPlugin("RenderLight", new Object[] {output, texture, norms, 10, 10, 400, 0xffffff, 0f, 90f, 1f, 1f});
        gfx.runPlugin("RenderLight", new Object[] {output, texture, norms, 10, 10, 400, 0xffffff, 45f, 45f, 0f, 2f, 0f, 1f, 1f, 1f});

        for (int i = 0; i < 10000; i++) {
        	gfx.runPlugin("FillColor", new Object[] {output, 0xff000000});
        	gfx.runPlugin("RenderLight", new Object[] {output, texture, norms, 10, 10, 400, 0xffffff, 45f, 45f, (((float)i)%40)/40, 2f, 0f, 1f, 1f, 1f});
        	//gfx.runPlugin("RenderLight", new Object[] {output, texture, norms, 150 + (int)(Math.sin(i * 0.01745)* 130) , 150 + (int)(Math.cos(i * 0.01745)* 130), 100, 0xffffff, 0f, 90f, 0f, 1f, 0f, 0f, 0f, 1f});
        	//gfx.runPlugin("RenderLight", new Object[] {output, texture, norms, 150 + (int)(Math.sin((i + 180) * 0.01745 )* 130) , 150 + (int)(Math.cos((i + 180) * 0.01745)* 130), 400, 0xffffff, 180f, 360f, 0f, 2f, 0f, 1f, 1f, 1f});
        	//gfx.runPlugin("FillColor", new Object[] {output2, 0xff000000});
        	//gfx.runPlugin("RenderLight", new Object[] {output2, texture, norms2, 150 + (int)(Math.sin(i * 0.01745)* 130) , 150 + (int)(Math.cos(i * 0.01745)* 130), 400, 0xffffff, 0f, 360f, 1f, 0f});
        	//gfx.runPlugin("RenderLight", new Object[] {output2, texture, norms, 150 + (int)(Math.sin((i + 180) * 0.01745 )* 130) , 150 + (int)(Math.cos((i + 180) * 0.01745)* 130), 400, 0xffffff, 180f, 360f, 1f, 0f});
        	try {
        		Thread.sleep(100);
        	} catch (Exception e) {
          	
        	}
        }
        
        try {
        	Thread.sleep(10000);
        } catch (Exception e) {
        	
        }
        gfx.Release();
    }
}
