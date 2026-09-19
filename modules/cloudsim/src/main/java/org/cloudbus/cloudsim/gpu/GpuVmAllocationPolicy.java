package org.cloudbus.cloudsim.gpu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * {@link GpuVmAllocationPolicy} extends {@link VmAllocationPolicy} to support
 * GPU-enabled VM placement. Subclasses place a {@link GpuVm} and its
 * {@link Vgpu} together by implementing {@link #allocateHostForVm(Vm)}.
 *
 * @author Ahmad Siavashi
 *
 */
public abstract class GpuVmAllocationPolicy extends VmAllocationPolicy {

	/**
	 * GPU-equipped hosts
	 */
	private List<GpuHost> gpuHostList;

	/**
	 * Holds which GpuHost a vGPU is allocated on
	 */
	private Map<Vgpu, GpuHost> vgpuHosts;

	/**
	 * @param list all data center hosts
	 */
	public GpuVmAllocationPolicy(List<? extends Host> list) {
		super(list);
		setGpuHostList(getHostList());
		setVgpuHosts(new HashMap<Vgpu, GpuHost>());
	}

	/**
	 * Allocates a host (and a GPU, if the VM has a vGPU) for the given VM.
	 *
	 * @param vm the VM to allocate
	 * @return true if the VM was allocated
	 */
	@Override
	public abstract boolean allocateHostForVm(Vm vm);

	@Override
	public boolean allocateHostForGuest(GuestEntity guest) {
		return allocateHostForVm((Vm) guest);
	}

	/**
	 * GPU-aware policies select the host and the GPU at the same time in
	 * {@link #allocateHostForVm(Vm)}, so a host cannot be looked up on its own.
	 */
	@Override
	public HostEntity findHostForGuest(GuestEntity guest) {
		throw new UnsupportedOperationException(
				getClass().getSimpleName() + " selects hosts in allocateHostForVm(Vm); use allocateHostForGuest instead.");
	}

	/**
	 * Allocates the given host and GPUs of that host for the vGPUs of the VM. If
	 * a vGPU cannot be allocated, the host allocation is rolled back.
	 */
	@Override
	public boolean allocateHostForGuest(GuestEntity guest, HostEntity host) {
		GpuVm gpuVm = (GpuVm) guest;
		if (!allocateHostForVm(gpuVm, (Host) host)) {
			return false;
		} else if (allocateGpusForVm(gpuVm, (GpuHost) host)) {
			return true;
		}
		deallocateHostForVm(gpuVm);
		return false;
	}

	/**
	 * Allocates GPUs of the host for all vGPUs of the VM. If one fails, the vGPUs
	 * already allocated are rolled back.
	 */
	protected boolean allocateGpusForVm(GpuVm vm, GpuHost host) {
		List<Vgpu> allocated = new ArrayList<Vgpu>();
		for (Vgpu vgpu : vm.getVgpuList()) {
			if (!allocateGpuForVgpu(vgpu, host)) {
				allocated.forEach(this::deallocateGpuForVgpu);
				return false;
			}
			allocated.add(vgpu);
		}
		return true;
	}

	/**
	 * A VM is allocated only if all its vGPUs are allocated. Otherwise, its
	 * allocation is rolled back.
	 *
	 * @param vm        the VM
	 * @param allocated whether the policy allocated the VM
	 * @return true if the VM and all its vGPUs are allocated
	 */
	public boolean ensureVgpusAllocated(GpuVm vm, boolean allocated) {
		if (allocated && !getVgpuHosts().keySet().containsAll(vm.getVgpuList())) {
			deallocateHostForVm(vm);
			return false;
		}
		return allocated;
	}

	/**
	 * Allocates the given host for the VM without allocating its vGPU.
	 */
	@Override
	public boolean allocateHostForVm(Vm vm, Host host) {
		if (getVmTable().containsKey(vm.getUid())) {
			return false;
		}
		return super.allocateHostForGuest(vm, host);
	}

	/**
	 * Deallocates the host and the allocated vGPUs of the VM.
	 */
	@Override
	public void deallocateHostForGuest(GuestEntity guest) {
		for (Vgpu vgpu : ((GpuVm) guest).getVgpuList()) {
			if (getVgpuHosts().containsKey(vgpu)) {
				deallocateGpuForVgpu(vgpu);
			}
		}
		super.deallocateHostForGuest(guest);
	}

	protected void deallocateGpuForVgpu(Vgpu vgpu) {
		getVgpuHosts().remove(vgpu).vgpuDestroy(vgpu);
	}

	protected boolean allocateGpuForVgpu(Vgpu vgpu, GpuHost gpuHost) {
		if (gpuHost.isGpuEquipped() && !getVgpuHosts().containsKey(vgpu)) {
			boolean result = gpuHost.vgpuCreate(vgpu);
			if (result) {
				getVgpuHosts().put(vgpu, gpuHost);
				return true;
			}
		}
		return false;
	}

	protected boolean allocateGpuForVgpu(Vgpu vgpu, GpuHost gpuHost, Pgpu pgpu) {
		if (!getVgpuHosts().containsKey(vgpu)) {
			boolean result = gpuHost.vgpuCreate(vgpu, pgpu);
			if (result) {
				getVgpuHosts().put(vgpu, gpuHost);
				return true;
			}
		}
		return false;
	}

	protected boolean allocateGpuHostForVgpu(Vgpu vgpu, GpuHost gpuHost, Pgpu pgpu) {
		if (!getVgpuHosts().containsKey(vgpu)) {
			boolean result = gpuHost.vgpuCreate(vgpu, pgpu);
			if (result) {
				getVgpuHosts().put(vgpu, gpuHost);
				return true;
			}
		}
		return false;
	}

	/**
	 * Allocates Hosts for a set of {@link GpuVm}s.
	 *
	 * @param set of VMs
	 * @return a list of vm-result pairs
	 */
	public Map<GpuVm, Boolean> allocateHostForVms(List<GpuVm> vms) {
		Map<GpuVm, Boolean> results = new HashMap<GpuVm, Boolean>();
		for (GpuVm vm : vms) {
			boolean result = allocateHostForVm(vm);
			results.put(vm, result);
		}
		return results;
	}

	/**
	 * @return the map between each VM UID and its allocated host
	 */
	protected Map<String, HostEntity> getVmTable() {
		return getGuestTable();
	}

	protected List<GpuHost> getGpuHostList() {
		return gpuHostList;
	}

	protected void setGpuHostList(List<GpuHost> gpuHostList) {
		this.gpuHostList = new ArrayList<GpuHost>();
		for (GpuHost host : gpuHostList) {
			if (host.isGpuEquipped()) {
				getGpuHostList().add(host);
			}
		}
	}

	public Map<Vgpu, GpuHost> getVgpuHosts() {
		return vgpuHosts;
	}

	protected void setVgpuHosts(Map<Vgpu, GpuHost> vgpuHosts) {
		this.vgpuHosts = vgpuHosts;
	}

}
