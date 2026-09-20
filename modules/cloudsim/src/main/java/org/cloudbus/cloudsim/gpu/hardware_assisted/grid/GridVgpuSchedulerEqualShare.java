package org.cloudbus.cloudsim.gpu.hardware_assisted.grid;

import java.util.List;

import org.cloudbus.cloudsim.gpu.Pgpu;
import org.cloudbus.cloudsim.gpu.Vgpu;
import org.cloudbus.cloudsim.gpu.VgpuScheduler;
import org.cloudbus.cloudsim.gpu.VgpuSchedulerFairShare;
import org.cloudbus.cloudsim.gpu.VideoCardTags;
import org.cloudbus.cloudsim.gpu.performance.PerformanceVgpuSchedulerFairShareEx;
import org.cloudbus.cloudsim.gpu.performance.models.PerformanceModel;
import org.cloudbus.cloudsim.gpu.selection.PgpuSelectionPolicy;

/**
 * The equal share scheduler of NVIDIA vGPU software. A GPU is divided equally
 * among the vGPUs that reside on it, whether they have running tasks or not, so
 * the share of a vGPU changes only when another vGPU is added to the GPU or
 * removed from it. Every vGPU type is time-sliced and hence requests the whole
 * GPU (see {@link GridVgpuTags}), so the proportional share of
 * {@link VgpuSchedulerFairShare} gives every vGPU of a GPU an equal share. It
 * places vGPUs by {@link GridVgpuPlacement}.
 *
 * @author Ahmad Siavashi
 */
public class GridVgpuSchedulerEqualShare extends PerformanceVgpuSchedulerFairShareEx {

	protected final GridVgpuPlacement placement;

	/**
	 * Instantiates a new equal share vgpu scheduler.
	 * 
	 * @param videoCardType the video card type (see {@link VideoCardTags})
	 * @param pgpuList      the list of gpu PEs of the video card where the
	 *                      VgpuScheduler is associated to.
	 * @param mixedSize     whether the GPUs are in mixed-size mode
	 */
	public GridVgpuSchedulerEqualShare(String videoCardType, List<Pgpu> pgpuList,
			PgpuSelectionPolicy pgpuSelectionPolicy, PerformanceModel<VgpuScheduler, Vgpu> performanceModel,
			boolean mixedSize) {
		super(videoCardType, pgpuList, pgpuSelectionPolicy, performanceModel);
		this.placement = new GridVgpuPlacement(videoCardType, pgpuList, mixedSize);
	}

	/**
	 * Instantiates a new equal share vgpu scheduler whose GPUs are in equal-size
	 * mode, which is the default of NVIDIA vGPU software.
	 */
	public GridVgpuSchedulerEqualShare(String videoCardType, List<Pgpu> pgpuList,
			PgpuSelectionPolicy pgpuSelectionPolicy, PerformanceModel<VgpuScheduler, Vgpu> performanceModel) {
		this(videoCardType, pgpuList, pgpuSelectionPolicy, performanceModel, false);
	}

	@Override
	public boolean isSuitable(Pgpu pgpu, Vgpu vgpu) {
		return placement.isSuitable(pgpu, vgpu) && super.isSuitable(pgpu, vgpu);
	}

	@Override
	public boolean allocatePgpuForVgpu(Pgpu pgpu, Vgpu vgpu, List<Double> mipsShare, int gddramShare, long bwShare) {
		if (super.allocatePgpuForVgpu(pgpu, vgpu, mipsShare, gddramShare, bwShare)) {
			placement.place(pgpu, vgpu);
			return true;
		}
		return false;
	}

	@Override
	public void deallocatePgpuForVgpu(Vgpu vgpu) {
		placement.remove(getPgpuForVgpu(vgpu), vgpu);
		super.deallocatePgpuForVgpu(vgpu);
	}

	public GridVgpuPlacement getPlacement() {
		return placement;
	}
}
