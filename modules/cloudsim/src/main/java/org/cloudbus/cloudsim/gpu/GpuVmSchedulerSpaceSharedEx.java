package org.cloudbus.cloudsim.gpu;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.core.GuestEntity;

/**
 * 
 * A modification to {@link VmSchedulerSpaceShared} in which requested
 * MIPS of the VM is ignored and the whole MIPS of allocated PEs is considered.
 * 
 * @author Ahmad Siavashi
 *
 */
public class GpuVmSchedulerSpaceSharedEx extends VmSchedulerSpaceShared {

	public GpuVmSchedulerSpaceSharedEx(List<? extends Pe> pelist) {
		super(pelist);
	}

	@Override
	public boolean allocatePesForGuest(GuestEntity guest, List<Double> mipsShare) {
		Vm vm = (Vm) guest;
		// if there is no enough free PEs, fails
		if (getFreePes().size() < vm.getNumberOfPes()) {
			return false;
		}

		List<Pe> selectedPes = new ArrayList<Pe>();
		Iterator<Pe> peIterator = getFreePes().iterator();
		Pe pe = peIterator.next();
		double totalMips = 0;
		for (int i = 0; i < vm.getNumberOfPes(); i++) {
			selectedPes.add(pe);
			totalMips += pe.getPeProvisioner().getMips();
			if (!peIterator.hasNext()) {
				break;
			}
			pe = peIterator.next();
		}
		if (vm.getNumberOfPes() > selectedPes.size()) {
			return false;
		}
		List<Double> allocatedMips = new ArrayList<>();
		for (Pe selectedPe : selectedPes) {
			allocatedMips.add(selectedPe.getPeProvisioner().getMips());
		}

		vm.setCurrentAllocatedMips(allocatedMips);

		getFreePes().removeAll(selectedPes);

		getPeAllocationMap().put(vm.getUid(), selectedPes);
		getMipsMapAllocated().put(vm.getUid(), allocatedMips);
		setAvailableMips(getAvailableMips() - totalMips);
		return true;
	}

}
