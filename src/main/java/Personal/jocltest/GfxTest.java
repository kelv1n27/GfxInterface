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
        GraphicsCardInterface gfx = new GraphicsCardInterface();
        //TextHandler text = new TextHandler(gfx, 10, 10, "abcdefghijklmnopqrstuvwxyz .!?0123456789", "/generic_alphabet.png");
        gfx.loadTexture("/Capture.png");
        int test = gfx.createCanvas(300, 300);
        gfx.showDebug();
        try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        System.out.println("textures:");
        for (int i = 0; i < 1000; i++) {
        	gfx.loadTexture("/Untitled.png");
        	try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	gfx.unloadTexture("/Untitled.png");
        }
        gfx.fillColor(test, 0xffff0000);
        System.out.println("canvases:");
        int canvas;
        for (int i = 0; i < 1000; i++) {
        	canvas = gfx.createCanvas(1, 1);
        	try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	gfx.releaseCanvas(canvas);
        }
       
        System.out.println("done");
        try {
			Thread.sleep(10000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        gfx.Release();

//        System.out.println(String.format("0x%08X", pixels[0]));
        
//        try {
//			ImageIO.write(image , "png", new File("test.png"));
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
        
    }
}
