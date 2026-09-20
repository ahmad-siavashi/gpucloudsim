/**
 * 
 */
package org.cloudbus.cloudsim.gpu.performance.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.cloudbus.cloudsim.gpu.Pgpu;
import org.cloudbus.cloudsim.gpu.Vgpu;
import org.cloudbus.cloudsim.gpu.VgpuScheduler;

/**
 * {@link PerformanceModelGpuPlacementConstant} imposes a constant performance
 * degradation on Vgpus that are placed on the same Pgpu, except for
 * pass-through vgpus which get the full performance of the underlying pgpu. It
 * is assumed VMs with idle Vgpus have no effect on performance of other VMs
 * with active Vgpus.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class PerformanceModelGpuPlacementConstant extends PerformanceModelGpuConstant {

	/**
	 * This class imposes a constant performance degradation on Vgpus that are
	 * placed on the same Pgpu. This class assumes VMs with idle Vgpus have no
	 * effect on performance of other VMs with active Vgpus.
	 * 
	 * @param performanceLoss
	 *            should be a number in [0,1] interval.
	 */
	public PerformanceModelGpuPlacementConstant(double performanceLoss) {
		super(performanceLoss);
	}

	@Override
	public List<Double> getAvailableMips(VgpuScheduler scheduler, Vgpu vgpu, List<Vgpu> vgpus) {
		// The vgpus that may degrade the performance of this one; idle vgpus do not,
		// as the class assumes. The given list belongs to the caller.
		List<Vgpu> others = new ArrayList<Vgpu>();
		for (Vgpu v : vgpus) {
			if (v != vgpu && v.getGpuTaskScheduler().runningTasks() > 0) {
				others.add(v);
			}
		}
		List<Double> allocatedMips = scheduler.getAllocatedMipsForVgpu(vgpu);
		List<Double> availableMips = super.getAvailableMips(scheduler, vgpu, others);
		for (Entry<Pgpu, List<Vgpu>> entry : scheduler.getPgpuVgpuMap().entrySet()) {
			if (entry.getValue().contains(vgpu)) {
				if (Collections.disjoint(entry.getValue(), others)) {
					return allocatedMips;
				}
				return availableMips;
			}
		}
		return allocatedMips;
	}
}
