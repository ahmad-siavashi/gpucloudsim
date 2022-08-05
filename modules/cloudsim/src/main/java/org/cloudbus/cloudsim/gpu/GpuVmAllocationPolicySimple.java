package org.cloudbus.cloudsim.gpu;

import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;

/**
 * {@link GpuVmAllocationPolicySimple} extends {@link GpuVmAllocationPolicy} and
 * implements first-fit algorithm for VM placement.
 * 
 * @author Ahmad Siavashi
 *
 */
public class GpuVmAllocationPolicySimple extends GpuVmAllocationPolicy {

	/**
	 * @param list
	 */
	public GpuVmAllocationPolicySimple(List<? extends Host> list) {
		super(list);
	}

	@Override
	public boolean allocateHostForVm(Vm vm) {
		if (!getVmTable().containsKey(vm.getUid())) {
			GpuVm gpuVm = (GpuVm) vm;
			for (Host host : getHostList()) {
				boolean result = allocateHostForVm(vm, host);
				if (!result) {
					continue;
				} else if (!gpuVm.hasVgpu()) {
					return true;
				} else if (((GpuHost) host).isGpuEquipped() && allocateGpuForVgpu(gpuVm.getVgpu(), (GpuHost) host)) {
					return true;
				}
				deallocateHostForVm(gpuVm);
			}
		}
		return false;
	}
}
