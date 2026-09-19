package org.cloudbus.cloudsim.gpu;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.gpu.selection.PgpuSelectionPolicy;
import org.cloudbus.cloudsim.util.MathUtil;

/**
 * A modification to {@link VgpuSchedulerFairShare} in which vGPU requested MIPS
 * is ignored and the host GPU determines the MIPS share.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class VgpuSchedulerFairShareEx extends VgpuSchedulerFairShare {

	public VgpuSchedulerFairShareEx(String videoCardType, List<Pgpu> pgpuList,
			PgpuSelectionPolicy pgpuSelectionPolicy) {
		super(videoCardType, pgpuList, pgpuSelectionPolicy);
		// TODO Auto-generated constructor stub
	}

	/**
	 * The PEs and MIPS that the vgpu requests are ignored, so only the memory and
	 * the bandwidth of the pgpu limit its vgpus.
	 */
	@Override
	public boolean isSuitable(Pgpu pgpu, Vgpu vgpu) {
		return pgpu.getGddramProvisioner().isSuitableForVgpu(vgpu, vgpu.getCurrentRequestedGddram())
				&& pgpu.getBwProvisioner().isSuitableForVgpu(vgpu, vgpu.getCurrentRequestedBw());
	}

	@Override
	public boolean allocatePgpuForVgpu(Pgpu pgpu, Vgpu vgpu, List<Double> mipsShare, int gddramShare, long bwShare) {
		if (!isSuitable(pgpu, vgpu)) {
			return false;
		}
		// set processing power according to the allocated pgpu
		vgpu.setNumberOfPes(pgpu.getPeList().size());
		vgpu.setPeMips(pgpu.getPeList().get(0).getMips());
		final List<Double> vgpuMipsShare = vgpu.getCurrentRequestedMips();
		final long vgpuBwShare = pgpu.getBwProvisioner().getBw();

		pgpu.getGddramProvisioner().allocateGddramForVgpu(vgpu, gddramShare);
		pgpu.getBwProvisioner().allocateBwForVgpu(vgpu, vgpuBwShare);
		getPgpuVgpuMap().get(pgpu).add(vgpu);

		getRequestedMipsMap().put(vgpu, vgpuMipsShare);
		getVgpuPeMap().put(vgpu, new ArrayList<Pe>());
		double mipsChange = MathUtil.sum(vgpuMipsShare);
		redistributeMipsDueToOverSubscription(pgpu, mipsChange);
		return true;
	}

}
