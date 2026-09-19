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
	 * Allocates a local vGPU on the given host, which is the host of its VM. A
	 * remote vGPU may be allocated on any GPU-equipped host.
	 */
	@Override
	protected boolean allocateGpuForVgpu(Vgpu vgpu, GpuHost gpuHost) {
		if (RemoteVgpuTags.isLocal(vgpu)) {
			return super.allocateGpuForVgpu(vgpu, gpuHost);
		}
		for (GpuHost remoteGpuHost : getGpuHostList()) {
			if (super.allocateGpuForVgpu(vgpu, remoteGpuHost)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Are the vGPU and its VM allocated on different hosts?
	 * 
	 * @param vgpu
	 * @return
	 */
	public boolean isRemoteVgpu(Vgpu vgpu) {
		return getVgpuHosts().get(vgpu) != vgpu.getVm().getHost();
	}

	/**
	 * Is any vGPU of the VM allocated on a different host?
	 * 
	 * @param vm
	 * @return
	 */
	public boolean hasRemoteVgpu(GpuVm vm) {
		return vm.getVgpuList().stream().anyMatch(this::isRemoteVgpu);
	}

}
