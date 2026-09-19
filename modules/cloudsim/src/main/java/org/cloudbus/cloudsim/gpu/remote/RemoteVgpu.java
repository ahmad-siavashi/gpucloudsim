package org.cloudbus.cloudsim.gpu.remote;

import org.cloudbus.cloudsim.gpu.GpuTaskScheduler;
import org.cloudbus.cloudsim.gpu.Vgpu;
import org.cloudbus.cloudsim.gpu.remote.RemoteVgpuTags.Tenancy;

/**
 * A {@link Vgpu} with a tenancy, which tells whether it may be allocated on a
 * host other than the host of its VM and whether it may share its GPU (see
 * {@link Tenancy}).
 * 
 * @author Ahmad Siavashi
 * 
 */
public class RemoteVgpu extends Vgpu {

	/** The vGPU tenancy */
	private Tenancy tenancy;

	/**
	 * @param tenancy the vGPU tenancy, e.g. {@link Tenancy#REMOTE_SHARED}
	 * 
	 * @see Vgpu#Vgpu(int, double, int, int, long, String, GpuTaskScheduler, int)
	 */
	public RemoteVgpu(int vgpuId, double mips, int numberOfPes, int gddram, long bw, String type, Tenancy tenancy,
			GpuTaskScheduler scheduler, int PCIeBw) {
		super(vgpuId, mips, numberOfPes, gddram, bw, type, scheduler, PCIeBw);
		setTenancy(tenancy);
	}

	public Tenancy getTenancy() {
		return tenancy;
	}

	protected void setTenancy(Tenancy tenancy) {
		this.tenancy = tenancy;
	}
}
