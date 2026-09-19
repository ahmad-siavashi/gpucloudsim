package org.cloudbus.cloudsim.gpu.hardware_assisted.grid;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.gpu.Pgpu;
import org.cloudbus.cloudsim.gpu.Vgpu;
import org.cloudbus.cloudsim.gpu.VgpuScheduler;
import org.cloudbus.cloudsim.gpu.VideoCardTags;
import org.cloudbus.cloudsim.gpu.performance.models.PerformanceModel;
import org.cloudbus.cloudsim.gpu.selection.PgpuSelectionPolicy;

/**
 * The best effort scheduler, which is the default time-sliced scheduler of
 * NVIDIA vGPU software. A GPU is shared in a round-robin fashion among its
 * vGPUs that have running tasks, so a vGPU uses the GPU cycles that idle vGPUs
 * leave. On the contrary, {@link GridVgpuSchedulerFairShareEx} gives every vGPU
 * of a GPU an equal share whether the other vGPUs are idle or not, which is the
 * equal share scheduler of NVIDIA vGPU software.
 *
 * @author Ahmad Siavashi
 */
public class GridVgpuSchedulerBestEffort extends GridVgpuSchedulerFairShareEx {

	/**
	 * Instantiates a new best effort vgpu scheduler.
	 *
	 * @param videoCardType the video card type (see {@link VideoCardTags})
	 * @param pgpuList      the list of gpu PEs of the video card where the
	 *                      VgpuScheduler is associated to.
	 * @param mixedSize     whether the GPUs are in mixed-size mode
	 */
	public GridVgpuSchedulerBestEffort(String videoCardType, List<Pgpu> pgpuList,
			PgpuSelectionPolicy pgpuSelectionPolicy, PerformanceModel<VgpuScheduler, Vgpu> performanceModel,
			boolean mixedSize) {
		super(videoCardType, pgpuList, pgpuSelectionPolicy, performanceModel, mixedSize);
	}

	/**
	 * Instantiates a new best effort vgpu scheduler whose GPUs are in equal-size
	 * mode, which is the default of NVIDIA vGPU software.
	 */
	public GridVgpuSchedulerBestEffort(String videoCardType, List<Pgpu> pgpuList,
			PgpuSelectionPolicy pgpuSelectionPolicy, PerformanceModel<VgpuScheduler, Vgpu> performanceModel) {
		super(videoCardType, pgpuList, pgpuSelectionPolicy, performanceModel);
	}

	/**
	 * The GPU is divided among the vGPU and the other vGPUs of the GPU that have
	 * running tasks.
	 */
	@Override
	public List<Double> getAllocatedMipsForVgpu(Vgpu vgpu) {
		Pgpu pgpu = getPgpuForVgpu(vgpu);
		if (pgpu == null) {
			return super.getAllocatedMipsForVgpu(vgpu);
		}
		int vgpus = 1;
		for (Vgpu other : getPgpuVgpuMap().get(pgpu)) {
			if (other != vgpu && other.getGpuTaskScheduler().runningTasks() > 0) {
				vgpus++;
			}
		}
		List<Double> mips = new ArrayList<Double>();
		for (Pe pe : pgpu.getPeList()) {
			mips.add(Math.floor(pe.getMips() / vgpus));
		}
		return mips;
	}

}
