package org.cloudbus.cloudsim.gpu.hardware_assisted.grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

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

/**
 * The best-fit policy is implemented according to, A. K. Kulkarni, B. Annappa,
 * GPU-aware resource management in heterogeneous cloud data centers, The
 * Journal of Supercomputing (2021) 1–28.
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
public class GridGpuVmAllocationPolicyBestFit extends GpuVmAllocationPolicy {

	private List<Host> nonGpuHostList = new ArrayList<>();

	public GridGpuVmAllocationPolicyBestFit(List<? extends Host> list) {
		super(list);
		// Create nonGpuHost list
		for (Host host : list) {
			GpuHost pm = (GpuHost) host;
			if (!pm.isGpuEquipped()) {
				nonGpuHostList.add(pm);
			}
		}
	}

	@Override
	public boolean allocateHostForVm(Vm vm) {
		if (!getVmTable().containsKey(vm.getUid())) {
			GpuVm gpuVm = (GpuVm) vm;
			// Case 1 - VM with GPU tasks
			if (gpuVm.hasVgpu()) {
				memoryAwareSortGpuHost(getGpuHostList());
				for (GpuHost pm : getGpuHostList()) {
					if (pm.isSuitableForVm(gpuVm)) {
						if (allocateGpusForVm(gpuVm, pm)) {
							if (allocateHostForVm(gpuVm, pm)) {
								return true;
							}
							gpuVm.getVgpuList().forEach(this::deallocateGpuForVgpu);
						}
					}
				}
				// Case 2 - VM with no GPU task
			} else {
				// Search nonGpuHost for nonGpuVm
				for (Host host : nonGpuHostList) {
					if (allocateHostForVm(gpuVm, host)) {
						return true;
					}
				}
				// Search GpuHost for nonGpuVm
				for (GpuHost pm : getGpuHostList()) {
					if (allocateHostForVm(gpuVm, pm)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	protected boolean allocateGpuForVgpu(Vgpu vgpu, GpuHost gpuHost) {
		if (!getVgpuHosts().containsKey(vgpu)) {
			for (VideoCard videoCard : gpuHost.getVideoCardAllocationPolicy().getVideoCards()) {
				// The vGPU scheduler checks the vGPU placement rules
				for (Pgpu pgpu : videoCard.getVgpuScheduler().getPgpuVgpuMap().keySet()) {
					boolean result = gpuHost.vgpuCreate(vgpu, pgpu);
					if (result) {
						getVgpuHosts().put(vgpu, gpuHost);
						return true;
					}
				}
			}
		}
		return false;
	}

	protected void memoryAwareSortGpuHost(List<GpuHost> gpuHostList) {
		Collections.sort(gpuHostList, new Comparator<GpuHost>() {
			@Override
			public int compare(GpuHost h1, GpuHost h2) {
				Integer minGpuMemory1 = h1.getVideoCardAllocationPolicy().getVideoCards().stream()
						.map(x -> x.getVgpuScheduler().getMinAvailableMemory()).mapToInt(v -> v).min()
						.orElseThrow(NoSuchElementException::new);
				Integer minGpuMemory2 = h2.getVideoCardAllocationPolicy().getVideoCards().stream()
						.map(x -> x.getVgpuScheduler().getMinAvailableMemory()).mapToInt(v -> v).min()
						.orElseThrow(NoSuchElementException::new);
				return Integer.compare(minGpuMemory1, minGpuMemory2);
			}
		});
	}

}
