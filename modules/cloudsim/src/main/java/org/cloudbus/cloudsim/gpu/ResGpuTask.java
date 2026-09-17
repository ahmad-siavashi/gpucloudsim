package org.cloudbus.cloudsim.gpu;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Consts;
import org.cloudbus.cloudsim.core.CloudSim;

/**
 * ResGpuTask represents a Task submitted to CloudResource (i.e. Vgpu) for
 * processing. This class keeps track the time for all activities in the
 * CloudResource for a specific Task. Before a Task exits the CloudResource, it
 * is RECOMMENDED to call this method {@link #finalizeTask()}.
 * <p/>
 * It contains a Task object along with its arrival time and the ID of the
 * machine and the Pe (Processing Element) allocated to it. It acts as a
 * placeholder for maintaining the amount of resource share allocated at various
 * times for simulating any scheduling using internal events.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class ResGpuTask {

	/** The Task object. */
	private final GpuTask task;

	/** The Task arrival time for the first time. */
	private double arrivalTime;

	/** The estimation of Task finished time. */
	private double finishedTime;

	/** The length of Task finished so far. */
	private long taskFinishedSoFar;

	/**
	 * Task execution start time. This attribute will only hold the latest time
	 * since a Task can be canceled, paused or resumed.
	 */
	private double startExecTime;

	/** The total time to complete this Task. */
	private double totalCompletionTime;

	// The below attributes are only to be used by the SpaceShared policy.

	/** The an array of Pe IDs. */
	private List<Integer> peArrayId = null;

	// NOTE: Below attributes are related to AR stuff

	/** The Constant NOT_FOUND. */
	private static final int NOT_FOUND = -1;

	/** The reservation start time. */
	private final long startTime;

	/** The reservation duration time. */
	private final int duration;

	/** The reservation id. */
	private final int reservId;

	/** The num Pe needed to execute this Task. */
	private int numberOfBlocks;

	/**
	 * Allocates a new ResGpuTask object upon the arrival of a Task object.
	 * 
	 * @param gpu task a gpu task object
	 */
	public ResGpuTask(GpuTask task) {
		this.task = task;
		startTime = 0;
		reservId = NOT_FOUND;
		duration = 0;
		// when a new ResGpuTask is created, then it will automatically set
		// the submission time and other properties, such as remaining length
		init();
	}

	/**
	 * Allocates a new ResGpuTask object upon the arrival of a Task object. Use this
	 * constructor to store reserved tasks, i.e. tasks that done reservation before.
	 * 
	 * @param task      a gpu task object
	 * @param startTime a reservation start time. Can also be interpreted as
	 *                  starting time to execute this Task.
	 * @param duration  a reservation duration time. Can also be interpreted as how
	 *                  long to execute this Task.
	 * @param reservID  a reservation ID that owns this Task
	 */
	public ResGpuTask(GpuTask task, long startTime, int duration, int reservID) {
		this.task = task;
		this.startTime = startTime;
		reservId = reservID;
		this.duration = duration;

		init();
	}

	/**
	 * Gets the Task or reservation start time.
	 * 
	 * @return Task's starting time
	 * @pre $none
	 * @post $none
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * Gets the reservation duration time.
	 * 
	 * @return reservation duration time
	 * @pre $none
	 * @post $none
	 */
	public int getDurationTime() {
		return duration;
	}

	/**
	 * Get task's number of blocks.
	 * 
	 * @return number of blocks
	 * @pre $none
	 * @post $none
	 */
	public int getNumberOfBlocks() {
		return numberOfBlocks;
	}

	/**
	 * Gets the reservation ID that owns this Task.
	 * 
	 * @return a reservation ID
	 * @pre $none
	 * @post $none
	 */
	public int getReservationID() {
		return reservId;
	}

	/**
	 * Checks whether this Task is submitted by reserving or not.
	 * 
	 * @return <tt>true</tt> if this Task has reserved before, <tt>false</tt>
	 *         otherwise
	 * @pre $none
	 * @post $none
	 */
	public boolean hasReserved() {
		if (reservId == NOT_FOUND) {
			return false;
		}

		return true;
	}

	/**
	 * Initializes all local attributes.
	 * 
	 * @pre $none
	 * @post $none
	 */
	private void init() {
		// get number of PEs required to run this Task
		numberOfBlocks = task.getNumberOfBlocks();

		peArrayId = new ArrayList<Integer>();

		task.setSubmissionTime(arrivalTime);

		// default values
		finishedTime = NOT_FOUND; // Cannot finish in this hourly slot.
		totalCompletionTime = 0.0;
		startExecTime = 0.0;

		// In case a Task has been executed partially by some other grid
		// hostList.
		taskFinishedSoFar = task.getTaskFinishedSoFar() * Consts.MILLION;
	}

	/**
	 * Gets this Task entity Id.
	 * 
	 * @return the Task entity Id
	 * @pre $none
	 * @post $none
	 */
	public int getTaskId() {
		return task.getTaskId();
	}

	/**
	 * Gets the Task's block length.
	 * 
	 * @return Task's block length
	 * @pre $none
	 * @post $none
	 */
	public long getBlockLength() {
		return task.getBlockLength();
	}

	/**
	 * Gets the total Task's length (across all PEs).
	 * 
	 * @return total Task's length
	 * @pre $none
	 * @post $none
	 */
	public long getTaskTotalLength() {
		return task.getTaskTotalLength();
	}

	/**
	 * Gets the Task's class type.
	 * 
	 * @return class type of the Task
	 * @pre $none
	 * @post $none
	 */
	public int getTaskClassType() {
		return task.getClassType();
	}

	/**
	 * Sets the Task status.
	 * 
	 * @param status the Task status
	 * @return <tt>true</tt> if the new status has been set, <tt>false</tt>
	 *         otherwise
	 * @pre status >= 0
	 * @post $none
	 */
	public boolean setTaskStatus(int status) {
		// gets Cloudlet's previous status
		int prevStatus = task.getTaskStatus();

		// if the status of a GpuTask is the same as last time, then ignore
		if (prevStatus == status) {
			return false;
		}

		boolean success = true;
		try {
			double clock = CloudSim.clock();   // gets the current clock

			// sets GpuTask's current status
			task.setTaskStatus(status);

			// if a previous GpuTask status is INEXEC
			if (prevStatus == GpuTask.INEXEC) {
				// and current status is either CANCELED, PAUSED or SUCCESS
				if (status == GpuTask.CANCELED || status == GpuTask.PAUSED || status == GpuTask.SUCCESS) {
					// then update the GpuTask completion time
					totalCompletionTime += (clock - startExecTime);
					return true;
				}
			}

			if (prevStatus == GpuTask.RESUMED && status == GpuTask.SUCCESS) {
				// then update the GpuTask completion time
				totalCompletionTime += (clock - startExecTime);
				return true;
			}

			// if a GpuTask is now in execution
			if (status == GpuTask.INEXEC || (prevStatus == GpuTask.PAUSED && status == GpuTask.RESUMED)) {
				startExecTime = clock;
				task.setExecStartTime(startExecTime);
			}

		} catch (Exception e) {
			success = false;
		}

		return success;
	}

	/**
	 * Gets the Task's execution start time.
	 * 
	 * @return Task's execution start time
	 * @pre $none
	 * @post $none
	 */
	public double getExecStartTime() {
		return task.getExecStartTime();
	}

	/**
	 * Sets this Task's execution parameters. These parameters are set by the
	 * CloudResource before departure or sending back to the original Task's owner.
	 * 
	 * @param wallClockTime the time of this Task resides in a CloudResource (from
	 *                      arrival time until departure time).
	 * @param actualGPUTime the total execution time of this Task in a
	 *                      CloudResource.
	 * @pre wallClockTime >= 0.0
	 * @pre actualGPUTime >= 0.0
	 * @post $none
	 */
	public void setExecParam(double wallClockTime, double actualGPUTime) {
		task.setExecParam(wallClockTime, actualGPUTime);
	}

	/**
	 * Sets the Pe (Processing Element) ID.
	 * 
	 * @param peId Pe ID
	 * @pre peID >= 0
	 * @post $none
	 */
	public void setPeId(int peId) {
		getPeIdList().add(peId);
	}

	/**
	 * Gets a list of Pe IDs. <br>
	 * Pe IDs are unique across all devices.
	 * 
	 * @return an array containing Pe IDs.
	 * @pre $none
	 * @post $none
	 */
	public List<Integer> getPeIdList() {
		return peArrayId;
	}

	/**
	 * Gets the remaining gpu task length that has to be execute yet, considering
	 * the {@link #getTaskTotalLength()}.
	 * 
	 * @return gpu task length
	 * @pre $none
	 * @post $result >= 0
	 */
	public long getRemainingTaskLength() {
		long length = task.getTaskTotalLength() * Consts.MILLION - taskFinishedSoFar;

		// Remaining Task length can't be negative number.
		if (length < 0) {
			return 0;
		}

		return (long) Math.floor(length / Consts.MILLION);
	}

	/**
	 * Finalizes all relevant information before <tt>exiting</tt> the CloudResource
	 * entity. This method sets the final data of:
	 * <ul>
	 * <li>wall clock time, i.e. the time of this Task resides in a CloudResource
	 * (from arrival time until departure time).
	 * <li>actual GPU time, i.e. the total execution time of this Task in a
	 * CloudResource.
	 * <li>Task's finished time so far
	 * </ul>
	 * 
	 * @pre $none
	 * @post $none
	 */
	public void finalizeTask() {
		// Sets the wall clock time and actual GPU time
		double wallClockTime = CloudSim.clock() - arrivalTime;
		task.setExecParam(wallClockTime, totalCompletionTime);

		long finished = taskFinishedSoFar > getTaskTotalLength() * Consts.MILLION ? getTaskTotalLength()
				: taskFinishedSoFar / Consts.MILLION;

		// Launch
		task.setTaskFinishedSoFar(finished);
	}

	/**
	 * Updates the length of gpu task that has already been completed.
	 * 
	 * @param miLength gpu task length in Instructions (I)
	 * @pre miLength >= 0.0
	 * @post $none
	 */
	public void updateTaskFinishedSoFar(long miLength) {
		taskFinishedSoFar += miLength;
	}

	/**
	 * Gets arrival time of a task.
	 * 
	 * @return arrival time
	 * @pre $none
	 * @post $result >= 0.0
	 * 
	 * @todo It is being used different words for the same term. Here it is used
	 *       arrival time while at Resource inner class of the Task class it is
	 *       being used submissionTime. It needs to be checked if they are the same
	 *       term or different ones in fact.
	 */
	public double getTaskArrivalTime() {
		return arrivalTime;
	}

	/**
	 * Sets the finish time for this Task. If time is negative, then it is being
	 * ignored.
	 * 
	 * @param time finish time
	 * @pre time >= 0.0
	 * @post $none
	 */
	public void setFinishTime(double time) {
		if (time < 0.0) {
			return;
		}

		finishedTime = time;
	}

	/**
	 * Gets the Task's finish time.
	 * 
	 * @return finish time of a gpu task or <tt>-1.0</tt> if it cannot finish in
	 *         this hourly slot
	 * @pre $none
	 * @post $result >= -1.0
	 */
	public double getTaskFinishTime() {
		return finishedTime;
	}

	/**
	 * Gets the related Task object.
	 * 
	 * @return gpu task object
	 * @pre $none
	 * @post $result != null
	 */
	public GpuTask getGpuTask() {
		return task;
	}

	/**
	 * Gets the Task status.
	 * 
	 * @return Task status
	 * @pre $none
	 * @post $none
	 */
	public int getTaskStatus() {
		return task.getTaskStatus();
	}

	/**
	 * Get am Unique Identifier (UID) of the task.
	 * 
	 * @return The UID
	 */
	public String getUid() {
		return getGpuTask().getCloudlet().getUserId() + "-" + getTaskId();
	}

}
