package org.cloudbus.cloudsim.gpu.remote;

import java.util.List;

import org.cloudbus.cloudsim.gpu.Pgpu;
import org.cloudbus.cloudsim.gpu.Vgpu;
import org.cloudbus.cloudsim.gpu.VgpuScheduler;
import org.cloudbus.cloudsim.gpu.VgpuSchedulerFairShareEx;
import org.cloudbus.cloudsim.gpu.performance.PerformanceScheduler;
import org.cloudbus.cloudsim.gpu.performance.PerformanceVgpuSchedulerFairShareEx;
import org.cloudbus.cloudsim.gpu.performance.models.PerformanceModel;
import org.cloudbus.cloudsim.gpu.selection.PgpuSelectionPolicy;

/**
 * Extends {@link PerformanceVgpuSchedulerFairShareEx} to add support for GPU remoting.
 * 
 * @author Ahmad Siavashi
 * 
 * @see S. Iserte, R. Peña-Ortiz, J. Gutiérrez-Aguado, J. M. Claver and R. Mayo, "GSaaS: A Service to Cloudify and Schedule GPUs," in IEEE Access, vol. 6, pp. 39762-39774, 2018, doi: 10.1109/ACCESS.2018.2855261.
 * 
 */
public class RemoteVgpuSchedulerFairShareEx extends PerformanceVgpuSchedulerFairShareEx
		implements PerformanceScheduler<Vgpu> {

	/**
	 * Extends {@link PerformanceVgpuSchedulerFairShareEx} to add support for GPU remoting.
	 * 
	 * @see {@link VgpuSchedulerFairShareEx}
	 */
	public RemoteVgpuSchedulerFairShareEx(String videoCardType, List<Pgpu> pgpuList,
			PgpuSelectionPolicy pgpuSelectionPolicy, PerformanceModel<VgpuScheduler, Vgpu> performanceModel) {
		super(videoCardType, pgpuList, pgpuSelectionPolicy, performanceModel);
	}

	@Override
	public boolean isSuitable(Pgpu pgpu, Vgpu vgpu) {
		if (!super.isSuitable(pgpu, vgpu)) {
			return false;
		}
		List<Vgpu> pgpuVgpus = getPgpuVgpuMap().get(pgpu);
		if (pgpuVgpus.isEmpty()) {
			return true;

		}
		if (RemoteVgpuTags.isExclusive(vgpu)) {
			return false;

		}
		if (RemoteVgpuTags.isExclusive(pgpuVgpus.get(0))) {
			return false;
		}

		return true;
	}

}
