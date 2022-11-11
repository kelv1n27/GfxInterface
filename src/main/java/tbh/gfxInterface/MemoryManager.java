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
	private int err[] = new int[1];
	private final GraphicsCardInterface gfx;
	
	private JFrame frame = new JFrame("GFX Memory Viewer");
	
	private DefaultListModel<MemoryPlugin> pmodel = new DefaultListModel<MemoryPlugin>();
	private JList<MemoryPlugin> plist = new JList<MemoryPlugin>(pmodel);
	private MemoryPlugin[] memoryHelpers;
	
	public MemoryManager(cl_mem[] memObjects, cl_context context, final GraphicsCardInterface gfx) {
		this.memObjects = memObjects;
		this.context = context;
		memoryHelpers = new MemoryPlugin[memObjects.length];
		this.gfx = gfx;
		
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		
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
		
		JScrollPane scroll = new JScrollPane(plist);
		scroll.setVerticalScrollBar(scroll.createVerticalScrollBar());
		scroll.setPreferredSize(new Dimension(400, 700));
		JPanel panel = new JPanel();
		panel.add(scroll, "East");
		frame.add(panel);
		frame.pack();
		frame.setVisible(false);
		
	}
	
	public int loadMemory(MemoryPlugin p) {
		String designation = p.getDefaultDesignation();
		int pass = 0;
		while(indexes.containsKey(designation) && (p.forceCollision || !memoryHelpers[indexes.get(designation)].getClass().equals(p.getClass()) ) && pass < 128) {//stop collisions on same key by different plugins
			pass++;
			System.out.println(memoryHelpers[indexes.get(designation)].getClass() + ":" + p.getClass());
			gfx.GfxLog(1, "Collision indexing memory plugin with key \"" + designation + "\", trying again");
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
	}
	
}