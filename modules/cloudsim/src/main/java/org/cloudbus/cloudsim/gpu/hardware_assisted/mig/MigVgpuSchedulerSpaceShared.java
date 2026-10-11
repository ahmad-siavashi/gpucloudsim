package org.cloudbus.cloudsim.gpu.hardware_assisted.mig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.gpu.Pgpu;
import org.cloudbus.cloudsim.gpu.Vgpu;
import org.cloudbus.cloudsim.gpu.VgpuScheduler;
import org.cloudbus.cloudsim.gpu.performance.PerformanceVgpuSchedulerSpaceShared;
import org.cloudbus.cloudsim.gpu.performance.models.PerformanceModel;
import org.cloudbus.cloudsim.gpu.selection.PgpuSelectionPolicy;

/**
 * A space-shared vGPU scheduler that places MIG-backed vGPUs by
 * {@link MigVgpuPlacement}. A GPU instance (GI) is a hardware partition of its
 * GPU, so the GIs of a GPU do not share its compute slices, its memory or its
 * memory bandwidth.
 *
 * @author Ahmad Siavashi
 */
public class MigVgpuSchedulerSpaceShared extends PerformanceVgpuSchedulerSpaceShared {

	protected final MigVgpuPlacement placement;

	/**
	 * Start slices requested by a policy that selects them itself, such as the
	 * executor of the dMIG method, for the next allocation of each vGPU.
	 */
	private final Map<Vgpu, Integer> requestedSlices = new HashMap<>();

	/**
	 * @param videoCardType the video card type, e.g.
	 *                      {@link MigGpuTags#NVIDIA_A100_40GB_CARD}
	 * @param policy        the policy that chooses the start slice of a GI
	 */
	public MigVgpuSchedulerSpaceShared(String videoCardType, List<Pgpu> pgpuList,
			PgpuSelectionPolicy pgpuSelectionPolicy, PerformanceModel<VgpuScheduler, Vgpu> performanceModel,
			MigVgpuPlacementPolicy policy) {
		super(videoCardType, pgpuList, pgpuSelectionPolicy, performanceModel);
		this.placement = new MigVgpuPlacement(videoCardType, pgpuList, policy);
	}

	/**
	 * Instantiates a scheduler whose GIs are placed by the built-in policy of
	 * NVIDIA, {@link MigVgpuPlacementPolicyCf}.
	 */
	public MigVgpuSchedulerSpaceShared(String videoCardType, List<Pgpu> pgpuList,
			PgpuSelectionPolicy pgpuSelectionPolicy, PerformanceModel<VgpuScheduler, Vgpu> performanceModel) {
		this(videoCardType, pgpuList, pgpuSelectionPolicy, performanceModel, new MigVgpuPlacementPolicyCf());
	}

	/**
	 * A GI takes the compute slices, the memory and the memory bandwidth of its
	 * profile, all of which its GPU must still have free, and the MIG rules must
	 * admit it at one of the start slices of the GPU.
	 * <p>
	 * The GIs of a GPU are partitions of it, so each takes SMs of its own. The
	 * space-shared scheduler of the generic layer instead compares the MIPS the
	 * vGPU requests with those of the SMs at the head of the list, which the GIs
	 * already on the GPU hold, so it is not used here.
	 */
	@Override
	public boolean isSuitable(Pgpu pgpu, Vgpu vgpu) {
		if (!placement.isSuitable(pgpu, vgpu)) {
			return false;
		}
		final List<Double> mipsShare = vgpu.getCurrentRequestedMips();
		if (!pgpu.getGddramProvisioner().isSuitableForVgpu(vgpu, vgpu.getCurrentRequestedGddram())
				|| !pgpu.getBwProvisioner().isSuitableForVgpu(vgpu, vgpu.getCurrentRequestedBw())) {
			return false;
		}
		List<Pe> freePes = pgpu.getPeList().stream()
				.filter(pe -> pe.getPeProvisioner().getTotalAllocatedMips() == 0).toList();
		if (freePes.size() < mipsShare.size()) {
			return false;
		}
		for (int i = 0; i < mipsShare.size(); i++) {
			if (mipsShare.get(i) > freePes.get(i).getPeProvisioner().getAvailableMips()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean allocatePgpuForVgpu(Pgpu pgpu, Vgpu vgpu, List<Double> mipsShare, int gddramShare, long bwShare) {
		if (super.allocatePgpuForVgpu(pgpu, vgpu, mipsShare, gddramShare, bwShare)) {
			Integer startSlice = requestedSlices.remove(vgpu);
			if (startSlice != null) {
				placement.place(pgpu, vgpu, startSlice);
			} else {
				placement.place(pgpu, vgpu);
			}
			return true;
		}
		requestedSlices.remove(vgpu);
		return false;
	}

	/**
	 * Requests the start slice of the next GI of the vGPU, bypassing the
	 * placement policy of the card.
	 *
	 * @param vgpu       the vGPU whose next GI the caller will allocate
	 * @param startSlice the start slice the GI must take
	 */
	public void requestStartSlice(Vgpu vgpu, int startSlice) {
		requestedSlices.put(vgpu, startSlice);
	}

	@Override
	public void deallocatePgpuForVgpu(Vgpu vgpu) {
		placement.remove(getPgpuForVgpu(vgpu), vgpu);
		super.deallocatePgpuForVgpu(vgpu);
	}

	public MigVgpuPlacement getPlacement() {
		return placement;
	}

}
