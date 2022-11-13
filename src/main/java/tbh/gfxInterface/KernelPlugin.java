package tbh.gfxInterface;

import org.jocl.cl_kernel;

public abstract class KernelPlugin extends RunnablePlugin{
	
	//protected GraphicsCardInterface gfx = null;
	protected cl_kernel kernel = null;

	public void kernelLoad(cl_kernel kernel) {
		this.kernel = kernel;
	}
	
	public void release() {
		org.jocl.CL.clReleaseKernel(kernel);
	}
	
	public abstract void run(Object[] args);
}
