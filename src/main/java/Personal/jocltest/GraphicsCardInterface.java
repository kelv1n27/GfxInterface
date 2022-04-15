package Personal.jocltest;

import static org.jocl.CL.CL_CONTEXT_PLATFORM;
import static org.jocl.CL.CL_DEVICE_TYPE_ALL;
import static org.jocl.CL.CL_MEM_COPY_HOST_PTR;
import static org.jocl.CL.CL_MEM_READ_ONLY;
import static org.jocl.CL.CL_TRUE;
import static org.jocl.CL.clBuildProgram;
import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clCreateCommandQueue;
import static org.jocl.CL.clCreateContext;
import static org.jocl.CL.clCreateKernel;
import static org.jocl.CL.clCreateProgramWithSource;
import static org.jocl.CL.clEnqueueNDRangeKernel;
import static org.jocl.CL.clEnqueueReadBuffer;
import static org.jocl.CL.clGetDeviceIDs;
import static org.jocl.CL.clGetPlatformIDs;
import static org.jocl.CL.clSetKernelArg;
import static org.jocl.CL.*;

import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import javax.imageio.ImageIO;

import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_image_desc;
import org.jocl.cl_image_format;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;

public class GraphicsCardInterface {

	// The platform, device type and device number
    // that will be used
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

    private cl_program gfxProg;
    private cl_kernel upscaleKern;
    private cl_kernel fillKern;
    private cl_kernel renderKern;
    private cl_kernel lineKern;
    private cl_kernel invertKern;

	public GraphicsCardInterface() {

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
	        
	    memHandler = new MemoryManager(memObjects, context);

//	            + "int texturex = textureCoords.x + (flip.x==0?(int)((get_global_id(0)%textureSize.x)/textureScale.x):(int)((textureSize.x - (get_global_id(0)%textureSize.x) - 0)/textureScale.x));\n"
//				+ "int texturey = textureCoords.y + (flip.y==0?(int)((get_global_id(1)%textureSize.y)/textureScale.y):(int)((textureSize.y - (get_global_id(1)%textureSize.y) - 0)/textureScale.y));\n"
	    gfxProg = clCreateProgramWithSource(context, 5, new String[]{
	    		"__kernel void upscale(__global int *pixels, __global int *pixIn, const int2 outSize, const int2 inSize){\n"
	            + "int w = outSize.x;\n"
	            + "int h = outSize.y;\n"
	            + "int inw = inSize.x;\n"
	            + "int inh = inSize.y;\n"
	            + "float xTransform = (((float)inw)/w);\n"
	            + "float yTransform = (((float)inh)/h);\n"
	            + "int i = get_global_id(0);\n"
	            + "int j = get_global_id(1);\n"
	            + "pixels[j*w + i] = pixIn[((((int)(j*yTransform))*inw)+((int)(i*xTransform)))];\n"
	            + "}",
	            "__kernel void fill(__global int *pixIn, const int2 size, const uint color){\n"
	            + "int w = size.x;\n"
	            + "int x = get_global_id(0);\n"
	            + "int y = get_global_id(1);\n"
	            + "pixIn[y*w + x] = color;\n"
	            + "}",
	            "__kernel void render(__global int* screen, __global int* texture, const int2 screenCoords, "
	            + "const int2 screenSize, const int2 textureCoords, const int2 textureSize, const float2 textureScale, "
	            + "const int2 flip, const int2 renderSize, const int2 waveAmp, const int2 wavePeriod, const int2 waveOffset, const float alphaShift){\n"
	            + "int screenx = screenCoords.x + get_global_id(0) + (int)(waveAmp.x * sin( ((float)wavePeriod.x * get_global_id(1)) + waveOffset.x) );\n"
	            + "if (screenx >= 0 && screenx < screenSize.x){\n"
	            + "int screeny = screenCoords.y + get_global_id(1) + (int)(waveAmp.y * sin( ((float)wavePeriod.y * get_global_id(0)) + waveOffset.y) );\n"
	            + "if (screeny >= 0 && screeny < screenSize.y){\n"
	            + "int texturex = (textureCoords.x + (flip.x==0?(int)(get_global_id(0)/textureScale.x):(int)((renderSize.x - get_global_id(0)-1)/textureScale.x)))%textureSize.x;\n"
	            + "int texturey = (textureCoords.y + (flip.y==0?(int)(get_global_id(1)/textureScale.y):(int)((renderSize.y - get_global_id(1)-1)/textureScale.y)))%textureSize.y;\n"
	            + "int screenColor = screen[(screeny*screenSize.x)+screenx];\n"
	            + "int screenB = screenColor & 0xff;\n"
	            + "int screenG = (screenColor >> 8) & 0xff;\n"
	            + "int screenR = (screenColor >> 16) & 0xff;\n"
	            + "int screenA = (screenColor >> 24) & 0xff;\n"
	            + "int texColor = texture[(texturey*textureSize.x)+texturex];\n"
	            + "int texB = texColor & 0xff;\n"
	            + "int texG = (texColor >> 8) & 0xff;\n"
	            + "int texR = (texColor >> 16) & 0xff;\n"
	            + "int texA = min(0xff, (int)(((texColor >> 24) & 0xff) * alphaShift));\n"
	            + "float screenInfluence = (0xff - texA)/(float)0xff;\n"
	            + "float texInfluence = (float)texA/0xff;\n"
	            //probably still requires some tweaking, screen influence seems to not do the trick if alpha is low but RGB texture value is high
	            + "screen[(screeny*screenSize.x)+screenx] = "
	            + "(min(0xff,(screenA + texA)) << 24) + "
	            + "(min(0xff,((int)(screenR*screenInfluence) + (int)(texR*texInfluence))) << 16) + "
	            + "(min(0xff,((int)(screenG*screenInfluence) + (int)(texG*texInfluence))) << 8) + "
	            + "min(0xff,((int)(screenB*screenInfluence) + (int)(texB*texInfluence)));"
	            + "}}}",
	            "__kernel void drawLine(__global int* screen, const int2 screenDims, const int2 startPoint, const int2 diffs, "
	            + "const uint length, const uint color){\n"
	            + "float pos = get_global_id(0)/(float)length;\n"
	            + "int xPos = ((int) (diffs.x * pos)) + startPoint.x;\n"
	            + "int yPos = ((int) (diffs.y * pos)) + startPoint.y;\n"
	            + "if (xPos >= 0 && xPos < screenDims.x && yPos >= 0 && yPos < screenDims.y){\n"
	            + "screen[(yPos * screenDims.x) + xPos] = color;\n"
	            + "}}",
	            "__kernel void invert(__global int* screen, __global int* texture, const int2 screenCoords, "
	    	    + "const int2 screenSize, const int2 textureCoords, const int2 textureSize, const float2 textureScale, "
	    	    + "const int2 flip, const int2 renderSize, const int2 waveAmp, const int2 wavePeriod, const int2 waveOffset, const float alphaShift){\n"
	    	    + "int screenx = screenCoords.x + get_global_id(0) + (int)(waveAmp.x * sin( ((float)wavePeriod.x * get_global_id(1)) + waveOffset.x) );\n"
	    	    + "if (screenx >= 0 && screenx < screenSize.x){\n"
	    	    + "int screeny = screenCoords.y + get_global_id(1) + (int)(waveAmp.y * sin( ((float)wavePeriod.y * get_global_id(0)) + waveOffset.y) );\n"
	    	    + "if (screeny >= 0 && screeny < screenSize.y){\n"
	    	    + "int texturex = (textureCoords.x + (flip.x==0?(int)(get_global_id(0)/textureScale.x):(int)((renderSize.x - get_global_id(0)-1)/textureScale.x)))%textureSize.x;\n"
	    	    + "int texturey = (textureCoords.y + (flip.y==0?(int)(get_global_id(1)/textureScale.y):(int)((renderSize.y - get_global_id(1)-1)/textureScale.y)))%textureSize.y;\n"
	    	    + "int screenColor = screen[(screeny*screenSize.x)+screenx];\n"
	    	    + "int screenB = screenColor & 0xff;\n"
	    	    + "int screenG = (screenColor >> 8) & 0xff;\n"
	    	    + "int screenR = (screenColor >> 16) & 0xff;\n"
	    	    + "int screenA = (screenColor >> 24) & 0xff;\n"
	    	    + "int texColor = texture[(texturey*textureSize.x)+texturex];\n"
	    	    + "float texBRatio = (texColor & 0xff)/(float)255;\n"
	    	    + "float texGRatio = ((texColor >> 8) & 0xff)/(float)255;\n"
	    	    + "float texRRatio = ((texColor >> 16) & 0xff)/(float)255;\n"
	    	    + "screen[(screeny*screenSize.x)+screenx] = "
	    	    + "(screenA << 24) + "
	    	    + "((screenR + (int)(texRRatio * (0xff -(2 * screenR)))) << 16) + "
	    	    + "((screenG + (int)(texGRatio * (0xff -(2 * screenG)))) << 8) + "
	    	    + "(screenB + (int)(texBRatio * (0xff -(2 * screenB))));"
	    	    + "}}}"}, null, null);
	        // Build the program
	        clBuildProgram(gfxProg, 0, null, null, null, null);
	        // Create the kernels
	        upscaleKern = clCreateKernel(gfxProg, "upscale", null);
	        fillKern = clCreateKernel(gfxProg, "fill", null);
	        int error[] = new int[1];
	        renderKern = clCreateKernel(gfxProg, "render", error);
	        if (error[0] != org.jocl.CL.CL_SUCCESS) System.out.println("GFX ERROR: Error creating render kernel: " + org.jocl.CL.stringFor_errorCode(error[0]));
	        lineKern = clCreateKernel(gfxProg, "drawLine", error);
	        if (error[0] != org.jocl.CL.CL_SUCCESS) System.out.println("GFX ERROR: Error creating line kernel: " + org.jocl.CL.stringFor_errorCode(error[0]));
	        invertKern = clCreateKernel(gfxProg, "invert", error);
	        if (error[0] != org.jocl.CL.CL_SUCCESS) System.out.println("GFX ERROR: Error creating line kernel: " + org.jocl.CL.stringFor_errorCode(error[0]));
		}
		
		public int loadTexture(String path) {
			return memHandler.loadTexture(path);
		}
		
		public int unloadTexture(String path) {
			return memHandler.unloadTexture(path);
		}
		
		public int createCanvas(int w, int h) {
			return memHandler.createCanvas(w, h);
		}
		
		public void releaseCanvas(int i) {
			memHandler.releaseCanvas(i);
		}

		public void Release() {
			org.jocl.CL.clReleaseKernel(upscaleKern);
			org.jocl.CL.clReleaseKernel(fillKern);
			org.jocl.CL.clReleaseKernel(renderKern);
			org.jocl.CL.clReleaseKernel(lineKern);
			org.jocl.CL.clReleaseKernel(invertKern);
			org.jocl.CL.clReleaseProgram(gfxProg);
			org.jocl.CL.clReleaseCommandQueue(commandQueue);
			org.jocl.CL.clReleaseContext(context);
			memHandler.releaseMem();
		}
		
		public int[] getSheetDims(int sheetIndex) {
			MemHelper temp = memHandler.getHelper(sheetIndex);
			return new int[] {temp.getWidth(), temp.getHeight()};
		}

		public void upscale(int sourceCanvas, int destCanvas) {
			clSetKernelArg(upscaleKern, 0, Sizeof.cl_mem, Pointer.to(memObjects[destCanvas]));
	        clSetKernelArg(upscaleKern, 1, Sizeof.cl_mem, Pointer.to(memObjects[sourceCanvas]));
	        clSetKernelArg(upscaleKern, 2, Sizeof.cl_mem, Pointer.to(new int[] {memHandler.getHelper(destCanvas).getWidth(), memHandler.getHelper(destCanvas).getHeight()}));
	        clSetKernelArg(upscaleKern, 3, Sizeof.cl_mem, Pointer.to(new int[] {memHandler.getHelper(sourceCanvas).getWidth(), memHandler.getHelper(sourceCanvas).getHeight()}));

	        long local_work_size[] = new long[]{1, 1};
	        long global_work_size[] = new long[]{memHandler.getHelper(destCanvas).getWidth(), memHandler.getHelper(destCanvas).getHeight()};

	        int err = clEnqueueNDRangeKernel(commandQueue, upscaleKern, 2, null, global_work_size, local_work_size, 0, null, null);
	        if (err != org.jocl.CL.CL_SUCCESS) System.out.println("GFX ERROR: Upscale failed: " + org.jocl.CL.stringFor_errorCode(err));
		}

		//COMPLETELY GARBAGE AND DEPRECIATED
//		public void readBuffer(int src, int[] dest) {
//	        int err = clEnqueueReadBuffer(commandQueue, memObjects[src], CL_TRUE, 0, Sizeof.cl_int * (memHandler.getHelper(src).getWidth()*memHandler.getHelper(src).getHeight()), Pointer.to(dest), 0, null, null);
//	        if (err != org.jocl.CL.CL_SUCCESS) System.out.println("GFX ERROR: Readbuffer Failed: " + org.jocl.CL.stringFor_errorCode(err));
//		}
		
		public void readBuffer(int src, BufferedImage dest) {
			DataBufferInt output_buffer = (DataBufferInt) dest.getRaster().getDataBuffer();
	        int output_data[] = output_buffer.getData();
	        int err = clEnqueueReadBuffer(commandQueue, memObjects[src], CL_TRUE, 0, Sizeof.cl_int * (memHandler.getHelper(src).getWidth()*memHandler.getHelper(src).getHeight()), Pointer.to(output_data), 0, null, null);
	        if (err != org.jocl.CL.CL_SUCCESS) System.out.println("GFX ERROR: Readbuffer Failed: " + org.jocl.CL.stringFor_errorCode(err));
		}

		public void fillColor(int canvas, int color) {
			clSetKernelArg(fillKern, 0, Sizeof.cl_mem, Pointer.to(memObjects[canvas]));
	        clSetKernelArg(fillKern, 1, Sizeof.cl_mem, Pointer.to(new int[] {memHandler.getHelper(canvas).getWidth(), memHandler.getHelper(canvas).getHeight()}));
	        clSetKernelArg(fillKern, 2, Sizeof.cl_uint, Pointer.to(new int[] {color}));

	        long local_work_size[] = new long[]{1, 1};
	        long global_work_size[] = new long[]{memHandler.getHelper(canvas).getWidth(), memHandler.getHelper(canvas).getHeight()};

	        int err = clEnqueueNDRangeKernel(commandQueue, fillKern, 2, null, global_work_size, local_work_size, 0, null, null);
	        if (err != org.jocl.CL.CL_SUCCESS) System.out.println("GFX ERROR: Screen fill color failed: " + org.jocl.CL.stringFor_errorCode(err));
		}
		
		public void render(int canvas, int textureIndex, int x, int y, int xSpriteOffset, int ySpriteOffset, int spriteWidth, 
				int spriteHeight, float xScale, float yScale, boolean flipX, boolean flipY,
				int xWaveAmp, int yWaveAmp, int xWavePeriod, int yWavePeriod, int xWaveOffset, int yWaveOffset, float alphaShift) {

			MemHelper temp = memHandler.getHelper(textureIndex);
			if (temp != null) {
				clSetKernelArg(renderKern, 0, Sizeof.cl_mem, Pointer.to(memObjects[canvas]));
				clSetKernelArg(renderKern, 1, Sizeof.cl_mem, Pointer.to(memObjects[textureIndex]));
				clSetKernelArg(renderKern, 2, Sizeof.cl_int2, Pointer.to(new int[] {x, y}));
				clSetKernelArg(renderKern, 3, Sizeof.cl_int2, Pointer.to(new int[] {memHandler.getHelper(canvas).getWidth(), memHandler.getHelper(canvas).getHeight()}));//needs changing
				clSetKernelArg(renderKern, 4, Sizeof.cl_int2, Pointer.to(new int[] {xSpriteOffset, ySpriteOffset}));
				clSetKernelArg(renderKern, 5, Sizeof.cl_int2, Pointer.to(new int[] {temp.getWidth(), temp.getHeight()}));
				clSetKernelArg(renderKern, 6, Sizeof.cl_float2, Pointer.to(new float[] {xScale, yScale}));
				clSetKernelArg(renderKern, 7, Sizeof.cl_int2, Pointer.to(new int[] {(flipX?1:0), (flipY?1:0)}));
				clSetKernelArg(renderKern, 8, Sizeof.cl_int2, Pointer.to(new int[] {(int)(spriteWidth*xScale), (int)(spriteHeight*yScale)}));
				clSetKernelArg(renderKern, 9, Sizeof.cl_int2, Pointer.to(new int[] {xWaveAmp, yWaveAmp}));
				clSetKernelArg(renderKern, 10, Sizeof.cl_int2, Pointer.to(new int[] {xWavePeriod, yWavePeriod}));
				clSetKernelArg(renderKern, 11, Sizeof.cl_int2, Pointer.to(new int[] {xWaveOffset, yWaveOffset}));
				clSetKernelArg(renderKern, 12, Sizeof.cl_float, Pointer.to(new float[] {alphaShift}));
				
				long local_work_size[] = new long[]{1, 1};
				long global_work_size[] = new long[]{ (long) (spriteWidth*xScale), (long) (spriteHeight*yScale)};

				int err = clEnqueueNDRangeKernel(commandQueue, renderKern, 2, null, global_work_size, local_work_size, 0, null, null);
				if (err != org.jocl.CL.CL_SUCCESS) System.out.println("GFX ERROR: Failed to render sprite: " + org.jocl.CL.stringFor_errorCode(err));
			} else {
				System.out.println("GFX ERROR: Tried to render invalid texture at index " + textureIndex);
			}
			
		}
		
		public void drawLine(int canvas, int startX, int startY, int endX, int endY, int color) {
			MemHelper temp = memHandler.getHelper(canvas);
			if (temp != null) {
				int xDiff = endX - startX;
				int yDiff = endY - startY;
				int length = (int) Math.sqrt((double)(xDiff * xDiff) + (yDiff * yDiff));
				clSetKernelArg(lineKern, 0, Sizeof.cl_mem, Pointer.to(memObjects[canvas]));
				clSetKernelArg(lineKern, 1, Sizeof.cl_int2, Pointer.to(new int[] {temp.getWidth(), temp.getHeight()}));
				clSetKernelArg(lineKern, 2, Sizeof.cl_int2, Pointer.to(new int[] {startX, startY}));
				clSetKernelArg(lineKern, 3, Sizeof.cl_int2, Pointer.to(new int[] {xDiff, yDiff}));
				clSetKernelArg(lineKern, 4, Sizeof.cl_uint, Pointer.to(new int[] {length}));
				clSetKernelArg(lineKern, 5, Sizeof.cl_uint, Pointer.to(new int[] {color}));
				
				long local_work_size[] = new long[]{1, 1};
				long global_work_size[] = new long[]{ (long) length, 0L};

				int err = clEnqueueNDRangeKernel(commandQueue, lineKern, 1, null, global_work_size, local_work_size, 0, null, null);
				if (err != org.jocl.CL.CL_SUCCESS) System.out.println("Failed to render line: " + org.jocl.CL.stringFor_errorCode(err));
			} else {
				System.out.println("GFX ERROR: Tried to render to invalid canvas at index " + canvas);
			}
		}
		
		public void invert(int canvas, int textureIndex, int x, int y, int xSpriteOffset, int ySpriteOffset, int spriteWidth, 
				int spriteHeight, float xScale, float yScale, boolean flipX, boolean flipY,
				int xWaveAmp, int yWaveAmp, int xWavePeriod, int yWavePeriod, int xWaveOffset, int yWaveOffset, float alphaShift) {

			MemHelper temp = memHandler.getHelper(textureIndex);
			if (temp != null) {
				clSetKernelArg(invertKern, 0, Sizeof.cl_mem, Pointer.to(memObjects[canvas]));
				clSetKernelArg(invertKern, 1, Sizeof.cl_mem, Pointer.to(memObjects[textureIndex]));
				clSetKernelArg(invertKern, 2, Sizeof.cl_int2, Pointer.to(new int[] {x, y}));
				clSetKernelArg(invertKern, 3, Sizeof.cl_int2, Pointer.to(new int[] {memHandler.getHelper(canvas).getWidth(), memHandler.getHelper(canvas).getHeight()}));//needs changing
				clSetKernelArg(invertKern, 4, Sizeof.cl_int2, Pointer.to(new int[] {xSpriteOffset, ySpriteOffset}));
				clSetKernelArg(invertKern, 5, Sizeof.cl_int2, Pointer.to(new int[] {temp.getWidth(), temp.getHeight()}));
				clSetKernelArg(invertKern, 6, Sizeof.cl_float2, Pointer.to(new float[] {xScale, yScale}));
				clSetKernelArg(invertKern, 7, Sizeof.cl_int2, Pointer.to(new int[] {(flipX?1:0), (flipY?1:0)}));
				clSetKernelArg(invertKern, 8, Sizeof.cl_int2, Pointer.to(new int[] {(int)(spriteWidth*xScale), (int)(spriteHeight*yScale)}));
				clSetKernelArg(invertKern, 9, Sizeof.cl_int2, Pointer.to(new int[] {xWaveAmp, yWaveAmp}));
				clSetKernelArg(invertKern, 10, Sizeof.cl_int2, Pointer.to(new int[] {xWavePeriod, yWavePeriod}));
				clSetKernelArg(invertKern, 11, Sizeof.cl_int2, Pointer.to(new int[] {xWaveOffset, yWaveOffset}));
				clSetKernelArg(invertKern, 12, Sizeof.cl_float, Pointer.to(new float[] {alphaShift}));
				
				long local_work_size[] = new long[]{1, 1};
				long global_work_size[] = new long[]{ (long) (spriteWidth*xScale), (long) (spriteHeight*yScale)};

				int err = clEnqueueNDRangeKernel(commandQueue, invertKern, 2, null, global_work_size, local_work_size, 0, null, null);
				if (err != org.jocl.CL.CL_SUCCESS) System.out.println("GFX ERROR: Failed to invert sprite: " + org.jocl.CL.stringFor_errorCode(err));
			} else {
				System.out.println("GFX ERROR: Tried to invert using invalid texture at index " + textureIndex);
			}
			
			
			
		}
		
		//FOR TESTING ONLY
		public void XXX(final BufferedImage output_image, final BufferedImage input_image, float x, float y) {
	        cl_image_format format = new cl_image_format();
	        format.image_channel_order = CL_RGBA;
	        format.image_channel_data_type = CL_UNSIGNED_INT8;

	        //allocate ouput pointer
	        cl_image_desc output_description = new cl_image_desc();
	        output_description.buffer = null; //must be null for 2D image
	        output_description.image_depth = 0; //is only used if the image is a 3D image
	        output_description.image_row_pitch = 0; //must be 0 if host_ptr is null
	        output_description.image_slice_pitch = 0; //must be 0 if host_ptr is null
	        output_description.num_mip_levels = 0; //must be 0
	        output_description.num_samples = 0; //must be 0
	        output_description.image_type = CL_MEM_OBJECT_IMAGE2D;
	        output_description.image_width = output_image.getWidth();
	        output_description.image_height = output_image.getHeight();
	        output_description.image_array_size = output_description.image_width * output_description.image_height;

	        cl_mem output_memory = clCreateImage(context, CL_MEM_WRITE_ONLY, format, output_description, null, null);
	        
	        //set up first kernel arg
//	        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(output_memory));
	        
	        //allocates input pointer
	        cl_image_desc input_description = new cl_image_desc();
	        input_description.buffer = null; //must be null for 2D image
	        input_description.image_depth = 0; //is only used if the image is a 3D image
	        input_description.image_row_pitch = 0; //must be 0 if host_ptr is null
	        input_description.image_slice_pitch = 0; //must be 0 if host_ptr is null
	        input_description.num_mip_levels = 0; //must be 0
	        input_description.num_samples = 0; //must be 0
	        input_description.image_type = CL_MEM_OBJECT_IMAGE2D;
	        input_description.image_width = input_image.getWidth();
	        input_description.image_height = input_image.getHeight();
	        input_description.image_array_size = input_description.image_width * input_description.image_height;

	        DataBufferInt input_buffer = (DataBufferInt) input_image.getRaster().getDataBuffer();
	        int input_data[] = input_buffer.getData();

	        cl_mem input_memory = clCreateImage(context, CL_MEM_READ_ONLY | CL_MEM_USE_HOST_PTR, format, input_description, Pointer.to(input_data), null);

	        //loads the input buffer to the gpu memory
	        long[] input_origin = new long[] { 0, 0, 0 };
	        long[] input_region = new long[] { input_image.getWidth(), input_image.getHeight(), 1 };
	        int input_row_pitch = input_image.getWidth() * Sizeof.cl_uint; //the length of each row in bytes
	        clEnqueueWriteImage(commandQueue, input_memory, CL_TRUE, input_origin, input_region, input_row_pitch, 0, Pointer.to(input_data), 0, null, null);
	        
//	        //set up second kernel arg
//	        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(input_memory));
//
//	        //set up third and fourth kernel args
//	        clSetKernelArg(kernel, 2, Sizeof.cl_float, Pointer.to(new float[] { x }));
//	        clSetKernelArg(kernel, 3, Sizeof.cl_float, Pointer.to(new float[] { y }));
	        
	        //blocks until all previously queued commands are issued
	        clFinish(commandQueue);

	        //enqueue the program execution
//	        long[] globalWorkSize = new long[] { input_description.image_width, input_description.image_height };
//	        clEnqueueNDRangeKernel(commandQueue, kernel, 2, null, globalWorkSize, null, 0, null, null);

	        //transfer the output result back to host
	        DataBufferInt output_buffer = (DataBufferInt) output_image.getRaster().getDataBuffer();
	        int output_data[] = output_buffer.getData();
	        long[] output_origin = new long[] { 0, 0, 0 };
	        long[] output_region = new long[] { output_description.image_width, output_description.image_height, 1 };
	        int output_row_pitch = output_image.getWidth() * Sizeof.cl_uint;
	        clEnqueueReadImage(commandQueue, output_memory, CL_TRUE, output_origin, output_region, output_row_pitch, 0, Pointer.to(output_data), 0, null, null);

	        //free pointers
	        clReleaseMemObject(input_memory);
	        clReleaseMemObject(output_memory);
	    }

}