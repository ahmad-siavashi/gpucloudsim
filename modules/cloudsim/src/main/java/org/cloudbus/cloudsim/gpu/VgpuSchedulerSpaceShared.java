package org.cloudbus.cloudsim.gpu;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.gpu.selection.PgpuSelectionPolicy;

/**
 * VgpuSchedulerSpaceShared is a vgpu allocation policy that allocates one or
 * more pgpu PEs from a video card to a vgpu, and doesn't allow sharing of
 * pgpus' PEs. The allocated PEs will be used until the vgpu finishes running.
 * If there is no enough free PEs as required by a vgpu, or whether the
 * available PEs doesn't have enough capacity, the allocation fails. In the case
 * of fail, no PE is allocated to the requesting vgpu.
 * 
 * @author Ahmad Siavashi
 */
public class VgpuSchedulerSpaceShared extends VgpuScheduler {

	/**
	 * Instantiates a new vgpu space-shared scheduler.
	 * 
	 * @param videoCardType       type of the video card associated with this
	 *                            vgpuScheduler
	 * @param pgpuList            list of video card's pgpus
	 * @param pgpuSelectionPolicy vgpu to pgpu allocation policy
	 */
	public VgpuSchedulerSpaceShared(String videoCardType, List<Pgpu> pgpuList,
			PgpuSelectionPolicy pgpuSelectionPolicy) {
		super(videoCardType, pgpuList, pgpuSelectionPolicy);
	}

	@Override
	public void deallocatePgpuForVgpu(Vgpu vgpu) {
		Pgpu pgpu = getPgpuForVgpu(vgpu);
		pgpu.getGddramProvisioner().deallocateGddramForVgpu(vgpu);
		pgpu.getBwProvisioner().deallocateBwForVgpu(vgpu);
		getPgpuVgpuMap().get(pgpu).remove(vgpu);
		for (Pe pe : getVgpuPeMap().get(vgpu)) {
			pe.getPeProvisioner().deallocateMipsForVm(vgpu.getVm());
		}
		getVgpuPeMap().remove(vgpu);
		getMipsMap().remove(vgpu);
		vgpu.setCurrentAllocatedMips(null);
	}

	@Override
	public boolean isSuitable(Pgpu pgpu, Vgpu vgpu) {
		final List<Double> mipsShare = vgpu.getCurrentRequestedMips();
		final int gddramShare = vgpu.getCurrentRequestedGddram();
		final long bwShare = vgpu.getCurrentRequestedBw();

		if (!pgpu.getGddramProvisioner().isSuitableForVgpu(vgpu, gddramShare)
				|| !pgpu.getBwProvisioner().isSuitableForVgpu(vgpu, bwShare)) {
			return false;
		}
		List<Pe> pgpuPes = pgpu.getPeList();
		int freePes = CollectionUtils.countMatches(pgpuPes, new Predicate() {
			@Override
			public boolean evaluate(Object arg) {
				Pe pe = (Pe) arg;
				if (pe.getPeProvisioner().getTotalAllocatedMips() == 0) {
					return true;
				}
				return false;
			}
		});

		if (freePes < mipsShare.size()) {
			return false;
		}

		for (int i = 0; i < mipsShare.size(); i++) {
			if (mipsShare.get(i) > pgpuPes.get(i).getPeProvisioner().getAvailableMips()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean allocatePgpuForVgpu(Pgpu pgpu, Vgpu vgpu, List<Double> mipsShare, int gddramShare, long bwShare) {
		final Vm vm = vgpu.getVm();

		if (!isSuitable(pgpu, vgpu)) {
			return false;
		}

		// allocate gddram
		pgpu.getGddramProvisioner().allocateGddramForVgpu(vgpu, gddramShare);
		// allocated gddram bandwidth
		pgpu.getBwProvisioner().allocateBwForVgpu(vgpu, bwShare);
		// and finally, select pes
		List<Pe> selectedPgpuPes = (List<Pe>) CollectionUtils.select(pgpu.getPeList(), new Predicate() {
			@Override
			public boolean evaluate(Object arg) {
				Pe pe = (Pe) arg;
				if (pe.getPeProvisioner().getTotalAllocatedMips() == 0) {
					return true;
				}
				return false;
			}
		});
		vgpu.setPeMips(pgpu.getPeList().get(0).getMips());
		mipsShare = vgpu.getCurrentRequestedMips();
		List<Pe> selectedPes = new ArrayList<Pe>();
		for (int i = 0; i < mipsShare.size(); i++) {
			Pe pe = selectedPgpuPes.get(i);
			pe.getPeProvisioner().allocateMipsForVm(vm, mipsShare.get(i));
			selectedPes.add(pe);
		}
		getPgpuVgpuMap().get(pgpu).add(vgpu);
		getVgpuPeMap().put(vgpu, selectedPes);
		getMipsMap().put(vgpu, mipsShare);
		vgpu.setCurrentAllocatedMips(mipsShare);
		return true;
	}

}
