package org.cloudbus.cloudsim.gpu.selection;

import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.cloudbus.cloudsim.gpu.Pgpu;
import org.cloudbus.cloudsim.gpu.Vgpu;
import org.cloudbus.cloudsim.gpu.VgpuScheduler;

public class PgpuSelectionPolicyNull implements PgpuSelectionPolicy {

	public PgpuSelectionPolicyNull() {
	}

	@Override
	public Pgpu selectPgpu(Vgpu vgpu, VgpuScheduler scheduler, List<Pgpu> pgpuList) {
		throw new NotImplementedException("");
	}

}
