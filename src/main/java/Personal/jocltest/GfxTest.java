package Personal.jocltest;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.jocl.Pointer;

public class GfxTest
{
    public static void main( String[] args )
    {

        int w = 1000;
        int h = 700;
        int pixels[] = new int[w * h];
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < w; i++)
        	for (int j = 0; j < h; j++) {
        		pixels[j*w + i] =0;
        	}

        GraphicsCardInterface gfx = new GraphicsCardInterface();
        TextHandler text = new TextHandler(gfx, 10, 10, "abcdefghijklmnopqrstuvwxyz .!?0123456789", "/generic_alphabet.png");
        int textureIndex = gfx.loadTexture("/Untitled.png");
        int textureIndexjunk = gfx.loadTexture("/Untitled.png");
        System.out.println(gfx.unloadTexture("/Untitled.png"));
        int textureIndex2 = gfx.loadTexture("/Capture.PNG");
        int srcCanvas = gfx.createCanvas(100, 100);
        int destCanvas = gfx.createCanvas(1000, 700);
        startTime=System.currentTimeMillis();
        gfx.fillColor(srcCanvas, 0xff0000ff);
        text.renderText(srcCanvas, "HEllo!1:", 0, 0, .5f);
        text.renderOffsetText(srcCanvas, "offset", 20, 20, .5f, 0, 2, 0, 3, 0, 4);
        gfx.render(srcCanvas, textureIndex, 20, 40, 0, 0, 20, 40, 1f, 1f, false, false, 0, 0, 0, 0, 0, 0, 3);
        gfx.render(srcCanvas, textureIndex2, 50, 40, 4, 4, 20, 40, 1f, 1f, false, false, 0, 0, 0, 0, 0, 0, 3);
        gfx.invert(srcCanvas, textureIndex2, 30, 40, 4, 4, 30, 40, 1f, 1f, false, false, 0, 0, 0, 0, 0, 0, 3);
        gfx.drawLine(srcCanvas, -2, 2, 50, 104, 0x5500ff00);
        gfx.upscale(srcCanvas, destCanvas);
        System.out.println("GPU computation Time: " + (System.currentTimeMillis() - startTime));
        startTime=System.currentTimeMillis();
//        gfx.readBuffer(destCanvas, pixels);
//        image.setRGB(0, 0, w, h, pixels, 0, w);
        gfx.readBuffer(destCanvas, image);
        System.out.println("GPU return Time: " + (System.currentTimeMillis() - startTime));
        
        startTime=System.currentTimeMillis();
        gfx.XXX(image, image, 0, 0);
        System.out.println("Test Function Time: " + (System.currentTimeMillis() - startTime));
        
        gfx.unloadTexture("/Untitled.png");
        gfx.unloadTexture("/Capture.PNG");
        gfx.releaseCanvas(destCanvas);
        gfx.releaseCanvas(srcCanvas);
        gfx.Release();

//        System.out.println(String.format("0x%08X", pixels[0]));
        
        try {
			ImageIO.write(image , "png", new File("test.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
        
    }
}
