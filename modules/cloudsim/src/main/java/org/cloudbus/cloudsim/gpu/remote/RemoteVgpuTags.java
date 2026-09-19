package org.cloudbus.cloudsim.gpu.remote;

import org.cloudbus.cloudsim.gpu.Vgpu;

/**
 * 
 * The modes supported in remote GPU virtualization.
 * 
 * @author Ahmad Siavashi
 *
 */
public class RemoteVgpuTags {

	/**
	 * The tenancy of a {@link RemoteVgpu}; it tells whether the vGPU may be
	 * allocated on a host other than the host of its VM (remote) or not (local),
	 * and whether it may share its pGPU (shared) or not (exclusive).
	 */
	public enum Tenancy {

		/** The vGPU may be on any host and does not share its pGPU. */
		REMOTE_EXCLUSIVE("RE"),

		/** The vGPU may be on any host and may share its pGPU. */
		REMOTE_SHARED("RS"),

		/** The vGPU must be on the host of its VM and does not share its pGPU. */
		LOCAL_EXCLUSIVE("LE"),

		/** The vGPU must be on the host of its VM and may share its pGPU. */
		LOCAL_SHARED("LS");

		/** The short name of the tenancy */
		private final String shortName;

		Tenancy(String shortName) {
			this.shortName = shortName;
		}

		@Override
		public String toString() {
			return shortName;
		}
	}

	/**
	 * @return the tenancy of the vGPU, which must be a {@link RemoteVgpu}; there is
	 *         no default tenancy
	 */
	public static Tenancy getTenancy(Vgpu vgpu) {
		if (!(vgpu instanceof RemoteVgpu)) {
			throw new IllegalArgumentException("Vgpu #" + vgpu.getId() + " has no tenancy; use a RemoteVgpu");
		}
		return ((RemoteVgpu) vgpu).getTenancy();
	}

	public static boolean isLocal(Vgpu vgpu) {
		Tenancy tenancy = getTenancy(vgpu);
		switch (tenancy) {
		case LOCAL_EXCLUSIVE:
		case LOCAL_SHARED:
			return true;
		default:
			return false;
		}
	}
	
	public static boolean isRemote(Vgpu vgpu) {
		return !isLocal(vgpu);
	}
	
	public static boolean isShared(Vgpu vgpu) {
		Tenancy tenancy = getTenancy(vgpu);
		switch (tenancy) {
		case LOCAL_SHARED:
		case REMOTE_SHARED:
			return true;
		default:
			return false;
		}
	}

	public static boolean isExclusive(Vgpu vgpu) {
		return !isShared(vgpu);
	}
	
}
