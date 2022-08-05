package org.cloudbus.cloudsim.gpu.hardware_assisted.grid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.cloudbus.cloudsim.gpu.Pgpu;
import org.cloudbus.cloudsim.gpu.Vgpu;
import org.cloudbus.cloudsim.gpu.VgpuScheduler;
import org.cloudbus.cloudsim.gpu.performance.PerformanceVgpuSchedulerFairShareEx;
import org.cloudbus.cloudsim.gpu.performance.models.PerformanceModel;
import org.cloudbus.cloudsim.gpu.selection.PgpuSelectionPolicy;

/**
 * This is a Time-Shared vgpu scheduler, which allows over-subscription. In
 * other words, the scheduler still allows the allocation of Vgpus that require
 * more GPU capacity than is available. OverSubscription results in performance
 * degradation. This scheduler can be considered as fair-share scheduler which
 * in turn is a time-sliced round-robin scheduler.
 * 
 * @author Ahmad Siavashi
 */
public class GridVgpuSchedulerFairShareEx extends PerformanceVgpuSchedulerFairShareEx {

	protected final String[] profiles;
	protected Map<Pgpu, String> allocationMap;

	/**
	 * Instantiates a new fair-share vgpu scheduler.
	 * 
	 * @param pgpulist the list of gpu PEs of the video card where the VgpuScheduler
	 *                 is associated to.
	 */

	public GridVgpuSchedulerFairShareEx(String videoCardType, List<Pgpu> pgpuList,
			PgpuSelectionPolicy pgpuSelectionPolicy, PerformanceModel<VgpuScheduler, Vgpu> performanceModel,
			String[] profiles) {
		super(videoCardType, pgpuList, pgpuSelectionPolicy, performanceModel);
		this.allocationMap = new HashMap<>();
		this.profiles = profiles;
	}

	@Override
	public boolean isSuitable(Pgpu pgpu, Vgpu vgpu) {
		if ((this.allocationMap.getOrDefault(pgpu, null) == null || this.allocationMap.get(pgpu).equals(vgpu.getType()))
				&& ArrayUtils.contains(this.profiles, vgpu.getType())) {
			return super.isSuitable(pgpu, vgpu);
		}
		return false;
	}

	@Override
	public boolean allocatePgpuForVgpu(Pgpu pgpu, Vgpu vgpu, List<Double> mipsShare, int gddramShare, long bwShare) {
		if (super.allocatePgpuForVgpu(pgpu, vgpu, mipsShare, gddramShare, bwShare)) {
			this.allocationMap.put(pgpu, vgpu.getType());
			return true;
		}
		return false;
	}

	@Override
	public void deallocatePgpuForVgpu(Vgpu vgpu) {
		Pgpu pgpu = getPgpuForVgpu(vgpu);
		if (getPgpuVgpuMap().get(pgpu).size() == 1) {
			this.allocationMap.remove(pgpu);
		}
		super.deallocatePgpuForVgpu(vgpu);
	}
}
