package org.cloudbus.cloudsim.gpu.remote;

import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.gpu.GpuHost;
import org.cloudbus.cloudsim.gpu.GpuVm;
import org.cloudbus.cloudsim.gpu.GpuVmAllocationPolicySimple;

/**
 * This class extends {@link RemoteGpuVmAllocationPolicy} and implements
 * first-fit allocation policy.
 * 
 * @author Ahmad Siavashi
 *
 */
public class RemoteGpuVmAllocationPolicySimple extends RemoteGpuVmAllocationPolicy {

	/**
	 * This class extends {@link RemoteGpuVmAllocationPolicy} and implements
	 * first-fit allocation policy.
	 * 
	 * @see {@link GpuVmAllocationPolicySimple}
	 */
	public RemoteGpuVmAllocationPolicySimple(List<? extends Host> list) {
		super(list);
	}

	@Override
	public boolean allocateHostForVm(Vm vm) {
		if (!getVmTable().containsKey(vm.getUid())) {
			GpuVm gpuVm = (GpuVm) vm;
			// A VM with a local vGPU needs a GPU-equipped host
			boolean hasLocalVgpu = gpuVm.getVgpuList().stream().anyMatch(RemoteVgpuTags::isLocal);
			for (Host host : hasLocalVgpu ? getGpuHostList() : this.<Host>getHostList()) {
				boolean result = allocateHostForVm(vm, host);
				if (result) {
					if (allocateGpusForVm(gpuVm, (GpuHost) host)) {
						return true;
					}
					deallocateHostForVm(vm);
					// Remote vGPUs do not depend on the host of the VM
					if (!hasLocalVgpu) {
						return false;
					}
				}
			}
		}
		return false;
	}
}