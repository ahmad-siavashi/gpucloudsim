package org.cloudbus.cloudsim.gpu.hardware_assisted.grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.gpu.GpuHost;
import org.cloudbus.cloudsim.gpu.GpuVm;
import org.cloudbus.cloudsim.gpu.GpuVmAllocationPolicy;
import org.cloudbus.cloudsim.gpu.Pgpu;
import org.cloudbus.cloudsim.gpu.Vgpu;
import org.cloudbus.cloudsim.gpu.VideoCard;
import org.cloudbus.cloudsim.gpu.allocation.VideoCardAllocationPolicy;
import org.cloudbus.cloudsim.gpu.allocation.VideoCardAllocationPolicyNull;
import org.cloudbus.cloudsim.gpu.selection.PgpuSelectionPolicy;
import org.cloudbus.cloudsim.gpu.selection.PgpuSelectionPolicyNull;

public class GridGpuVmAllocationPolicyVird extends GpuVmAllocationPolicy {

	/**
	 * /** The VIRD policy is implemented according to, 
	 * A. Garg, U. Kurkure, H. Sivaraman, L. Vu, Virtual machine placement solution 
	 * for VGPU enabled clouds, in: 2019 International Conference on High Performance 
	 * Computing & Simulation (HPCS), IEEE, 2019, pp. 897–903.
	 * 
	 * <b>Note</b>: This class performs a global placement, so classes required for
	 * hierarchical placement that extend or implement
	 * {@link VideoCardAllocationPolicy} and {@link PgpuSelectionPolicy} can be set
	 * to {@link VideoCardAllocationPolicyNull} and {@link PgpuSelectionPolicyNull}
	 * respectively. Otherwise, they are ignored.
	 * 
	 * @author Ahmad Siavashi
	 *
	 */

	private Map<Pgpu, GpuHost> pgpuGpuHostMap = new HashMap<>();
	private List<Pair<Pgpu, Integer>> pgpuListS = new ArrayList<>();
	private List<Pair<Pgpu, Integer>> pgpuListU = new ArrayList<>();

	private List<Integer> pgpuProfiles = Arrays.asList(512, 1024, 2048, 4096, 8192);

	protected static Integer EMPTY = 0;

	public GridGpuVmAllocationPolicyVird(List<? extends Host> list) {
		super(list);
		for (GpuHost gpuHost : getGpuHostList()) {
			for (VideoCard videoCard : gpuHost.getVideoCardAllocationPolicy().getVideoCards()) {
				for (Pgpu pgpu : videoCard.getVgpuScheduler().getPgpuList()) {
					pgpuListU.add(Pair.of(pgpu, EMPTY));
					pgpuGpuHostMap.put(pgpu, gpuHost);
				}
			}
		}
	}

	@Override
	protected void deallocateGpuForVgpu(Vgpu vgpu) {
		Pgpu pgpu = vgpu.getVideoCard().getVgpuScheduler().getPgpuForVgpu(vgpu);
		super.deallocateGpuForVgpu(vgpu);
		if (pgpu.getGddramProvisioner().getAvailableGddram() == pgpu.getGddramProvisioner().getGddram()) {
			Pair<Pgpu, Integer> pgpuEntry = pgpuListS.stream().filter(x -> x.getKey() == pgpu).findFirst().get();
			pgpuListS.remove(pgpuEntry);
			pgpuListU.add(Pair.of(pgpu, EMPTY));
		}
	}

	@Override
	public Map<GpuVm, Boolean> allocateHostForVms(List<GpuVm> vms) {
		Map<GpuVm, Boolean> results = new HashMap<GpuVm, Boolean>();
		// Sort VMs in descending order according to associated vGPU
		sortVms(vms);
		sortPgpusListAsc(pgpuListS);
		for (GpuVm vm : vms) {
			boolean result = false;

			if (!vm.hasVgpu()) {
				for (Host host : this.<Host>getHostList()) {
					result = allocateHostForVm(vm, host);
					if (result) {
						break;
					}
				}
				results.put(vm, result);
				continue;
			}

			int vgpuGddram = vm.getVgpu().getGddram();

			// List of used GPUs
			for (Pair<Pgpu, Integer> pgpuEntity : pgpuListS) {
				Pgpu pgpu = pgpuEntity.getKey();
				Integer pgpuProfile = pgpuEntity.getValue();
				if (pgpu.getGddramProvisioner().getAvailableGddram() >= pgpuProfile
						&& vm.getVgpu().getGddram() <= pgpuProfile) {
					// Allocate VM on Pgpu's host
					vm.getVgpu().setGddram(pgpuProfile);
					result = allocateVmOnPgpuHost(vm, pgpu);
					if (result) {
						results.put(vm, result);
						break;
					}
				}
				vm.getVgpu().setGddram(vgpuGddram);
			}

			if (result) {
				continue;
			}

			Pair<Pgpu, Integer> selectedPgpuEntity = null;
			// List of unused GPUs
			for (Pair<Pgpu, Integer> pgpuEntity : pgpuListU) {
				Pgpu pgpu = pgpuEntity.getKey();
				Integer pgpuProfile = getPgpuProfiles().stream().filter(p -> p >= vm.getVgpu().getGddram()).findFirst()
						.orElse(EMPTY);
				if (pgpuProfile != EMPTY && pgpu.getGddramProvisioner().getAvailableGddram() >= pgpuProfile) {
					// Allocate VM on Pgpu's host
					vm.getVgpu().setGddram(pgpuProfile);
					result = allocateVmOnPgpuHost(vm, pgpu);
					if (result) {
						selectedPgpuEntity = pgpuEntity;
						break;
					}
				}
				vm.getVgpu().setGddram(vgpuGddram);
			}

			if (selectedPgpuEntity != null) {
				pgpuListU.remove(selectedPgpuEntity);
				pgpuListS.add(Pair.of(selectedPgpuEntity.getKey(), vm.getVgpu().getGddram()));
			}

			results.put(vm, result);
		}
		return results;

	}

	protected boolean allocateVmOnPgpuHost(GpuVm vm, Pgpu pgpu) {
		GpuHost pgpuHost = pgpuGpuHostMap.get(pgpu);
		boolean result = allocateHostForVm(vm, pgpuHost);
		if (result) {
			result = allocateGpuHostForVgpu(vm.getVgpu(), pgpuHost, pgpu);
			if (result) {
				return true;
			}
			deallocateHostForVm(vm);
		}
		return false;
	}

	protected void sortPgpusListAsc(List<Pair<Pgpu, Integer>> pgpuList) {
		Collections.sort(pgpuList, new Comparator<Pair<Pgpu, Integer>>() {
			public int compare(Pair<Pgpu, Integer> p1, Pair<Pgpu, Integer> p2) {
				return Integer.compare(p1.getValue(), p2.getValue());
			};
		});
	}

	/**
	 * Sort VMs in decreasing order according to their attached vGPU
	 * 
	 * @param vms
	 */
	protected void sortVms(List<GpuVm> vms) {
		Collections.sort(vms, Collections.reverseOrder(new Comparator<GpuVm>() {
			@Override
			public int compare(GpuVm vm1, GpuVm vm2) {
				int vgpu1gddram = !vm1.hasVgpu() ? 0 : vm1.getVgpu().getGddram();
				int vgpu2gddram = !vm2.hasVgpu() ? 0 : vm2.getVgpu().getGddram();
				return Integer.compare(vgpu1gddram, vgpu2gddram);
			}
		}));
	}

	@Override
	public boolean allocateHostForVm(Vm vm) {
		throw new NotImplementedException("not implemented");
	}

	@Override
	protected boolean allocateGpuForVgpu(Vgpu vgpu, GpuHost gpuHost) {
		throw new NotImplementedException("not implemented");
	}

	public List<Integer> getPgpuProfiles() {
		return pgpuProfiles;
	}

	public void setPgpuProfiles(List<Integer> pgpuProfiles) {
		this.pgpuProfiles = pgpuProfiles;
		Collections.sort(this.pgpuProfiles);
	}

}
