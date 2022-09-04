package Personal.jocltest;

import static org.jocl.CL.CL_MEM_COPY_HOST_PTR;
import static org.jocl.CL.CL_MEM_READ_ONLY;
import static org.jocl.CL.clCreateBuffer;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_context;
import org.jocl.cl_mem;

public class MemoryManager{
	
	private cl_mem memObjects[];
	private cl_context context;
	private HashMap <String, Integer> indexes = new HashMap<String, Integer>();
	private MemHelper[] helpers;
	private int err[] = new int[1];
	private final GraphicsCardInterface gfx;
	
	private JFrame frame = new JFrame("GFX Memory Viewer");
	private DefaultListModel<MemHelper> model = new DefaultListModel<MemHelper>();
	private JList<MemHelper> list = new JList<MemHelper>(model);
	private JFrame imgFrame = new JFrame("GFX Memory Viewer");
	private Canvas canvas = new Canvas();
	private BufferedImage image;
	
	public MemoryManager(cl_mem[] memObjects, cl_context context, final GraphicsCardInterface gfx) {
		this.memObjects = memObjects;
		this.context = context;
		helpers = new MemHelper[memObjects.length];
		this.gfx = gfx;
		
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		list.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				try {
					int frameX = imgFrame.getX();
					int frameY = imgFrame.getY();
					image = new BufferedImage(list.getSelectedValue().getWidth(), list.getSelectedValue().getHeight(), BufferedImage.TYPE_INT_ARGB);
					imgFrame.dispose();
					imgFrame = new JFrame("GFX Memory Viewer");
					imgFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
					gfx.readBuffer(list.getSelectedIndex(), image);
					JLabel label = new JLabel();
					label.setIcon(new ImageIcon(image));
					imgFrame.add(label);
					imgFrame.setLocation(frameX, frameY);
					imgFrame.setVisible(true);
					imgFrame.pack();
				} catch (NullPointerException ex) {
					System.out.println("GFX WARN: GFX memory debug viewer tried accessing invalid helper " + list.getSelectedIndex());
				} catch (IllegalStateException ex ) {
					System.out.println("GFX WARN: Error in GFX memory debug viewer trying to create window to display image stored in helper " + list.getSelectedIndex());
				}
			}
			
		});
		JScrollPane scroll = new JScrollPane(list);
		scroll.setVerticalScrollBar(scroll.createVerticalScrollBar());
		scroll.setPreferredSize(new Dimension(300, 700));
		JPanel panel = new JPanel();
		panel.add(scroll, "East");
		frame.add(panel);
		frame.pack();
		frame.setVisible(false);
		
		imgFrame.setVisible(true);
		imgFrame.setVisible(false);
		imgFrame.setLayout(new BorderLayout());
		imgFrame.add(canvas, BorderLayout.CENTER);
		//imgFrame.add(canvas);
		canvas.createBufferStrategy(3);
	}
	
	public int loadTexture(String path) {
		if(indexes.keySet().contains(path)) {//texture already loaded
			helpers[indexes.get(path)].stake();
			return indexes.get(path);
		} else {//texture not already loaded
			int i = 0;
			while (i < helpers.length && helpers[i] != null) //gets first open slot
				i++;
			BufferedImage image;
			try {
				image = ImageIO.read(getClass().getResourceAsStream(path));
				int[] texture = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
				Pointer texturePtr = Pointer.to(texture);
			
				memObjects[i] = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * (image.getWidth()*image.getHeight()), texturePtr, err);
				if (err[0] != org.jocl.CL.CL_SUCCESS) {
					System.out.println("GFX ERROR: Failed to allocate memory for texture '" + path + "': " + org.jocl.CL.stringFor_errorCode(err[0]));
					return -1;
				}
				MemHelper helper = new MemHelper(image.getWidth(), image.getHeight(), path);
				helpers[i] = helper;
				indexes.put(path, i);
				
				model.addElement(helper);
				
				return i;
			} catch (IOException e) {
				System.out.println("GFX ERROR: Failed to load Texture '" + path + "'");
				e.printStackTrace();
				return -1;
			} catch (IllegalArgumentException e) {
				System.out.println("GFX ERROR: Failed to load Texture '" + path + "', possibly misspelled filepath");
				e.printStackTrace();
				return -1;
			}
		}
	}
	
	public int unloadTexture(String path) {
		if (indexes.keySet().contains(path)) {
			int index = indexes.get(path);
			if (helpers[index].destake() > 0)//if other objects rely on texture
				return helpers[index].getStakes();
			else {//if nothing else relies on texture
				org.jocl.CL.clReleaseMemObject(memObjects[index]);
				
				model.removeElement(helpers[index]);
				//removeDefaultListModelElementWorkaround(helpers[index]);
				helpers[index] = null;
				indexes.remove(path);
				return 0;
			}
		} else {
			System.out.println("GFX ERROR: Attempted to remove texture '" + path + "' when it is not in memory");
			return 0;
		}
	}
	
	public int createCanvas(int w, int h) {
		int i = 0;
		while (i < helpers.length && helpers[i] != null) //gets first open slot
			i++;
		if (i == helpers.length) {
			System.out.println("GFX ERROR: Out of memory space to allocate new canvas");
			return -1;
		}
		int canvas[] = new int [w*h];
		Pointer ptr = Pointer.to(canvas);
		memObjects[i] = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * (w*h), ptr, err);
		if (err[0] != org.jocl.CL.CL_SUCCESS) {
			System.out.println("GFX ERROR: Failed to allocate memory for new canvas " + org.jocl.CL.stringFor_errorCode(err[0]));
			return -1;
		}
		MemHelper helper = new MemHelper(w, h, "canvas");
		helpers[i] = helper;
		
		model.addElement(helper);
		
		return i;
	}
	
	public void releaseCanvas(int i) {
		if (helpers[i] == null) {
			System.out.println("GFX ERROR: tried to release canvas in empty memory slot " + i);
			return;
		}
		org.jocl.CL.clReleaseMemObject(memObjects[i]);
		
		MemHelper toRemove = helpers[i];
		model.removeElement(toRemove);
		
		helpers[i] = null;
	}
	
	public MemHelper getHelper(int i) throws IllegalArgumentException {
		if (i < 0 || i >= helpers.length)
			throw new IllegalArgumentException("GFX ERROR: Tried to access invalid helper at index " + i + ", helper out of bounds");
		if (helpers[i] == null)
			throw new IllegalArgumentException("GFX ERROR: Tried to access invalid helper at index " + i + ", helper does not exist");
		
		return helpers[i];
	}
	
	public void releaseMem() {
		for(String s : indexes.keySet()) {
			org.jocl.CL.clReleaseMemObject(memObjects[indexes.get(s)]);
		}
		indexes.clear();
		frame.dispose();
		imgFrame.dispose();
	}
	
	public void showDebug() {
		frame.setVisible(true);
	}
	
	public void updateDebugImg(int index) {
		if (imgFrame.isVisible() && index == list.getSelectedIndex()) {
			try {
				int frameX = imgFrame.getX();
				int frameY = imgFrame.getY();
				image = new BufferedImage(list.getSelectedValue().getWidth(), list.getSelectedValue().getHeight(), BufferedImage.TYPE_INT_ARGB);
				imgFrame.dispose();
				imgFrame = new JFrame("GFX Memory Viewer");
				imgFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
				gfx.readBuffer(list.getSelectedIndex(), image);
				JLabel label = new JLabel();
				label.setIcon(new ImageIcon(image));
				imgFrame.add(label);
				imgFrame.setLocation(frameX, frameY);
				imgFrame.setVisible(true);
				imgFrame.pack();
			} catch (NullPointerException ex) {
				System.out.println("GFX WARN: GFX memory debug viewer tried accessing invalid helper " + list.getSelectedIndex());
			} catch (IllegalStateException ex ) {
				System.out.println("GFX WARN: Error in GFX memory debug viewer trying to create window to display image stored in helper " + list.getSelectedIndex());
			}
		}
	}
	
}

class MemHelper {
	private int width;
	private int height;
	private int stakes;
	private String designation;
	
	public MemHelper(int w, int h, String designation) {
		this.width = w;
		this.height = h;
		this.designation = designation;
		stakes = 1;
	}
	
	public int stake() {
		stakes++;
		return stakes;
	}
	
	public int destake() {
		stakes--;
		return stakes;
	}
	
	public int getStakes() {
		return stakes;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	@Override
	public String toString() {
		return "[" + stakes + "x][" + width + " x " + height + "] " + designation;
		//return "hi";
	}
}