package tbh.gfxInterface;

public class GfxTest
{
    public static void main( String[] args )
    { 
        GraphicsCardInterface gfx = new GraphicsCardInterface((short)1, "/");
//        int test = gfx.loadMemory(gfx.buildMemoryObject("IntArrayImage", new Object[] {"/Capture.png"}));
//        int oopstest = gfx.loadMemory(gfx.buildMemoryObject("IntArrayImage", new Object[] {"/generic_alphabet.png"}));
        int test2 = gfx.loadMemory(gfx.buildMemoryObject("IntArrayImage", new Object[] {300, 300}));
//        int test3 = gfx.loadMemory(gfx.buildMemoryObject("IntArrayImage", new Object[] {600, 600}));
        gfx.runPlugin("FillColor", new Object[] {test2, 0xff000000});
        gfx.runPlugin("RenderLight", new Object[] {test2, 150, 150, 150, 0xffffff, 10f, 180f, 1f});
//        gfx.runPlugin("DrawLine", new Object[] {300, 0, 0, 300, 0xffff0000, test2});
//        gfx.runPlugin("AdvRender", new Object[] {test2, test, 50, 50, 0, 0, 100, 100, 1f, 1f, false, false, 0, 0, 0, 0, 0, 0, 1f});
//        //gfx.runPlugin("Render", new Object[] {test2, test, 50, 50, 0, 0, 100, 100, 1f, 1f, false, false, 1f});
//        gfx.runPlugin("Upscale", new Object[] {test2, test3});
//        gfx.runPlugin("DrawLine", new Object[] {0, 0, 600, 600, 0xffff0000, test3});

        gfx.showDebug();
        
//        float[][] prods = new float[25][25];
//        for (int a = -12; a < 12; a++) {
//        	for (int b = -12; b < 12; b++) {
//        		float dist = (float) Math.sqrt((float)((a * a) + (b * b)));
//        		float distVecX = a/dist;
//        		float distVecY = b/dist;
//        		if (Math.sqrt((distVecX * distVecX) + (distVecY * distVecY)) >= 1.000001 || Math.sqrt((distVecX * distVecX) + (distVecY * distVecY)) <= 0.99999)
//        			System.out.println("Bad dist vector for a= " + a + ", b= " + b + ", vec= " + Math.sqrt((distVecX * distVecX) + (distVecY * distVecY)));
//        		float dirVecX = (float) Math.sin(270 * 0.01745f);
//        		float dirVecY = (float) Math.cos(270 * 0.01745f);
//        		if (Math.sqrt((dirVecX * dirVecX) + (dirVecY * dirVecY)) >= 1.000001 || Math.sqrt((dirVecX * dirVecX) + (dirVecY * dirVecY)) <= 0.99999)
//        			System.out.println("Bad dist vector for a= " + a + ", b= " + b + ", vec= " + Math.sqrt((distVecX * distVecX) + (distVecY * distVecY)));
//        		float dotProd = (distVecX * dirVecX) + (distVecY * dirVecY);
//        		if (dotProd > 1|| dotProd < -1)
//        			System.out.println("bad dot prod for a= " + a + ", b= " + b + ", prod= " + dotProd);
//        		prods[b+12][a+12] = Math.max(0, dotProd);
//        	}
//        }
//        
//        for (int x = 0; x < 24; x++) {
//        	for (int y = 0; y < 24; y++) {
//        		System.out.print(prods[x][y] + " ");
//        	}
//        	System.out.println();
//        }

        for (int i = 0; i < 10000; i++) {
        	gfx.runPlugin("FillColor", new Object[] {test2, 0xff000000});
        	gfx.runPlugin("RenderLight", new Object[] {test2, 150, 150, 190, 0xffffff, 45f, -90f, 1f});
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
