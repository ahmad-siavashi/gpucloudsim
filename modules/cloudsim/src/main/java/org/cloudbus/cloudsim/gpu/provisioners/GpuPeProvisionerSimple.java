package org.cloudbus.cloudsim.gpu.provisioners;

import java.util.List;

import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;

/**
 * GpuPeProvisionerSimple is an extension of {@link PeProvisionerSimple} which
 * keys the PE table by uid rather than by guest. A Vgpu is not a guest of its
 * Pgpu's PEs, so the Vgpu's uid is the only handle the vGPU schedulers have.
 * Each Pgpu's PE has to have its own instance of a GpuPeProvisionerSimple.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class GpuPeProvisionerSimple extends PeProvisionerSimple {

	/**
	 * Instantiates a new GPU pe provisioner simple.
	 * 
	 * @param availableMips
	 *            The total mips capacity of the PE that the provisioner can
	 *            allocate to Vgpus.
	 */
	public GpuPeProvisionerSimple(double availableMips) {
		super(availableMips);
	}

	/**
	 * Gets total allocated MIPS for a given uid.
	 * 
	 * @param uid
	 *            the uid of the guest
	 * @return total allocated MIPS
	 */
	public double getTotalAllocatedMipsForGuest(String uid) {
		List<Double> allocatedMips = getPeTable().get(uid);

		if (allocatedMips != null) {
			double totalAllocatedMips = 0.0;
			for (double mips : allocatedMips) {
				totalAllocatedMips += mips;
			}
			return totalAllocatedMips;
		}
		return 0;
	}

	/**
	 * Releases all virtual PEs allocated to a given uid.
	 * 
	 * @param uid
	 *            the uid of the guest
	 */
	public void deallocateMipsForGuest(String uid) {
		List<Double> allocatedMips = getPeTable().remove(uid);

		if (allocatedMips != null) {
			for (double mips : allocatedMips) {
				setAvailableMips(getAvailableMips() + mips);
			}
		}
	}

}
