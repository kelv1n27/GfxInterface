package tbh.gfxInterface;

import static org.jocl.CL.CL_CONTEXT_PLATFORM;
import static org.jocl.CL.CL_DEVICE_TYPE_ALL;
import static org.jocl.CL.clCreateCommandQueue;
import static org.jocl.CL.clCreateContext;
import static org.jocl.CL.clGetDeviceIDs;
import static org.jocl.CL.clGetPlatformIDs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.jar.JarFile;

import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;

public class GraphicsCardInterface {

	// The platform, device type and device number that will be used
    private final int platformIndex = 0;
    private final long deviceType = CL_DEVICE_TYPE_ALL;
    private final int deviceIndex = 0;
    // Obtain the number of platforms
    private int numPlatformsArray[] = new int[1];
    private int numPlatforms;
    // Obtain a platform ID
    private cl_platform_id platforms[];
    private cl_platform_id platform;
    // Obtain the number of devices for the platform
    private int numDevicesArray[];
    private int numDevices;
    // Obtain a device ID
    private cl_device_id devices[];
    private cl_device_id device;
    // Initialize the context properties
    private cl_context_properties contextProperties;
    // Create a context for the selected device
    private cl_context context;
    // Create a command-queue for the selected device
    private cl_command_queue commandQueue;
    //allocated memory for gpu
    cl_mem memObjects[];
        
    private MemoryManager memHandler;
    
    private String pluginLocation = "/";
    private HashMap<String, KernelPlugin> programs = new HashMap<String, KernelPlugin>();
    
    private short Gfx_Log_Level = 1;
    public final short GFX_LOG_LEVEL_LOG = 0;
    public final short GFX_LOG_LEVEL_WARN = 1;
    public final short GFX_LOG_LEVEL_ERR = 2;
    public final short GFX_LOG_LEVEL_OFF = 3;

    private cl_program gfxProg;
    private cl_kernel invertKern;

	public GraphicsCardInterface() {
		initOpenCL();	
	    memHandler = new MemoryManager(memObjects, context, this);
	    loadPlugins(pluginLocation);
	}
	
	public GraphicsCardInterface(short logLevel, String pluginDir) {
		Gfx_Log_Level = logLevel;
		initOpenCL();	        
		memHandler = new MemoryManager(memObjects, context, this);
		pluginLocation = pluginDir;
		loadPlugins(pluginLocation);
	}
	
	private void initOpenCL() {
		// Obtain the number of platforms
		clGetPlatformIDs(0, null, numPlatformsArray);
		numPlatforms = numPlatformsArray[0];

		// Obtain a platform ID
		platforms = new cl_platform_id[numPlatforms];
		clGetPlatformIDs(platforms.length, platforms, null);
		platform = platforms[platformIndex];

		// Obtain the number of devices for the platform
		numDevicesArray = new int[1];
		clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
		numDevices = numDevicesArray[0];

		// Obtain a device ID
		devices = new cl_device_id[numDevices];
		clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
		device = devices[deviceIndex];

		// Initialize the context properties
		contextProperties = new cl_context_properties();
		contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

		// Create a context for the selected device
		context = clCreateContext(contextProperties, 1, new cl_device_id[]{device}, null, null, null);

		// Create a command-queue for the selected device
		commandQueue = clCreateCommandQueue(context, device, 0, null);

		//allocate 128 spaces in memory for sprites
		memObjects = new cl_mem[128];
	}
	
	public void loadPlugins(String pluginDir) {
		MemoryPlugin.load(this);
		try {
	    	//get all the .jar files
			File pluginFolder = new File(getClass().getResource(pluginLocation).toURI());
			if (!pluginFolder.exists())
				throw new FileNotFoundException();
			File[] files=pluginFolder.listFiles((dir, name) -> name.endsWith(".jar"));
			//get all the classes and kernel programs in the .jar files
			ArrayList<URL> urls = new ArrayList<URL>();
			ArrayList<String> classes = new ArrayList<String>();
			ArrayList<String> kernels = new ArrayList<String>();
			for(File f : files) {
				try {
					JarFile jarfile = new JarFile(f);
					urls.add(new URL("jar:file:"+pluginFolder + "/" + f.getName() + "!/"));
					jarfile.stream().forEach(jarEntry -> {
                        if(jarEntry.getName().endsWith(".class")){
                        	GfxLog(0, "found class \"" + jarEntry.getName() + "\"");
                            classes.add(jarEntry.getName());
                        }
                        if(jarEntry.getName().endsWith(".kernel")){
                        	GfxLog(0, "found kernel \"" + jarEntry.getName() + "\"");
                            kernels.add(jarEntry.getName());
                        }
                    });
					//load the classes and programs
					URLClassLoader pluginLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]));
					for (String c : classes) {
						try {
							String modString = c.replaceAll("/", ".").replace(".class", "");
							Class newClass = pluginLoader.loadClass(modString);
							if (newClass.getSuperclass() == KernelPlugin.class) {
								//kernel plugin, load kernel and all that
								String progName = modString.split("\\.")[modString.split("\\.").length-1];
								String match = null;
								for (String k : kernels) {
									if (k.replaceAll(".kernel", "").equals(progName))
										match = k;
								}
								if (match == null) {
									GfxLog(2, "Did not find matching kernel for \"" + modString + "\"");
									break;
								}
								//kernel-class match found, read kernel and try to create program
								try {
									InputStream in = jarfile.getInputStream(jarfile.getEntry(match));
									BufferedReader br;
									br = new BufferedReader(new InputStreamReader(in));
									String line = "";
									String code = "";
									while ((line = br.readLine()) != null) {
										code += line+"\n";
									}
									code = code.substring(0, code.length()-1);
									br.close();
									gfxProg = org.jocl.CL.clCreateProgramWithSource(context, 1, new String[] {code}, null, null);
									org.jocl.CL.clBuildProgram(gfxProg, 0, null, null, null, null);
									int[] err = new int[1];
									cl_kernel newKernel = org.jocl.CL.clCreateKernel(gfxProg, match.replaceAll(".kernel", ""), err);
									if (err[0] != org.jocl.CL.CL_SUCCESS) throw new Exception("Kernel err = " + org.jocl.CL.stringFor_errorCode(err[0]));
									//kernel creation was successful, load class
									KernelPlugin newProg = (KernelPlugin) newClass.newInstance();
									newProg.load(this, newKernel);
									//index the class into the programs hashmap
									boolean added = false;
									String key = match.replaceAll(".kernel", "");
									int pass = 0;
									while(!added || pass > 100) {
										boolean collision = false;
										for (String s : programs.keySet()) {
											if ((key + (pass == 0 ? "" : pass)).equals(s)){
												GfxLog(1, "Collision indexing class \"" + key + (pass++ == 0 ? "" : pass) + "\", trying again with key \"" + key + pass + "\"");
											}
										}
										if (!collision) {
											programs.put(key, newProg);
											added = true;
										}
										if (pass > 100 && !added) GfxLog(1, "Failed to index class \"" + key + "\" in 100 attempts, aborting load");
									}
									GfxLog(0, "Kernel plugin \"" + match.replaceAll(".kernel", "") + "\" loaded");
								} catch (Exception e) {
									GfxLog(2, "Could not read kernel \"" + match + "\"");
									e.printStackTrace();
									break;
								}
							} else if (newClass.getSuperclass() == MemoryPlugin.class){
								GfxLog(0, "Memory plugin \"" + modString + "\" loaded");
							}
							
						} catch (ClassNotFoundException | NoClassDefFoundError e) {
							GfxLog(2, "Failure loading class \"" + c + "\"");
							e.printStackTrace();
						}
					}
				} catch (IOException e) {
					GfxLog(2, "Could not read \"" + f + "\"");
					e.printStackTrace();
				}
				
			}
		} catch (URISyntaxException e) {
			GfxLog(2, "URI syntax error loading graphics plugins at \"" + pluginLocation + "\"");
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			GfxLog(2, "Plugin location \"" + pluginLocation + "\" not found");
			e.printStackTrace();
		}
	}
	
	public cl_command_queue getCommandQueue() {
		return commandQueue;
	}
	
	public cl_context getContext() {
		return context;
	}
	
	public void runPlugin(String pluginKey, Object[] args) {
		GfxLog(0, "Running plugin \"" + pluginKey + "\"");
		if (programs.get(pluginKey) != null)
			try {
				programs.get(pluginKey).run(args);
			} catch (Exception e) {
				GfxLog(2, "Uncaught exception in plugin \"" + pluginKey + "\"");
				e.printStackTrace();
			}
		else
			GfxLog(2, "Plugin \"" + pluginKey + "\" called but not loaded");
	}
	
	public void Release() {
		org.jocl.CL.clReleaseKernel(invertKern);
		
		for(KernelPlugin p : programs.values())
			p.release();
		programs.clear();
			
		org.jocl.CL.clReleaseProgram(gfxProg);
		org.jocl.CL.clReleaseCommandQueue(commandQueue);
		org.jocl.CL.clReleaseContext(context);
		memHandler.releaseMem();
		memHandler = null;
	}
		
	public void showDebug() {
		memHandler.showDebug();
	}
	
	public void GfxLog(int logLevel, String msg) {
		if (logLevel >= Gfx_Log_Level)
			switch (logLevel) {
			case GFX_LOG_LEVEL_OFF:
				break;
			case GFX_LOG_LEVEL_ERR:
				System.out.println("GFX ERR: " + msg);
				break;
			case GFX_LOG_LEVEL_WARN:
				System.out.println("GFX WARN: " + msg);
				break;
			case GFX_LOG_LEVEL_LOG:
				System.out.println("GFX LOG: " + msg);
				break;
			default:
				System.out.println("GFX UNCATEGORIZED: " + msg);
			}
	}
	
	public cl_mem getMemoryObject(int index) throws IllegalArgumentException{
		if (index < 0 || index >= memObjects.length)
			throw new IllegalArgumentException("Tried to access invalid memory object at index " + index + ", object out of bounds");
		if (memObjects[index] == null)
			throw new IllegalArgumentException("Tried to access invalid memory object at index " + index + ", object does not exist");
		return memObjects[index];
	}
		
	public cl_mem[] getMemObjects() {
		return memObjects;
	}
	
	public int loadMemory(MemoryPlugin p) {
		return memHandler.loadMemory(p);
	}
	
	public int releaseMemory(int index) {
		return memHandler.releaseMemory(index);
	}
	
	public Object retrieveMemory(int index) {
		return memHandler.retrieveMemory(index);
	}
	
	public MemoryPlugin getMemoryPlugin(int index) {
		return memHandler.getMemoryPlugin(index);
	}
	
	public void updateResource(int index) {
		memHandler.updateMemoryPlugin(index);
	}
	
	
		
//	public int loadTexture(String path) {
//		return memHandler.loadTexture(path);
//	}
//		
//	public int unloadTexture(String path) {
//		return memHandler.unloadTexture(path);
//	}
//	public int createCanvas(int w, int h) {
//		return memHandler.createCanvas(w, h);
//	}
//		
//	public void releaseCanvas(int i) {
//		memHandler.releaseCanvas(i);
//	}
//	
//	public boolean resourceExists(int index) {
//		return memHandler.getHelper(index) != null;
//	}
//	
//	
//	
//	public int getResourceWidth(int index) {
//		return memHandler.getHelper(index).getWidth();
//	}
//	
//	public int getResourceHeight(int index) {
//		return memHandler.getHelper(index).getHeight();
//	}
//	
//	
//
//	
//		
//	public void readBuffer(int src, BufferedImage dest) {
//		DataBufferInt output_buffer = (DataBufferInt) dest.getRaster().getDataBuffer();
//	    int output_data[] = output_buffer.getData();
//	    int err = clEnqueueReadBuffer(commandQueue, memObjects[src], CL_TRUE, 0, Sizeof.cl_int * (memHandler.getHelper(src).getWidth()*memHandler.getHelper(src).getHeight()), Pointer.to(output_data), 0, null, null);
//	    if (err != org.jocl.CL.CL_SUCCESS) GfxLog(2, "Readbuffer Failed: " + org.jocl.CL.stringFor_errorCode(err));
//	}

}