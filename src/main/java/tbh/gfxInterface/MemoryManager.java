package tbh.gfxInterface;

import java.awt.Dimension;
import java.util.HashMap;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jocl.cl_context;
import org.jocl.cl_mem;

public class MemoryManager{
	
	private cl_mem memObjects[];
	private cl_context context;
	private HashMap <String, Integer> indexes = new HashMap<String, Integer>();
	//private MemHelper[] helpers;////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private int err[] = new int[1];
	private final GraphicsCardInterface gfx;
	
	private JFrame frame = new JFrame("GFX Memory Viewer");
	//private DefaultListModel<MemHelper> model = new DefaultListModel<MemHelper>();/////////////////////////////////////////////////////////////////
	//private JList<MemHelper> list = new JList<MemHelper>(model);///////////////////////////////////////////////////////////////////////////////////
	//private JFrame imgFrame = new JFrame("GFX Memory Viewer");/////////////////////////////////////////////////////////////////////////////////////
	//private Canvas canvas = new Canvas();//////////////////////////////////////////////////////////////////////////////////////////////////////////
	//private BufferedImage image;///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private DefaultListModel<MemoryPlugin> pmodel = new DefaultListModel<MemoryPlugin>();
	private JList<MemoryPlugin> plist = new JList<MemoryPlugin>(pmodel);
	private MemoryPlugin[] memoryHelpers;
	
	public MemoryManager(cl_mem[] memObjects, cl_context context, final GraphicsCardInterface gfx) {
		this.memObjects = memObjects;
		this.context = context;
		//helpers = new MemHelper[memObjects.length];////////////////////////////////////////////////////////////////////////////////////////////////
		memoryHelpers = new MemoryPlugin[memObjects.length];
		this.gfx = gfx;
		
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
//		list.addListSelectionListener(new ListSelectionListener() {// this whole listener thing
//
//			@Override
//			public void valueChanged(ListSelectionEvent e) {
//				try {
//					image = new BufferedImage(list.getSelectedValue().getWidth(), list.getSelectedValue().getHeight(), BufferedImage.TYPE_INT_ARGB);
//					gfx.readBuffer(list.getSelectedIndex(), image);
//					JLabel label = new JLabel();
//					label.setIcon(new ImageIcon(image));
//					imgFrame.getContentPane().removeAll();
//					imgFrame.add(label);
//					imgFrame.setVisible(true);
//					imgFrame.pack();
//				} catch (NullPointerException ex) {
//					gfx.GfxLog(1, "GFX memory debug viewer tried accessing invalid helper " + list.getSelectedIndex());
//				} catch (IllegalStateException ex ) {
//					gfx.GfxLog(1, "Error in GFX memory debug viewer trying to create window to display image stored in helper " + list.getSelectedIndex());
//				}
//			}
//			
//		});////////////////////////////////////////////////////////////////////////////cont. from above
		
		plist.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				try {
					updateMemoryPlugin(plist.getSelectedIndex());
					plist.getSelectedValue().setVisible(true);
				} catch (NullPointerException ex) {
					gfx.GfxLog(1, "GFX memory debug viewer tried accessing invalid memory plugin " + plist.getSelectedIndex());
					ex.printStackTrace();
				} catch (Exception ex) {
					gfx.GfxLog(1, "Unknown error opening debug window for memory plugin " + plist.getSelectedIndex());
					ex.printStackTrace();
				}
				
			}
		});
		
		//JScrollPane oldscroll = new JScrollPane(list);
		JScrollPane scroll = new JScrollPane(plist);
		scroll.setVerticalScrollBar(scroll.createVerticalScrollBar());
		scroll.setPreferredSize(new Dimension(300, 700));
		JPanel panel = new JPanel();
		panel.add(scroll, "East");
		frame.add(panel);
		frame.pack();
		frame.setVisible(false);
		
		//imgFrame.setVisible(true);////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//imgFrame.setVisible(false);///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//imgFrame.setLayout(new BorderLayout());///////////////////////////////////////////////////////////////////////////////////////////////////////
		//imgFrame.add(canvas, BorderLayout.CENTER);////////////////////////////////////////////////////////////////////////////////////////////////////
		//imgFrame.add(canvas);
		//canvas.createBufferStrategy(3);///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	}
	
	public int loadMemory(MemoryPlugin p) {
		String designation = p.getDefaultDesignation();
		int pass = 0;
		while(indexes.containsKey(designation) && !indexes.get(designation).getClass().equals(p.getClass()) && pass < 128) {//stop collisions on same key by different plugins
			pass++;
			if (pass > 1) {
				designation = designation.substring(0, designation.length()-1) + pass;
			} else {
				designation += pass;
			}
		}
		if (pass >= 128) {//all memory slots filled with colliding keys
			gfx.GfxLog(2, "Failure indexing new " + p.getClass());
			return -1;
		} else if (!indexes.containsKey(designation)){//plugin not already indexed
			int i = 0;
			while(i < memoryHelpers.length && memoryHelpers[i] != null)//get first open slot
				i++;
			try {
				p.addMemoryObject(i);
				p.setDesignation(designation);
				memoryHelpers[i] = p;
				indexes.put(designation, i);
				pmodel.addElement(p);
				return i;
			} catch (Exception e) {
				gfx.GfxLog(2, "Uncaught Exception trying to load new " + p.getClass());
				e.printStackTrace();
				return -1;
			}
		} else {//plugin already loaded
			memoryHelpers[indexes.get(designation)].stake();
			return indexes.get(designation);
		}
	}
	
	public int releaseMemory(int index) {
		try {
			int stakes = memoryHelpers[index].destake();
			if (stakes <= 0) {
				memoryHelpers[index].removeMemoryObject(index);
				//org.jocl.CL.clReleaseMemObject(memObjects[index]);
				pmodel.removeElement(memoryHelpers[index]);
				memoryHelpers[index] = null;
				for (String s : indexes.keySet()) {
					indexes.remove(s, index);
				}
			}
			return stakes;
		} catch (IndexOutOfBoundsException e) {
			gfx.GfxLog(2, "Tried to release memory at index out of bounds index" + index);
			return -1;
		} catch (NullPointerException e) {
			gfx.GfxLog(2, "Tried to release nonexistant memory at index " + index);
			return -1;
		}
	}
	
	public Object retrieveMemory(int index) {
		try {
			return memoryHelpers[index].retrieveMemoryObject(index);
		} catch (IndexOutOfBoundsException e) {
			gfx.GfxLog(2, "Tried to retrieve memory at out of bounds index " + index);
			return null;
		} catch (NullPointerException e) {
			gfx.GfxLog(2, "Tried to retrieve nonexistant memory at index " + index);
			return null;
		}
	}
	
	public MemoryPlugin getMemoryPlugin(int index) {
		return memoryHelpers[index];
	}
	
	public void updateMemoryPlugin(int index) {
		gfx.GfxLog(0, "Updating debug window of plugin " + index);
		memoryHelpers[index].updateDebug(index);
	}
	
	public void showDebug() {
		frame.setVisible(true);
	}
	
	public void releaseMem() {
		for(String s : indexes.keySet()) {
			org.jocl.CL.clReleaseMemObject(memObjects[indexes.get(s)]);
		}
		for(MemoryPlugin p : memoryHelpers)
			if (p != null)
				p.dispose();
		indexes.clear();
		frame.dispose();
		//imgFrame.dispose();
	}
	
	// everything below here gets canned
	
//	public int loadTexture(String path) {
//		if(indexes.keySet().contains(path)) {//texture already loaded
//			for (String s : indexes.keySet())
//				System.out.println(s);
//			helpers[indexes.get(path)].stake();
//			return indexes.get(path);
//		} else {//texture not already loaded
//			int i = 0;
//			while (i < helpers.length && helpers[i] != null) //gets first open slot
//				i++;
//			BufferedImage image;
//			try {
//				image = ImageIO.read(getClass().getResourceAsStream(path));
//				int[] texture = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
//				Pointer texturePtr = Pointer.to(texture);
//			
//				memObjects[i] = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * (image.getWidth()*image.getHeight()), texturePtr, err);
//				if (err[0] != org.jocl.CL.CL_SUCCESS) {
//					gfx.GfxLog(2, "Failed to allocate memory for texture '" + path + "': " + org.jocl.CL.stringFor_errorCode(err[0]));
//					return -1;
//				}
//				MemHelper helper = new MemHelper(image.getWidth(), image.getHeight(), path);
//				helpers[i] = helper;
//				indexes.put(path, i);
//				
//				model.addElement(helper);
//				
//				return i;
//			} catch (IOException e) {
//				gfx.GfxLog(2, "Failed to load Texture '" + path + "'");
//				e.printStackTrace();
//				return -1;
//			} catch (IllegalArgumentException e) {
//				gfx.GfxLog(2, "Failed to load Texture '" + path + "', possibly misspelled filepath");
//				e.printStackTrace();
//				return -1;
//			}
//		}
//	}
//	
//	public int unloadTexture(String path) {
//		if (indexes.keySet().contains(path)) {
//			int index = indexes.get(path);
//			if (helpers[index].destake() > 0)//if other objects rely on texture
//				return helpers[index].getStakes();
//			else {//if nothing else relies on texture
//				org.jocl.CL.clReleaseMemObject(memObjects[index]);
//				
//				model.removeElement(helpers[index]);
//				//removeDefaultListModelElementWorkaround(helpers[index]);
//				helpers[index] = null;
//				indexes.remove(path);
//				return 0;
//			}
//		} else {
//			gfx.GfxLog(2, "Attempted to remove texture '" + path + "' when it is not in memory");
//			return 0;
//		}
//	}
//	
//	public int createCanvas(int w, int h) {
//		int i = 0;
//		while (i < helpers.length && helpers[i] != null) //gets first open slot
//			i++;
//		if (i == helpers.length) {
//			gfx.GfxLog(2, "Out of memory space to allocate new canvas");
//			return -1;
//		}
//		int canvas[] = new int [w*h];
//		Pointer ptr = Pointer.to(canvas);
//		memObjects[i] = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * (w*h), ptr, err);
//		if (err[0] != org.jocl.CL.CL_SUCCESS) {
//			gfx.GfxLog(2, "Failed to allocate memory for new canvas " + org.jocl.CL.stringFor_errorCode(err[0]));
//			return -1;
//		}
//		MemHelper helper = new MemHelper(w, h, "canvas");
//		helpers[i] = helper;
//		
//		model.addElement(helper);
//		
//		return i;
//	}
//	
//	public void releaseCanvas(int i) {
//		if (helpers[i] == null) {
//			gfx.GfxLog(2, "tried to release canvas in empty memory slot " + i);
//			return;
//		}
//		org.jocl.CL.clReleaseMemObject(memObjects[i]);
//		
//		MemHelper toRemove = helpers[i];
//		model.removeElement(toRemove);
//		
//		helpers[i] = null;
//	}
//	
//	

//	
//	
//	public MemHelper getHelper(int i) throws IllegalArgumentException {
//		if (i < 0 || i >= helpers.length) 
//			throw new IllegalArgumentException("Tried to access invalid helper at index " + i + ", helper out of bounds");
//		if (helpers[i] == null) 
//			throw new IllegalArgumentException("Tried to access invalid helper at index " + i + ", helper does not exist");
//		return helpers[i];
//	}
//	public void updateDebugImg(int index) {
//		if (index == list.getSelectedIndex()) {
//			try {
//				image = new BufferedImage(list.getSelectedValue().getWidth(), list.getSelectedValue().getHeight(), BufferedImage.TYPE_INT_ARGB);
//				gfx.readBuffer(list.getSelectedIndex(), image);
//				JLabel label = new JLabel();
//				label.setIcon(new ImageIcon(image));
//				imgFrame.getContentPane().removeAll();
//				imgFrame.add(label);
//				imgFrame.setVisible(true);
//				imgFrame.pack();
//			} catch (NullPointerException ex) {
//				gfx.GfxLog(1, "GFX memory debug viewer tried accessing invalid helper " + list.getSelectedIndex());
//			} catch (IllegalStateException ex ) {
//				gfx.GfxLog(1, "Error in GFX memory debug viewer trying to create window to display image stored in helper " + list.getSelectedIndex());
//			}
//		}
//	}
//	
//}
//
//class MemHelper {
//	private int width;
//	private int height;
//	private int stakes;
//	private String designation;
//	
//	public MemHelper(int w, int h, String designation) {
//		this.width = w;
//		this.height = h;
//		this.designation = designation;
//		stakes = 1;
//	}
//	
//	public int stake() {
//		stakes++;
//		return stakes;
//	}
//	
//	public int destake() {
//		stakes--;
//		return stakes;
//	}
//	
//	public int getStakes() {
//		return stakes;
//	}
//	
//	public int getWidth() {
//		return width;
//	}
//	
//	public int getHeight() {
//		return height;
//	}
//	
//	@Override
//	public String toString() {
//		return "[" + stakes + "x][" + width + " x " + height + "] " + designation;
//		//return "hi";
//	}
}