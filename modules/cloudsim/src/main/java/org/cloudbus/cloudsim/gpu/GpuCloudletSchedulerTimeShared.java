package org.cloudbus.cloudsim.gpu;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;

/**
 * {@link GpuCloudletSchedulerTimeShared} extends
 * {@link CloudletSchedulerTimeShared} to schedule {@link GpuCloudlet}s.
 *
 * @author Ahmad Siavashi
 *
 */
public class GpuCloudletSchedulerTimeShared extends CloudletSchedulerTimeShared implements GpuCloudletScheduler {

	private List<GpuTask> gpuTaskList;

	/**
	 * {@link CloudletSchedulerTimeShared} with GpuCloudlet support. Assumes all PEs have same MIPS
	 * capacity.
	 */
	public GpuCloudletSchedulerTimeShared() {
		super();
		setGpuTaskList(new ArrayList<GpuTask>());
	}

	@Override
	public void cloudletFinish(Cloudlet cl) {
		GpuCloudlet gcl = (GpuCloudlet) cl;
		if (!gcl.hasGpuTask()) {
			super.cloudletFinish(cl);
		} else {
			// the host portion is done; hold the cloudlet until its GpuTask finishes
			getGpuTaskList().add(gcl.getGpuTask());
			gcl.updateStatus(Cloudlet.CloudletStatus.PAUSED);
			getCloudletPausedList().add(gcl);
		}
	}

	protected List<GpuTask> getGpuTaskList() {
		return gpuTaskList;
	}

	protected void setGpuTaskList(List<GpuTask> gpuTaskList) {
		this.gpuTaskList = gpuTaskList;
	}

	@Override
	public boolean hasGpuTask() {
		return !getGpuTaskList().isEmpty();
	}

	@Override
	public GpuTask getNextGpuTask() {
		if (hasGpuTask()) {
			return getGpuTaskList().remove(0);
		}
		return null;
	}

	@Override
	public boolean notifyGpuTaskCompletion(GpuTask gt) {
		for (Cloudlet cl : getCloudletPausedList()) {
			GpuCloudlet gcl = (GpuCloudlet) cl;
			if (gcl.getGpuTask() == gt) {
				gcl.updateStatus(Cloudlet.CloudletStatus.SUCCESS);
				gcl.finalizeCloudlet();
				return true;
			}
		}
		return false;
	}

}
