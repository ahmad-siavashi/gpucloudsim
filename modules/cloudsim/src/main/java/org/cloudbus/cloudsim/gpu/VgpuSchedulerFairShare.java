package org.cloudbus.cloudsim.gpu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.gpu.provisioners.GpuPeProvisionerSimple;
import org.cloudbus.cloudsim.gpu.selection.PgpuSelectionPolicy;
import org.cloudbus.cloudsim.lists.PeList;
import org.cloudbus.cloudsim.util.MathUtil;

/**
 * This is a Time-Shared {@link Vgpu} scheduler, which allows over-subscription.
 * In other words, the scheduler still allows the allocation of Vgpus that
 * require more GPU capacity than is available. OverSubscription results in
 * performance degradation. This scheduler can be considered as fair-share
 * scheduler which in turn is a time-sliced round-robin scheduler.
 * 
 * @author Ahmad Siavashi
 */
public class VgpuSchedulerFairShare extends VgpuSchedulerTimeShared {

	/**
	 * Requested Vgpu mips (which defers from mipsMap that holds actual scales mips
	 * values)
	 */
	private Map<Vgpu, List<Double>> requestedMipsMap;

	/**
	 * Instantiates a new fair-share vgpu scheduler.
	 * 
	 * @param pgpulist the list of gpu PEs of the video card where the VgpuScheduler
	 *                 is associated to.
	 */
	public VgpuSchedulerFairShare(String videoCardType, List<Pgpu> pgpuList, PgpuSelectionPolicy pgpuSelectionPolicy) {
		super(videoCardType, pgpuList, pgpuSelectionPolicy);
		setRequestedMipsMap(new HashMap<Vgpu, List<Double>>());
	}

	/**
	 * MIPS are over-subscribed, so only the memory and the bandwidth of the pgpu
	 * limit its vgpus.
	 */
	@Override
	public boolean isSuitable(Pgpu pgpu, Vgpu vgpu) {
		if (pgpu.getPeList().size() < vgpu.getCurrentRequestedMips().size()) {
			return false;
		}
		return pgpu.getGddramProvisioner().isSuitableForVgpu(vgpu, vgpu.getCurrentRequestedGddram())
				&& pgpu.getBwProvisioner().isSuitableForVgpu(vgpu, vgpu.getCurrentRequestedBw());
	}

	@Override
	public boolean allocatePgpuForVgpu(Pgpu pgpu, Vgpu vgpu, List<Double> mipsShare, int gddramShare, long bwShare) {
		if (!isSuitable(pgpu, vgpu)) {
			return false;
		}
		pgpu.getGddramProvisioner().allocateGddramForVgpu(vgpu, gddramShare);
		pgpu.getBwProvisioner().allocateBwForVgpu(vgpu, bwShare);
		getPgpuVgpuMap().get(pgpu).add(vgpu);
		getRequestedMipsMap().put(vgpu, mipsShare);
		getVgpuPeMap().put(vgpu, new ArrayList<Pe>());
		double mipsChange = MathUtil.sum(mipsShare);
		redistributeMipsDueToOverSubscription(pgpu, mipsChange);
		return true;
	}

	/**
	 * Rescales mips share of resident vgpus whenever a vgpu enters or leaves.
	 * 
	 * @param pgpu       the pgpu to redistribute the mips share of its resident
	 *                   vgpus
	 * @param mipsChange the amount of mips that has been changed in the pgpu;
	 *                   either added or removed.
	 */
	protected void redistributeMipsDueToOverSubscription(final Pgpu pgpu, double mipsChange) {
		// find vgpus running on the selected pgpu
		List<Vgpu> pgpuVgpus = getPgpuVgpuMap().get(pgpu);
		// calculating the scaling factor from the requested mips of the vgpus; their
		// allocated mips are already scaled
		final double totalPgpuMips = PeList.getTotalMips(pgpu.getPeList());
		double totalRequestedMipsFromPgpu = 0.0;
		for (Vgpu vgpu : pgpuVgpus) {
			totalRequestedMipsFromPgpu += MathUtil.sum(getRequestedMipsMap().get(vgpu));
		}
		final double scaleFactor = totalPgpuMips / totalRequestedMipsFromPgpu;
		// deallocate
		for (Vgpu vgpu : pgpuVgpus) {
			for (Pe pe : getVgpuPeMap().get(vgpu)) {
				((GpuPeProvisionerSimple) pe.getPeProvisioner()).deallocateMipsForGuest(vgpu.getUid());
			}
		}
		for (Vgpu vgpu : pgpuVgpus) {
			List<Double> scaledVmMips = new ArrayList<Double>();
			// scale
			for (double mips : getRequestedMipsMap().get(vgpu)) {
				scaledVmMips.add(Math.floor(mips * scaleFactor));
			}
			double totalScaledMipsForVm = MathUtil.sum(scaledVmMips);
			double totalRequestedMipsForVm = MathUtil.sum(getRequestedMipsMap().get(vgpu));
			// a vgpu never gets more than it requests
			if (totalScaledMipsForVm >= totalRequestedMipsForVm) {
				scaledVmMips = getRequestedMipsMap().get(vgpu);
			}
			getMipsMap().put(vgpu, scaledVmMips);
			vgpu.setCurrentAllocatedMips(scaledVmMips);
			// reallocate
			Collections.sort(pgpu.getPeList(), Collections.reverseOrder(new Comparator<Pe>() {
				public int compare(Pe pe1, Pe pe2) {
					return Double.compare(pe1.getPeProvisioner().getAvailableMips(),
							pe2.getPeProvisioner().getAvailableMips());
				}
			}));
			getVgpuPeMap().get(vgpu).clear();
			// No two Vgpu PEs are mapped to one Pgpu PE
			for (int i = 0; i < scaledVmMips.size(); i++) {
				Pe pe = pgpu.getPeList().get(i);
				pe.getPeProvisioner().allocateMipsForGuest(vgpu.getUid(), scaledVmMips.get(i));
				getVgpuPeMap().get(vgpu).add(pe);
			}
		}
	}

	@Override
	public void deallocatePgpuForVgpu(Vgpu vgpu) {
		Pgpu pgpu = getPgpuForVgpu(vgpu);
		pgpu.getGddramProvisioner().deallocateGddramForVgpu(vgpu);
		pgpu.getBwProvisioner().deallocateBwForVgpu(vgpu);
		double totalMipsChange = 0.0;
		getPgpuVgpuMap().get(pgpu).remove(vgpu);
		for (Pe pe : getVgpuPeMap().get(vgpu)) {
			GpuPeProvisionerSimple peProvisioner = (GpuPeProvisionerSimple) pe.getPeProvisioner();
			double allocatedMipsForVm = peProvisioner.getTotalAllocatedMipsForGuest(vgpu.getUid());
			peProvisioner.deallocateMipsForGuest(vgpu.getUid());
			totalMipsChange += allocatedMipsForVm;
		}
		getVgpuPeMap().remove(vgpu);
		getMipsMap().remove(vgpu);
		vgpu.setCurrentAllocatedMips(null);
		getRequestedMipsMap().remove(vgpu);
		redistributeMipsDueToOverSubscription(pgpu, totalMipsChange);
	}

	/**
	 * @return the requestedMipsMap
	 */
	public Map<Vgpu, List<Double>> getRequestedMipsMap() {
		return requestedMipsMap;
	}

	/**
	 * @param requestedMipsMap the requestedMipsMap to set
	 */
	protected void setRequestedMipsMap(Map<Vgpu, List<Double>> requestedMipsMap) {
		this.requestedMipsMap = requestedMipsMap;
	}
}
