package org.cloudbus.cloudsim.gpu.hardware_assisted.grid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.cloudbus.cloudsim.gpu.Pgpu;
import org.cloudbus.cloudsim.gpu.Vgpu;
import org.cloudbus.cloudsim.gpu.VgpuScheduler;
import org.cloudbus.cloudsim.gpu.performance.PerformanceVgpuSchedulerSpaceShared;
import org.cloudbus.cloudsim.gpu.performance.models.PerformanceModel;
import org.cloudbus.cloudsim.gpu.selection.PgpuSelectionPolicy;

/**
 * A space-shared vGPU Scheduler that considers GRID restrictions.
 * 
 * @author Ahmad Siavashi
 */
public class GridVgpuSchedulerSpaceShared extends PerformanceVgpuSchedulerSpaceShared {

	protected final String[] profiles;
	protected Map<Pgpu, String> allocationMap;

	public GridVgpuSchedulerSpaceShared(String videoCardType, List<Pgpu> pgpuList,
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
