package tbh.gfxInterface;

import org.jocl.cl_kernel;

public abstract class KernelPlugin {
	
	public GraphicsCardInterface gfx = null;
	public cl_kernel kernel = null;

	public void load(GraphicsCardInterface gfx, cl_kernel kernel) {
		this.gfx = gfx;
		this.kernel = kernel;
	}
	
	public void release() {
		org.jocl.CL.clReleaseKernel(kernel);
	}
	
	public abstract void run(Object[] args);
}
