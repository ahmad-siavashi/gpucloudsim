package org.cloudbus.cloudsim.gpu.remote;

import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.gpu.GpuHost;
import org.cloudbus.cloudsim.gpu.GpuVm;
import org.cloudbus.cloudsim.gpu.GpuVmAllocationPolicy;
import org.cloudbus.cloudsim.gpu.Vgpu;

/**
 * This class extends {@link GpuVmAllocationPolicy} to add support for GPU
 * remoting.
 * 
 * @author Ahmad Siavashi
 *
 */
public abstract class RemoteGpuVmAllocationPolicy extends GpuVmAllocationPolicy {

	/**
	 * This class extends {@link GpuVmAllocationPolicy} to add support for GPU
	 * remoting.
	 * 
	 * @see {@link GpuVmAllocationPolicy}
	 */
	public RemoteGpuVmAllocationPolicy(List<? extends Host> list) {
		super(list);

	}

	/**
	 * Are VM and vGPU allocated on different hosts?
	 * 
	 * @param vm
	 * @return
	 */
	public boolean hasRemoteVgpu(GpuVm vm) {
		GpuHost host = (GpuHost) vm.getHost();
		Vgpu vgpu = vm.getVgpu();
		if (vgpu == null || host.getVideoCardAllocationPolicy().getVideoCards().contains(vgpu.getVideoCard())) {
			return false;
		}
		return true;
	}

}
