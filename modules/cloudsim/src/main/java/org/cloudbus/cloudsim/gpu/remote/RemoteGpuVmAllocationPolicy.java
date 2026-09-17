package org.cloudbus.cloudsim.gpu.remote;

import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
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
	 * Allocates the given host and, if the VM has a vGPU, a GPU. A remote vGPU
	 * may be allocated on any GPU-equipped host.
	 */
	@Override
	public boolean allocateHostForGuest(GuestEntity guest, HostEntity host) {
		GpuVm vm = (GpuVm) guest;
		Vgpu vgpu = vm.getVgpu();
		if (vgpu == null || RemoteVgpuTags.isLocal(vgpu)) {
			return super.allocateHostForGuest(guest, host);
		}
		if (!allocateHostForVm(vm, (Host) host)) {
			return false;
		}
		for (GpuHost gpuHost : getGpuHostList()) {
			if (allocateGpuForVgpu(vgpu, gpuHost)) {
				return true;
			}
		}
		deallocateHostForVm(vm);
		return false;
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
