package org.cloudbus.cloudsim.gpu.hardware_assisted.mig;

import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.gpu.GpuHost;
import org.cloudbus.cloudsim.gpu.GpuVm;
import org.cloudbus.cloudsim.gpu.GpuVmAllocationPolicy;
import org.cloudbus.cloudsim.gpu.Pgpu;
import org.cloudbus.cloudsim.gpu.Vgpu;

/**
 * The default MIG placement policy: first-fit host selection paired with the
 * built-in GI placement policy of NVIDIA.
 * <p>
 * The VM is placed on the first host that can accommodate it, each of its GIs
 * is placed on the first GPU that admits it, and the start slice of a GI is the
 * one that maximizes the configuration flexibility (CF) of the GPU, with ties
 * broken by the lowest slice. The GIs of a multi-GI VM are placed one at a
 * time, and the VM is placed completely or not at all.
 * <p>
 * This is the baseline the built-in policy of
 * {@link MigVgpuSchedulerSpaceShared} implements at the slice level, so the
 * schedulers of the video cards are expected to use
 * {@link MigVgpuPlacementPolicyCf}.
 *
 * @author Ahmad Siavashi
 */
public class MigGpuVmAllocationPolicyDefault extends GpuVmAllocationPolicy {

	public MigGpuVmAllocationPolicyDefault(List<? extends Host> list) {
		super(list);
	}

	@Override
	public boolean allocateHostForVm(Vm vm) {
		if (getVmTable().containsKey(vm.getUid())) {
			return false;
		}
		GpuVm gpuVm = (GpuVm) vm;
		for (Host host : candidateHosts()) {
			if (!allocateHostForVm(gpuVm, host)) {
				continue;
			} else if (allocateGpusForVm(gpuVm, (GpuHost) host)) {
				return true;
			}
			deallocateHostForVm(gpuVm);
		}
		return false;
	}

	/**
	 * Places the GI of the vGPU on the first GPU of the host that admits it.
	 */
	@Override
	protected boolean allocateGpuForVgpu(Vgpu vgpu, GpuHost gpuHost) {
		if (getVgpuHosts().containsKey(vgpu)) {
			return false;
		}
		for (Pgpu pgpu : getPgpuList(gpuHost)) {
			if (allocateGpuForVgpu(vgpu, gpuHost, pgpu)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The hosts the arrival rule may place the VM on. The default is every host
	 * of the data center; the dMIG method overrides this while a plan is active,
	 * because the arrival rule then places no request on the neighborhood of the
	 * plan.
	 */
	protected List<Host> candidateHosts() {
		return this.<Host>getHostList();
	}

	/**
	 * @return the GPUs of the host, in the order of its video cards
	 */
	protected List<Pgpu> getPgpuList(GpuHost gpuHost) {
		if (!gpuHost.isGpuEquipped()) {
			return List.of();
		}
		return gpuHost.getVideoCardAllocationPolicy().getVideoCards().stream()
				.flatMap(videoCard -> videoCard.getVgpuScheduler().getPgpuList().stream()).toList();
	}

}
