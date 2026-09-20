package org.cloudbus.cloudsim.gpu;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Consts;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.util.MathUtil;

/**
 * GpuTaskSchedulerLeftover implements a policy of scheduling performed by a
 * {@link Vgpu} to run its {@link GpuTask GpuTasks}. The implemented policy is
 * leftover. A GpuTask occupies GPU resources until the end of its execution.
 * The simultaneous execution of other tasks is only possible if enough
 * resources are left unused; otherwise they will be in a waiting list. Even
 * though tasks may wait for GPU, their data transfer happens as soon as there
 * will be enough memory available in the GPU.
 * 
 * @author Ahmad Siavashi
 */
public class GpuTaskSchedulerLeftover extends GpuTaskScheduler {
	/** The number of used PEs. It holds virtual PE ids. */
	protected List<Integer> usedPes;

	/**
	 * Creates a new GpuTaskSchedulerLeftover object
	 */
	public GpuTaskSchedulerLeftover() {
		super();
		setUsedPes(new ArrayList<Integer>());
	}

	/**
	 * Allocates unused PEs to the task until it reaches its PEs limit. A limit of
	 * zero means no limit.
	 * 
	 * @return $true if a PE is allocated; $false otherwise
	 */
	protected boolean allocatePes(ResGpuTask rgt) {
		boolean allocated = false;
		final int pesLimit = rgt.getGpuTask().getPesLimit();
		for (int i = 0; i < getCurrentMipsShare().size(); i++) {
			if (pesLimit > 0 && rgt.getPeIdList().size() >= pesLimit) {
				break;
			}
			if (!getUsedPes().contains(i)) {
				rgt.setPeId(i);
				getUsedPes().add(i);
				allocated = true;
			}
		}
		return allocated;
	}

	/**
	 * Releases the PEs of a task that leaves the execution list without finishing.
	 */
	protected void releasePes(ResGpuTask rgt) {
		getUsedPes().removeAll(rgt.getPeIdList());
		rgt.getPeIdList().clear();
	}

	protected boolean redistributeGpuTaskMips(ResGpuTask rgt) {
		return allocatePes(rgt);
	}

	@Override
	public double updateGpuTaskProcessing(double currentTime, List<Double> mipsShare) {
		setCurrentMipsShare(mipsShare);
		double timeSpan = currentTime - getPreviousTime(); // time since last
		for (ResGpuTask rcl : getTaskExecList()) {
			rcl.updateTaskFinishedSoFar((long) (getTotalCurrentAvailableMipsForTask(rcl, mipsShare)
					* rcl.getGpuTask().getUtilizationOfGpu(currentTime) * timeSpan * Consts.MILLION));
		}

		// no more tasks in this scheduler
		if (getTaskExecList().isEmpty() && getTaskWaitingList().isEmpty()) {
			setPreviousTime(currentTime);
			return 0.0;
		}

		// update each task
		int finished = 0;
		List<ResGpuTask> toRemove = new ArrayList<ResGpuTask>();
		for (ResGpuTask rcl : getTaskExecList()) {
			// finished anyway, rounding issue...
			if (rcl.getRemainingTaskLength() == 0) {
				toRemove.add(rcl);
				taskFinish(rcl);
				finished++;
			}
		}
		getTaskExecList().removeAll(toRemove);
		
		// Redistribute freed resources
		if(!toRemove.isEmpty()) {
			for (ResGpuTask rcl : getTaskExecList()) {
				redistributeGpuTaskMips(rcl);
			}
		}

		// while there are unused PEs, add tasks from the waiting list
		while (!getTaskWaitingList().isEmpty() && getCurrentMipsShare().size() > getUsedPes().size()) {
			ResGpuTask rcl = getTaskWaitingList().remove(0);
			rcl.setTaskStatus(GpuTask.INEXEC);
			allocatePes(rcl);
			getTaskExecList().add(rcl);
		}

		// estimate finish time of tasks in the execution queue
		double nextEvent = Double.MAX_VALUE;
		for (ResGpuTask rcl : getTaskExecList()) {
			double estimatedFinishTime = currentTime + getEstimatedFinishTime(rcl);
			if (estimatedFinishTime < nextEvent) {
				nextEvent = estimatedFinishTime;
			}
		}
		setPreviousTime(currentTime);
		return nextEvent;
	}

	@Override
	public GpuTask taskCancel(int taskId) {
		// First, looks in the finished queue
		for (ResGpuTask rcl : getTaskFinishedList()) {
			if (rcl.getTaskId() == taskId) {
				getTaskFinishedList().remove(rcl);
				return rcl.getGpuTask();
			}
		}

		// Then searches in the exec list
		for (ResGpuTask rcl : getTaskExecList()) {
			if (rcl.getTaskId() == taskId) {
				getTaskExecList().remove(rcl);
				if (rcl.getRemainingTaskLength() == 0) {
					taskFinish(rcl);
				} else {
					rcl.setTaskStatus(GpuTask.CANCELED);
					releasePes(rcl);
				}
				return rcl.getGpuTask();
			}
		}

		// Now, looks in the paused queue
		for (ResGpuTask rcl : getTaskPausedList()) {
			if (rcl.getTaskId() == taskId) {
				getTaskPausedList().remove(rcl);
				return rcl.getGpuTask();
			}
		}

		// Finally, looks in the waiting list
		for (ResGpuTask rcl : getTaskWaitingList()) {
			if (rcl.getTaskId() == taskId) {
				rcl.setTaskStatus(GpuTask.CANCELED);
				getTaskWaitingList().remove(rcl);
				return rcl.getGpuTask();
			}
		}

		return null;

	}

	@Override
	public boolean taskPause(int taskId) {
		boolean found = false;
		int position = 0;

		// first, looks for the task in the exec list
		for (ResGpuTask rcl : getTaskExecList()) {
			if (rcl.getTaskId() == taskId) {
				found = true;
				break;
			}
			position++;
		}

		if (found) {
			// moves to the paused list
			ResGpuTask rgl = getTaskExecList().remove(position);
			if (rgl.getRemainingTaskLength() == 0) {
				taskFinish(rgl);
			} else {
				rgl.setTaskStatus(GpuTask.PAUSED);
				releasePes(rgl);
				getTaskPausedList().add(rgl);
			}
			return true;

		}

		// now, look for the task in the waiting list
		position = 0;
		found = false;
		for (ResGpuTask rcl : getTaskWaitingList()) {
			if (rcl.getTaskId() == taskId) {
				found = true;
				break;
			}
			position++;
		}

		if (found) {
			// moves to the paused list
			ResGpuTask rgl = getTaskWaitingList().remove(position);
			if (rgl.getRemainingTaskLength() == 0) {
				taskFinish(rgl);
			} else {
				rgl.setTaskStatus(GpuTask.PAUSED);
				getTaskPausedList().add(rgl);
			}
			return true;

		}

		return false;
	}

	@Override
	public void taskFinish(ResGpuTask rcl) {
		rcl.setTaskStatus(GpuTask.SUCCESS);
		rcl.finalizeTask();
		List<Integer> pesToRemove = new ArrayList<>();
		for (Integer peId : rcl.getPeIdList()) {
			pesToRemove.add(peId);
		}
		getUsedPes().removeAll(pesToRemove);
		getTaskFinishedList().add(rcl);
	}

	@Override
	public double taskResume(int taskId) {
		boolean found = false;
		int position = 0;

		// look for the task in the paused list
		for (ResGpuTask rcl : getTaskPausedList()) {
			if (rcl.getTaskId() == taskId) {
				found = true;
				break;
			}
			position++;
		}

		if (found) {
			ResGpuTask rcl = getTaskPausedList().remove(position);
			int numberOfCurrentAvailablePEs = getCurrentMipsShare().size() - getUsedPes().size();
			// it can go to the exec list
			if (numberOfCurrentAvailablePEs > 0) {
				rcl.setTaskStatus(GpuTask.INEXEC);
				allocatePes(rcl);

				getTaskExecList().add(rcl);

				return getEstimatedFinishTime(rcl);

			} else {// no enough free PEs: go to the waiting queue
				rcl.setTaskStatus(GpuTask.QUEUED);

				getTaskWaitingList().add(rcl);
				return 0.0;
			}

		}

		// not found in the paused list: either it is in in the queue, executing
		// or not exist
		return 0.0;

	}

	/**
	 * Returns the estimated amount of time that it takes for this task to finish.
	 * 
	 * @param rcl
	 * @return finish time estimation of the task
	 */
	protected double getEstimatedFinishTime(ResGpuTask rcl) {
		List<Double> mipsShare = getCurrentMipsShare();
		double totalMips = getTotalCurrentAvailableMipsForTask(rcl, mipsShare);
		return rcl.getRemainingTaskLength() / totalMips;
	}

	/**
	 * Submit a task which has finished its host to device memory transfer.
	 */
	@Override
	public double taskSubmit(GpuTask task) {
		ResGpuTask rgt = new ResGpuTask(task);
		int numberOfCurrentAvailablePEs = getCurrentMipsShare().size() - getUsedPes().size();
		// it can go to the exec list
		if (numberOfCurrentAvailablePEs > 0) {
			rgt.setTaskStatus(GpuTask.INEXEC);
			allocatePes(rgt);
			getTaskExecList().add(rgt);
			return getEstimatedFinishTime(rgt);
		} else {// no enough free PEs: go to the waiting queue
			rgt.setTaskStatus(GpuTask.QUEUED);
			getTaskWaitingList().add(rgt);
			return 0.0;
		}
	}

	@Override
	public double getTotalUtilizationOfGpu(double time) {
		final double totalMipsShare = MathUtil.sum(getCurrentMipsShare());
		double totalRequestedMips = 0.0;
		for (ResGpuTask gl : getTaskExecList()) {
			totalRequestedMips += gl.getGpuTask().getUtilizationOfGpu(time)
					* getTotalCurrentAllocatedMipsForTask(gl, time);
		}
		double totalUtilization = totalRequestedMips / totalMipsShare;
		return totalUtilization;
	}

	/**
	 * Returns the first task to migrate to another VM.
	 * 
	 * @return the first running task
	 * @pre $none
	 * @post $none
	 * 
	 */
	@Override
	public GpuTask migrateTask() {
		if (getTaskExecList().isEmpty()) {
			return null;
		}
		ResGpuTask rcl = getTaskExecList().remove(0);
		rcl.finalizeTask();
		GpuTask cl = rcl.getGpuTask();
		releasePes(rcl);
		return cl;
	}

	@Override
	public List<Double> getCurrentRequestedMips() {
		List<Double> mipsShare = new ArrayList<Double>();
		if (getCurrentMipsShare() != null) {
			double totalGpuUtilization = getTotalUtilizationOfGpu(CloudSim.clock());
			for (Double mips : getCurrentMipsShare()) {
				mipsShare.add(mips * totalGpuUtilization);
			}
		}
		return mipsShare;
	}

	@Override
	public double getTotalCurrentAvailableMipsForTask(ResGpuTask rcl, List<Double> mipsShare) {
		double totalMips = 0.0;
		for (Integer peId : rcl.getPeIdList()) {
			totalMips += mipsShare.get(peId);
		}
		return totalMips;
	}

	@Override
	public double getTotalCurrentAllocatedMipsForTask(ResGpuTask rcl, double time) {
		double totalMips = 0.0;
		for (Double mips : getCurrentAllocatedMipsForTask(rcl, time)) {
			totalMips += mips;
		}
		return totalMips;
	}

	@Override
	public List<Double> getCurrentAllocatedMipsForTask(ResGpuTask rcl, double time) {
		List<Double> allocatedMips = new ArrayList<Double>();
		for (Integer peId : rcl.getPeIdList()) {
			allocatedMips.add(getCurrentMipsShare().get(peId));
		}
		return allocatedMips;
	}

	@Override
	public double getTotalCurrentRequestedMipsForTask(ResGpuTask rcl, double time) {
		return rcl.getGpuTask().getUtilizationOfGpu(time) * getTotalCurrentAllocatedMipsForTask(rcl, time);
	}

	@Override
	public double getCurrentRequestedUtilizationOfGddram() {
		double totalUtilization = 0;
		for (ResGpuTask gl : getTaskExecList()) {
			totalUtilization += gl.getGpuTask().getUtilizationOfGddram(CloudSim.clock());
		}
		if (totalUtilization > 1) {
			totalUtilization = 1;
		}
		return totalUtilization;
	}

	@Override
	public double getCurrentRequestedUtilizationOfBw() {
		double totalUtilization = 0;
		for (ResGpuTask gl : getTaskExecList()) {
			totalUtilization += gl.getGpuTask().getUtilizationOfBw(CloudSim.clock());
		}
		if (totalUtilization > 1) {
			totalUtilization = 1;
		}
		return totalUtilization;
	}

	/**
	 * @return the usedPes
	 */
	protected List<Integer> getUsedPes() {
		return usedPes;
	}

	/**
	 * @param usedPes the usedPes to set
	 */
	protected void setUsedPes(List<Integer> usedPes) {
		this.usedPes = usedPes;
	}

}
