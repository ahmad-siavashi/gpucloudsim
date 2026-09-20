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
 * The fixed share scheduler of NVIDIA vGPU software. A vGPU always gets the
 * same share of its GPU, which is one over the maximum number of vGPUs of its
 * type that the GPU accepts. The share does not change when other vGPUs are
 * added to the GPU or removed from it, so a GPU with fewer vGPUs than its
 * maximum leaves cycles unused. On the contrary,
 * {@link GridVgpuSchedulerEqualShare} divides a GPU among its vGPUs and
 * {@link GridVgpuSchedulerBestEffort} among those that have running tasks.
 *
 * @author Ahmad Siavashi
 */
public class GridVgpuSchedulerFixedShare extends GridVgpuSchedulerEqualShare {

	/**
	 * Instantiates a new fixed share vgpu scheduler.
	 *
	 * @param videoCardType the video card type (see {@link VideoCardTags})
	 * @param pgpuList      the list of gpu PEs of the video card where the
	 *                      VgpuScheduler is associated to.
	 * @param mixedSize     whether the GPUs are in mixed-size mode
	 */
	public GridVgpuSchedulerFixedShare(String videoCardType, List<Pgpu> pgpuList,
			PgpuSelectionPolicy pgpuSelectionPolicy, PerformanceModel<VgpuScheduler, Vgpu> performanceModel,
			boolean mixedSize) {
		super(videoCardType, pgpuList, pgpuSelectionPolicy, performanceModel, mixedSize);
	}

	/**
	 * Instantiates a new fixed share vgpu scheduler whose GPUs are in equal-size
	 * mode, which is the default of NVIDIA vGPU software.
	 */
	public GridVgpuSchedulerFixedShare(String videoCardType, List<Pgpu> pgpuList,
			PgpuSelectionPolicy pgpuSelectionPolicy, PerformanceModel<VgpuScheduler, Vgpu> performanceModel) {
		super(videoCardType, pgpuList, pgpuSelectionPolicy, performanceModel);
	}

	/**
	 * The GPU is divided by the maximum number of vGPUs of the type of the vGPU,
	 * whether that many reside on the GPU or not.
	 */
	@Override
	public List<Double> getAllocatedMipsForVgpu(Vgpu vgpu) {
		Pgpu pgpu = getPgpuForVgpu(vgpu);
		if (pgpu == null) {
			return super.getAllocatedMipsForVgpu(vgpu);
		}
		int vgpus = getMaxVgpusOfType(vgpu);
		List<Double> mips = new ArrayList<Double>();
		for (Pe pe : pgpu.getPeList()) {
			mips.add(Math.floor(pe.getMips() / vgpus));
		}
		return mips;
	}

	/**
	 * @return the maximum number of vGPUs of the type of the vGPU that a GPU
	 *         accepts, which is the number of placements of its size
	 */
	protected int getMaxVgpusOfType(Vgpu vgpu) {
		final int sizeGb = vgpu.getGddram() / 1024;
		return GridVideoCardTags.getPlacements(getVideoCardType()).getIds(sizeGb, false).length;
	}

}
