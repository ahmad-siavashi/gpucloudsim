package org.cloudbus.cloudsim.gpu.performance;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.VmScheduler;
import org.cloudbus.cloudsim.gpu.GpuHost;
import org.cloudbus.cloudsim.gpu.Vgpu;
import org.cloudbus.cloudsim.gpu.allocation.VideoCardAllocationPolicy;
import org.cloudbus.cloudsim.provisioners.BwProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisioner;

/**
 * {@link PerformanceGpuHost} extends {@link GpuHost} to add support for
 * schedulers that implement {@link PerformanceScheduler PerformanceScheduler}
 * interface.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class PerformanceGpuHost extends GpuHost {

	/**
	 * @see org.cloudbus.cloudsim.gpu.GpuHost#GpuHost GpuHost
	 */
	public PerformanceGpuHost(int id, String type, RamProvisioner ramProvisioner, BwProvisioner bwProvisioner,
			long storage, List<? extends Pe> peList, VmScheduler vmScheduler,
			VideoCardAllocationPolicy videoCardAllocationPolicy) {
		super(id, type, ramProvisioner, bwProvisioner, storage, peList, vmScheduler, videoCardAllocationPolicy);
	}

	/**
	 * @see org.cloudbus.cloudsim.gpu.GpuHost#GpuHost GpuHost
	 */
	public PerformanceGpuHost(int id, String type, RamProvisioner ramProvisioner, BwProvisioner bwProvisioner,
			long storage, List<? extends Pe> peList, VmScheduler vmScheduler) {
		super(id, type, ramProvisioner, bwProvisioner, storage, peList, vmScheduler);
	}

	@Override
	protected List<Double> getVgpuMips(Vgpu vgpu, List<Vgpu> runningVgpus) {
		@SuppressWarnings("unchecked")
		PerformanceScheduler<Vgpu> vgpuScheduler = (PerformanceScheduler<Vgpu>) getVideoCardAllocationPolicy()
				.getVgpuVideoCardMap().get(vgpu).getVgpuScheduler();
		// performance models may modify the list
		return vgpuScheduler.getAvailableMips(vgpu, new ArrayList<Vgpu>(runningVgpus));
	}

}
