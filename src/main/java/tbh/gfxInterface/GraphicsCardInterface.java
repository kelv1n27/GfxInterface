package tbh.gfxInterface;

import static org.jocl.CL.CL_CONTEXT_PLATFORM;
import static org.jocl.CL.CL_DEVICE_TYPE_ALL;
import static org.jocl.CL.clCreateCommandQueue;
import static org.jocl.CL.clCreateContext;
import static org.jocl.CL.clGetDeviceIDs;
import static org.jocl.CL.clGetPlatformIDs;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jocl.Pointer;
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
    
    private ByteClassLoader bcl;
        
    private HashMap <String, Integer> indexes = new HashMap<String, Integer>();
    private JFrame frame = new JFrame("GFX Memory Viewer");
	private DefaultListModel<MemoryPlugin> pmodel = new DefaultListModel<MemoryPlugin>();
	private JList<MemoryPlugin> plist = new JList<MemoryPlugin>(pmodel);
	private MemoryPlugin[] memoryHelpers;
    
    private String pluginLocation = "/";
    private HashMap<String, RunnablePlugin> runnablePlugins = new HashMap<String, RunnablePlugin>();
    private HashMap<String, Class> memoryPlugins = new HashMap<String, Class>();
    
    private short Gfx_Log_Level = 1;
    public final short GFX_LOG_LEVEL_LOG = 0;
    public final short GFX_LOG_LEVEL_WARN = 1;
    public final short GFX_LOG_LEVEL_ERR = 2;
    public final short GFX_LOG_LEVEL_OFF = 3;

    private cl_program gfxProg;
    private cl_kernel invertKern;

	public GraphicsCardInterface() {
		initOpenCL();	
	    loadPlugins(getClass().getProtectionDomain().getCodeSource().getLocation().toString(), pluginLocation);
	    initDebugWindow();
	}
	
	public GraphicsCardInterface(short logLevel, String runningDir, String pluginDir) {
		Gfx_Log_Level = logLevel;
		initOpenCL();	        
		pluginLocation = pluginDir;
		loadPlugins(runningDir, pluginLocation);
		initDebugWindow();
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
		memoryHelpers = new MemoryPlugin[memObjects.length];
	}
	
	public void loadPlugins(String runningDir, String pluginDir) {
		
		MemoryPlugin.load(this);
		RunnablePlugin.load(this);
		try {
			//check running location to determine how to load plugins
			File runningLocation = new File(new URI(runningDir));
			if (runningLocation.isDirectory()) {//if running location is a directory, running from IDE
				GfxLog(0, "Loading plugins in directory " + runningLocation.getName());
				File pluginLocation = new File(new URI(runningDir + pluginDir));
				if (!pluginLocation.exists()) {
					GfxLog(2, "Error opening plugin location + " + pluginLocation);
					throw new FileNotFoundException();
				}
				//get all .jar files in plugin location
				File[] files=pluginLocation.listFiles((dir, name) -> name.endsWith(".jar"));
				ArrayList<URL> urls = new ArrayList<URL>();
				ArrayList<JarFile> jars = new ArrayList<JarFile>();
				HashMap<JarFile, ArrayList<String>> classes = new HashMap<JarFile, ArrayList<String>>();
				HashMap<JarFile, ArrayList<String>> kernels = new HashMap<JarFile, ArrayList<String>>();
				//list all class files and kernels in the jars
				for(File f : files) {
					try {
						JarFile jarfile = new JarFile(f);
						classes.put(jarfile, new ArrayList<String>());
						kernels.put(jarfile, new ArrayList<String>());
						jars.add(jarfile);
						GfxLog(0, "Loading classes in jarfile " + f);
						urls.add(new URL("jar:file:"+pluginLocation + "/" + f.getName() + "!/"));
						jarfile.stream().forEach(jarEntry -> {
	                        if(jarEntry.getName().endsWith(".class")){
	                        	GfxLog(0, "found class \"" + jarEntry.getName() + "\"");
	                            classes.get(jarfile).add(jarEntry.getName());
	                        }
	                        if(jarEntry.getName().endsWith(".kernel")){
	                        	GfxLog(0, "found kernel \"" + jarEntry.getName() + "\"");
	                        	kernels.get(jarfile).add(jarEntry.getName());
	                        }
	                    });
					} catch (IOException e) {
						GfxLog(2, "Could not read \"" + f + "\"");
						e.printStackTrace();
					}
				}
				//load the classes and programs
				URLClassLoader pluginLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]));
				for(JarFile j : jars) {
					
					for (String c : classes.get(j)) {
						try {
							String modString = c.replaceAll("/", ".").replace(".class", "");
							Class newClass = pluginLoader.loadClass(modString);
							//find relevant parent class
							Class parent = newClass.getSuperclass();
							while(parent != KernelPlugin.class && parent != MemoryPlugin.class && parent != RunnablePlugin.class && parent != Object.class)
								parent = parent.getSuperclass();
							if (parent == KernelPlugin.class) {//index kernel plugins
								String progName = modString.split("\\.")[modString.split("\\.").length-1];
								String match = null;
								for (String k : kernels.get(j)) {
									if (k.replaceAll(".kernel", "").equals(progName))
										match = k;
								}
								if (match == null) {
									GfxLog(2, "Did not find matching kernel for \"" + modString + "\"");
									break;
								}
								//kernel-class match found, read kernel and try to create program
								try {
									//System.out.println(jarfile == null);
									InputStream in = j.getInputStream(j.getEntry(match));
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
									byte[] buildLog = new byte[2048];
									Pointer logPtr = Pointer.to(buildLog);
									int builderr = org.jocl.CL.clGetProgramBuildInfo(gfxProg, device, org.jocl.CL.CL_PROGRAM_BUILD_LOG, 2048, logPtr, null);
									if (err[0] != org.jocl.CL.CL_SUCCESS) throw new Exception("Kernel err = " + org.jocl.CL.stringFor_errorCode(err[0]) + new String(buildLog));
									//kernel creation was successful, load class
									KernelPlugin newProg = (KernelPlugin) newClass.newInstance();
									newProg.kernelLoad(newKernel);
									//index the class into the programs hashmap
									boolean added = false;
									String key = match.replaceAll(".kernel", "");
									int pass = 0;
									while(!added || pass > 100) {
										boolean collision = false;
										for (String s : runnablePlugins.keySet()) {
											if ((key + (pass == 0 ? "" : pass)).equals(s)){
												GfxLog(1, "Collision indexing class \"" + key + (pass++ == 0 ? "" : pass) + "\", trying again with key \"" + key + pass + "\"");
											}
										}
										if (!collision) {
											runnablePlugins.put(key, newProg);
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
							} else if (parent == MemoryPlugin.class){//index memory plugins
								boolean added = false;
								String key = modString.replaceAll("[a-zA-Z0-9\\-_]*\\.", "");
								int pass = 0;
								while(!added || pass > 100) {
									boolean collision = false;
									for (String s : memoryPlugins.keySet()) {
										if ((key + (pass == 0 ? "" : pass)).equals(s)){
											GfxLog(1, "Collision indexing class \"" + key + (pass++ == 0 ? "" : pass) + "\", trying again with key \"" + key + pass + "\"");
										}
									}
									if (!collision) {
										memoryPlugins.put(key, newClass);
										added = true;
									}
									if (pass > 100 && !added) GfxLog(1, "Failed to index class \"" + key + "\" in 100 attempts, aborting load");
								}
								GfxLog(0, "Memory plugin \"" + key + "\" loaded");
							} else if (parent == RunnablePlugin.class){//runnable plugin without a kernel
								boolean added = false;
								String key = modString.replaceAll("[a-zA-Z0-9\\-_]*\\.", "");
								int pass = 0;
								while(!added || pass > 100) {
									boolean collision = false;
									for (String s : runnablePlugins.keySet()) {
										if ((key + (pass == 0 ? "" : pass)).equals(s)){
											GfxLog(1, "Collision indexing class \"" + key + (pass++ == 0 ? "" : pass) + "\", trying again with key \"" + key + pass + "\"");
										}
									}
									if (!collision) {
										try {
											runnablePlugins.put(key, (RunnablePlugin) newClass.newInstance());
										} catch (InstantiationException e) {
											GfxLog(2, "Could not instantiate runnable plugin \"" + key + "\", load failed");
											e.printStackTrace();
										} catch (IllegalAccessException e) {
											GfxLog(2, "Could not instantiate runnable plugin \"" + key + "\", load failed");
											e.printStackTrace();
										}
										added = true;
									}
									if (pass > 100 && !added) GfxLog(1, "Failed to index class \"" + key + "\" in 100 attempts, aborting load");
								}
							}
						} catch (ClassNotFoundException | NoClassDefFoundError e) {
							GfxLog(2, "Failure loading class \"" + c + "\"");
							e.printStackTrace();
						}
					}
				}
				classes.clear();
				kernels.clear();
				jars.clear();
				urls.clear();
			} else if (runningLocation.isFile()) {//if running location is a file, running from exported .jar
				GfxLog(0, "Loading plugins in app " + runningLocation);
				bcl = new ByteClassLoader();
				try {
					JarInputStream appStream = new JarInputStream(new FileInputStream(runningLocation));
					ZipEntry e = appStream.getNextEntry();
					
					//for each entry in the app
					while (e != null) {
						//if it's a jar file in the plugin directory
						if (e.getName().startsWith(pluginLocation.substring(1)) && e.getName().endsWith(".jar")) {
							JarInputStream pluginStream = new JarInputStream(appStream);
							GfxLog(0, "Loading plugin " + e.getName());
							ZipEntry z = pluginStream.getNextEntry();
							HashMap<String, byte[]> classes = new HashMap<String, byte[]>();
							HashMap<String, String> kernels = new HashMap<String, String>();
							//look through the plugin for classes and kernels
							while (z != null) {
								if (z.getName().endsWith(".class")) {
									GfxLog(0, "found class " + z.getName());
									byte[] bytes = pluginStream.readAllBytes();
									classes.put(z.getName().replaceAll("/", ".").replace(".class", ""), bytes);
								}
								if (z.getName().endsWith(".kernel")) {
									GfxLog(0, "found kernel " + z.getName());
									String kernel = "";
									String line = "";
									BufferedReader br = new BufferedReader(new InputStreamReader(pluginStream));
									while ((line = br.readLine()) != null) {
										kernel += line+"\n";
									}
									kernels.put(z.getName().replace(".kernel", ""), kernel);
								}
								z = pluginStream.getNextEntry();
							}
							//for each found class
							/*
							 * Uses while loop instead of a for loop to allow retries. a retry is needed if one plugin is a child class of/extends one of the other
							 * classes in the plugin, as the parent class must be loaded first before the child class can be resolved by the class loader. To retry
							 * a plugin, it is moved to the end of the array list, and tryDiff indicates the last index of plugins to be loaded this "try". If 
							 * iter = classesArr.size then all plugins are loaded and you can continue. if tryDiff does not change between tries, it means that 
							 * no new classes have been resolved this try and thus no more classes can be resolved.
							 */
							int iter = 0;
							ArrayList<String> classesArr = new ArrayList<String>();
							for (String s : classes.keySet()) classesArr.add(s);
							int tryIndex = classesArr.size() - 1;
							int prevTryDiff = -1;
							int tryDiff = classesArr.size() - 1;
							while(iter < classesArr.size() && tryDiff != prevTryDiff) {
								//use byte class loader to create the class
								try {
									String s = classesArr.get(iter);
//									System.out.println(s);
									bcl.currentBytes = classes.get(s);
									Class newClass = bcl.loadClass(s);
									//find relevant parent class
									Class parent = newClass.getSuperclass();
									while(parent != KernelPlugin.class && parent != MemoryPlugin.class && parent != RunnablePlugin.class && parent != Object.class)
										parent = parent.getSuperclass();
									//handle kernel plugin
									if (parent == KernelPlugin.class) {
										String progName = s.split("\\.")[s.split("\\.").length-1];
										boolean match = false;
										for (String ks : kernels.keySet())
											if (progName.equals(ks))
												match = true;
										if (!match) {
											GfxLog(2, "Did not find matching kernel for \"" + s + "\"");
											break;
										}
										//kernel-class match found, read kernel and try to create program
										try {
											gfxProg = org.jocl.CL.clCreateProgramWithSource(context, 1, new String[] {kernels.get(progName)}, null, null);
											org.jocl.CL.clBuildProgram(gfxProg, 0, null, null, null, null);
											int[] err = new int[1];
											cl_kernel newKernel = org.jocl.CL.clCreateKernel(gfxProg, progName, err);
											byte[] buildLog = new byte[2048];
											Pointer logPtr = Pointer.to(buildLog);
											int builderr = org.jocl.CL.clGetProgramBuildInfo(gfxProg, device, org.jocl.CL.CL_PROGRAM_BUILD_LOG, 2048, logPtr, null);
											if (err[0] != org.jocl.CL.CL_SUCCESS) throw new Exception("Kernel err = " + org.jocl.CL.stringFor_errorCode(err[0]) + new String(buildLog));
											//kernel creation was successful, load class
											KernelPlugin newProg = (KernelPlugin) newClass.newInstance();
											newProg.kernelLoad(newKernel);
											//index the class into the programs hashmap
											boolean added = false;
											String key = progName;
											int pass = 0;
											while(!added || pass > 100) {
												boolean collision = false;
												for (String st : runnablePlugins.keySet()) {
													if ((key + (pass == 0 ? "" : pass)).equals(st)){
														GfxLog(1, "Collision indexing class \"" + key + (pass++ == 0 ? "" : pass) + "\", trying again with key \"" + key + pass + "\"");
													}
												}
												if (!collision) {
													runnablePlugins.put(key, newProg);
													added = true;
												}
												if (pass > 100 && !added) GfxLog(1, "Failed to index class \"" + key + "\" in 100 attempts, aborting load");
											}
											GfxLog(0, "Kernel plugin \"" + progName + "\" loaded");
										} catch (Exception ex) {
											GfxLog(2, "Could not read kernel \"" + progName + "\"");
											ex.printStackTrace();
											break;
										}
									} 
									//handle memory plugin
									else if (parent == MemoryPlugin.class) {
										boolean added = false;
										String key = s.replaceAll("/", ".").replace(".class", "").replaceAll("[a-zA-Z0-9\\-_]*\\.", "");
										int pass = 0;
										while(!added || pass > 100) {
											boolean collision = false;
											for (String st : memoryPlugins.keySet()) {
												if ((key + (pass == 0 ? "" : pass)).equals(st)){
													GfxLog(1, "Collision indexing class \"" + key + (pass++ == 0 ? "" : pass) + "\", trying again with key \"" + key + pass + "\"");
												}
											}
											if (!collision) {
												memoryPlugins.put(key, newClass);
												added = true;
											}
											if (pass > 100 && !added) GfxLog(1, "Failed to index class \"" + key + "\" in 100 attempts, aborting load");
										}
										GfxLog(0, "Memory plugin \"" + key + "\" loaded");
									} 
									//handle runnable plugin
									else if (parent == RunnablePlugin.class) {
										boolean added = false;
										String key = s.replaceAll("/", ".").replace(".class", "").replaceAll("[a-zA-Z0-9\\-_]*\\.", "");
										int pass = 0;
										while(!added || pass > 100) {
											boolean collision = false;
											for (String st : runnablePlugins.keySet()) {
												if ((key + (pass == 0 ? "" : pass)).equals(st)){
													GfxLog(1, "Collision indexing class \"" + key + (pass++ == 0 ? "" : pass) + "\", trying again with key \"" + key + pass + "\"");
												}
											}
											if (!collision) {
												try {
													runnablePlugins.put(key, (RunnablePlugin) newClass.newInstance());
												} catch (InstantiationException ex) {
													GfxLog(2, "Could not instantiate runnable plugin \"" + key + "\", load failed");
													ex.printStackTrace();
												} catch (IllegalAccessException ex) {
													GfxLog(2, "Could not instantiate runnable plugin \"" + key + "\", load failed");
													ex.printStackTrace();
												}
												added = true;
											}
											if (pass > 100 && !added) GfxLog(1, "Failed to index class \"" + key + "\" in 100 attempts, aborting load");
										}
										GfxLog(0, "Runnable plugin \"" + key + "\" loaded");
									}
//									bcl.printClasses();
								} catch (NoClassDefFoundError | ClassNotFoundException e1) {
									GfxLog(1, "Error loading class " + classesArr.get(iter) + ", will retry later.");
									String newString = classesArr.get(iter);
									classesArr.remove(iter);
									classesArr.add(iter, "dummy");
									classesArr.add(newString);
//									e1.printStackTrace();
								}
								
								if(iter == tryIndex) {
									prevTryDiff = tryDiff;
									tryDiff = classesArr.size() - 1 - tryIndex;
									tryIndex = classesArr.size()-1;
								}
								iter++;
							}
							for (int i = iter; i < classesArr.size() - 1; i++) GfxLog(2, "Could not load plugin " + classesArr.get(i));
							//pluginStream.close();
						}
						e = appStream.getNextEntry();
					}
					appStream.close();
				} catch (IOException e) {
					GfxLog(2, "Failure loading from app \"" + runningLocation.getAbsolutePath() + "\"");
					e.printStackTrace();
				}
			} else {//running location doesn't exist probably
				GfxLog(2, "Error opening running location " + getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
				throw new FileNotFoundException();
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
		if (runnablePlugins.get(pluginKey) != null)
			try {
				runnablePlugins.get(pluginKey).run(args);
			} catch (Exception e) {
				GfxLog(2, "Uncaught exception in plugin \"" + pluginKey + "\"");
				e.printStackTrace();
			}
		else
			GfxLog(2, "Plugin \"" + pluginKey + "\" called but not loaded");
	}
	
	public void Release() {
		org.jocl.CL.clReleaseKernel(invertKern);
		
		for(RunnablePlugin p : runnablePlugins.values())
			if (p instanceof KernelPlugin)
				((KernelPlugin) p).release();
		runnablePlugins.clear();
			
		org.jocl.CL.clReleaseProgram(gfxProg);
		org.jocl.CL.clReleaseCommandQueue(commandQueue);
		org.jocl.CL.clReleaseContext(context);
		for(String s : indexes.keySet()) {
			org.jocl.CL.clReleaseMemObject(memObjects[indexes.get(s)]);
		}
		for(MemoryPlugin p : memoryHelpers)
			if (p != null)
				p.dispose();
		indexes.clear();
		frame.dispose();
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
	
	public String[] getKernelPluginList() {
		return runnablePlugins.keySet().toArray(new String[] {});
	}
	
	public String[] getMemoryPluginList() {
		return memoryPlugins.keySet().toArray(new String[] {});
	}
	
	public MemoryPlugin buildMemoryObject(String pluginKey, Object[] args) {
		try {
			Class[] classes = new Class[args.length];
			for (int i = 0; i < args.length; i++) {
				classes[i] = args[i].getClass();
			}
			Constructor<MemoryPlugin> constructor = memoryPlugins.get(pluginKey).getConstructor(classes);
			return (MemoryPlugin) constructor.newInstance(args);
		} catch (NullPointerException e) {
			GfxLog(2, "Memory plugin of type \"" + pluginKey + "\" does not exist");
			e.printStackTrace();
			return null;
		} catch (NoSuchMethodException e) {
			Constructor<MemoryPlugin>[] constructors = memoryPlugins.get(pluginKey).getConstructors();
			String constructorString = "";
			for (int i = 0; i < constructors.length; i++)
				constructorString += constructors[i] + "\n";
			constructorString = constructorString.substring(0, constructorString.length()-1);//gets rid of unneeded \n
			GfxLog(2, "Invalid arguments for type \"" + pluginKey + "\", valid constructors are: \n" + constructorString);
			e.printStackTrace();
			return null;
		} catch (Exception e) {
			GfxLog(2, "Unknown error building memory plugin of type " + pluginKey);
			e.printStackTrace();
			return null;
		}
	}
	
	public void initDebugWindow() {
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		
		plist.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				try {
					updateResource(plist.getSelectedIndex());
					plist.getSelectedValue().setVisible(true);
				} catch (NullPointerException ex) {
					GfxLog(1, "GFX memory debug viewer tried accessing invalid memory plugin " + plist.getSelectedIndex());
					ex.printStackTrace();
				} catch (Exception ex) {
					GfxLog(1, "Unknown error opening debug window for memory plugin " + plist.getSelectedIndex());
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
	
	
	
	public void showDebug() {
		frame.setVisible(true);
	}
		
	public int loadMemory(MemoryPlugin p) {
		if (p == null) {
			GfxLog(2, "Tried to load memory with null memory object");
			return -1;
		}
		String designation = p.getDefaultDesignation();
		int pass = 0;
		while(indexes.containsKey(designation) && (p.forceCollision || !memoryHelpers[indexes.get(designation)].getClass().equals(p.getClass()) ) && pass < 128) {//stop collisions on same key by different plugins
			pass++;
			GfxLog(1, "Collision indexing memory plugin with key \"" + designation + "\", trying again");
			if (pass > 1) {
				designation = designation.substring(0, designation.length()-1) + pass;
			} else {
				designation += pass;
			}
		}
		if (pass >= 128) {//all memory slots filled with colliding keys
			GfxLog(2, "Failure indexing new " + p.getClass());
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
				GfxLog(2, "Uncaught Exception trying to load new " + p.getClass());
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
				pmodel.removeElement(memoryHelpers[index]);
				memoryHelpers[index] = null;
				for (String s : indexes.keySet().toArray(new String[] {})) {
					indexes.remove(s, index);
				}
			}
			return stakes;
		} catch (IndexOutOfBoundsException e) {
			GfxLog(2, "Tried to release memory at index out of bounds index" + index);
			return -1;
		} catch (NullPointerException e) {
			GfxLog(2, "Tried to release nonexistant memory at index " + index);
			return -1;
		}
	}
	
	public Object retrieveMemory(int index) {
		try {
			return memoryHelpers[index].retrieveMemoryObject(index);
		} catch (IndexOutOfBoundsException e) {
			GfxLog(2, "Tried to retrieve memory at out of bounds index " + index);
			return null;
		} catch (NullPointerException e) {
			GfxLog(2, "Tried to retrieve nonexistant memory at index " + index);
			return null;
		}
	}
	
	public MemoryPlugin getMemoryPlugin(int index) {
		return memoryHelpers[index];
	}
	
	public void updateResource(int index) {
		if (frame.isVisible()) {
			GfxLog(0, "Updating debug window of plugin " + index);
			memoryHelpers[index].updateDebug(index);
		}
	}
	
}

class ByteClassLoader extends ClassLoader{
	
	public byte[] currentBytes;
	private HashMap<String, Class<?>> classes = new HashMap<String, Class<?>>();
	
	@Override
	protected Class<?> findClass(final String name) throws ClassNotFoundException{
		if (currentBytes != null) {
			Class<?> definedClass = defineClass(name, currentBytes, 0, currentBytes.length);
			classes.put(name, definedClass);
			currentBytes = null;
			return definedClass;
		}
		else {
			if (classes.get(name) != null) return classes.get(name);
			return super.findClass(name);
		}
	}
	
	public void printClasses() {
		for (String s : classes.keySet()) System.out.println(s);
	}
}