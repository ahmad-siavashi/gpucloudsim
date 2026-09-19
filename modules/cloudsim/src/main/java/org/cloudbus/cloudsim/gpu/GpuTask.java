package org.cloudbus.cloudsim.gpu;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.core.CloudSim;

/**
 * {@link GpuTask} represents a process that is executed in a {@link Vgpu}.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class GpuTask {

	/**
	 * task Id.
	 */
	private final int taskId;

	/**
	 * The {@link GpuCloudlet} associated with the task.
	 */
	private GpuCloudlet gpuCloudlet;

	/**
	 * The execution length of one gpu task block (Unit: in Million Instructions
	 * (MI)).
	 */
	private long blockLength;

	/**
	 * The input size of this task (unit: in byte) which is transfered from CPU to
	 * GPU before execution.
	 */
	private final long taskInputSize;

	/**
	 * The output size of this task (unit: in byte) which is transfered from GPU to
	 * CPU after execution.
	 */
	private final long taskOutputSize;

	/**
	 * The memory size that needs to be allocate in the {@link Vgpu} for the task.
	 */
	private final long requestedGddramSize;

	/**
	 * Total number of task block. The length of the task is
	 * {@link #getBlockLength()} * {@link #numberOfBlocks}
	 * 
	 * @see #blockLength
	 */
	private int numberOfBlocks;

	/**
	 * Limits the number of GPU PEs that the task can use for its execution on
	 * {@link Vgpu}.
	 */
	private int pesLimit;

	/**
	 * The id of the {@link Vgpu} of the VM that executes the task. The value -1
	 * selects the first vgpu of the VM.
	 */
	private int vgpuId = -1;

	/**
	 * The execution status of this task.
	 */
	private int status;

	/**
	 * The execution start time of this task. With new functionalities, such as
	 * CANCEL, PAUSED and RESUMED, this attribute only stores the latest execution
	 * time. Previous execution time are ignored.
	 */
	private double execStartTime;

	/**
	 * The time in which the execution of the task is finished.
	 */
	private double finishTime;

	/**
	 * Indicates if transaction history records for this task is to be outputted.
	 */
	private final boolean record;

	/**
	 * Stores the operating system line separator.
	 */
	private String newline;

	/**
	 * The task transaction history.
	 */
	private StringBuffer history;

	/**
	 * The list of every resource where the task has been executed. In case it
	 * starts and finishes executing in a single cloud resource, without being
	 * migrated, this list will have only one item.
	 */
	private final List<Resource> resList;

	/**
	 * The index of the last resource where the task was executed. If the task is
	 * migrated during its execution, this index is updated. The value -1 indicates
	 * the task has not been executed yet.
	 */
	private int index;

	/**
	 * The classType or priority of this task for scheduling on a resource.
	 */
	private int classType;

	/**
	 * The format of decimal numbers.
	 */
	private DecimalFormat num;

	// //////////////////////////////////////////
	// Below are CONSTANTS attributes
	/**
	 * The task has been created.
	 */
	public static final int CREATED = 0;

	/**
	 * The task has been assigned to a CloudResource object to be executed as
	 * planned.
	 */
	public static final int READY = 1;

	/**
	 * The task has moved to a Cloud node.
	 */
	public static final int QUEUED = 2;

	/**
	 * The task is in execution in a Cloud node.
	 */
	public static final int INEXEC = 3;

	/**
	 * The task has been executed successfully.
	 */
	public static final int SUCCESS = 4;

	/**
	 * The task has failed.
	 */
	public static final int FAILED = 5;

	/**
	 * The task has been canceled.
	 */
	public static final int CANCELED = 6;

	/**
	 * The task has been paused. It can be resumed by changing the status into
	 * <tt>RESUMED</tt>.
	 */
	public static final int PAUSED = 7;

	/**
	 * The task has been resumed from <tt>PAUSED</tt> state.
	 */
	public static final int RESUMED = 8;

	/**
	 * The task has failed due to a resource failure.
	 */
	public static final int FAILED_RESOURCE_UNAVAILABLE = 9;

	// Utilization
	/**
	 * The utilization model that defines how the task will use the PEs of the GPU.
	 */
	private UtilizationModel utilizationModelGpu;

	/**
	 * The utilization model that defines how the task will use the GPU GDDRAM.
	 */
	private UtilizationModel utilizationModelGddram;

	/**
	 * The utilization model that defines how the task will use the GPU GDDRAM
	 * bandwidth.
	 */
	private UtilizationModel utilizationModelBw;

	/**
	 * Allocates a new task object. The task length, input and output sizes should
	 * be greater than or equal to 1. By default this constructor sets the history
	 * of this object.
	 * 
	 * @param taskId                 the unique ID of this task
	 * @param blockLength            the length or size (in MI) of a single task
	 *                               block
	 * @param inputSize              the input size (in MB) of this task
	 * @param outputSize             the output size (in MB) of this task
	 * @param numberOfBlocks         the number of task blocks
	 * @param utilizationModelGpu    the utilization model of gpu
	 * @param utilizationModelGddram the utilization model of gddram
	 * @param utilizationModelBw     the utilization model of gddram bw
	 * 
	 * @pre taskID >= 0
	 * @pre blockLength >= 0.0
	 * @pre taskInputSize >= 1
	 * @pre taskOutputSize >= 1
	 * @post $none
	 */
	public GpuTask(final int taskId, final long blockLength, final int numberOfBlocks, final long inputSize,
			final long outputSize, final long requestedGddramSize,
			final UtilizationModel utilizationModelGpu, final UtilizationModel utilizationModelGddram,
			final UtilizationModel utilizationModelBw) {
		this(taskId, blockLength, numberOfBlocks, inputSize, outputSize, requestedGddramSize,
				utilizationModelGpu, utilizationModelGddram, utilizationModelBw, false);
	}

	/**
	 * Allocates a new GPUTask object. The task length, input and output file sizes
	 * should be greater than or equal to 1.
	 * 
	 * @param taskId                 the unique ID of this task
	 * @param blockLength            the length or size (in MI) of a single task
	 *                               block
	 * @param inputSize              the input size (in MB) of this task
	 * @param outputSize             the output size (in MB) of this task
	 * @param record                 record the history of this object or not
	 * @param numberOfBlocks         the number of task blocks
	 * @param utilizationModelGpu    the utilization model of gpu
	 * @param utilizationModelGddram the utilization model of gddram
	 * @param utilizationModelBw     the utilization model of gddram BW
	 * 
	 * @pre taskID >= 0
	 * @pre blockLength >= 0.0
	 * @pre taskInputSize >= 1
	 * @pre taskOutputSize >= 1
	 * @post $none
	 */
	public GpuTask(final int taskId, final long blockLength, final int numberOfBlocks, final long inputSize,
			final long outputSize, final long requestedGddramSize,
			final UtilizationModel utilizationModelGpu, final UtilizationModel utilizationModelGddram,
			final UtilizationModel utilizationModelBw, final boolean record) {
		status = CREATED;
		this.taskId = taskId;
		this.numberOfBlocks = numberOfBlocks;
		// If not set explicitly, then take as much as you need.
		setPesLimit(numberOfBlocks);
		execStartTime = 0.0;
		finishTime = -1.0; // meaning this task hasn't finished yet
		classType = 0;
		// Task length, Input and Output size should be at least 1 byte.
		this.blockLength = Math.max(1, blockLength);
		this.taskInputSize = Math.max(1, inputSize);
		this.taskOutputSize = Math.max(1, outputSize);
		this.requestedGddramSize = Math.max(1, requestedGddramSize);

		// Normally, a task is only executed on a resource without being
		// migrated to others. Hence, to reduce memory consumption, set the
		// size of this ArrayList to be less than the default one.
		resList = new ArrayList<Resource>(2);
		index = -1;
		this.record = record;

		setUtilizationModelGpu(utilizationModelGpu);
		setUtilizationModelGddram(utilizationModelGddram);
		setUtilizationModelBw(utilizationModelBw);
	}

	// ////////////////////// INTERNAL CLASS ///////////////////////////////////
	/**
	 * Internal class that keeps track of task's movement in different
	 * CloudResources. Each time a task is run on a given GPU, the task's execution
	 * history on each VM is registered at {@link GpuTask#resList}
	 */
	private static class Resource {

		/**
		 * Task's submission (arrival) time to a CloudResource.
		 */
		public double submissionTime = 0.0;

		/**
		 * The time this task resides in a CloudResource (from arrival time until
		 * departure time, that may include waiting time).
		 */
		public double wallClockTime = 0.0;

		/**
		 * The total time the task spent being executed in a CloudResource.
		 */
		public double actualGpuTime = 0.0;

		/**
		 * Cost per second a CloudResource charge to execute this task.
		 */
		public double costPerSec = 0.0;

		/**
		 * Task's length finished so far.
		 */
		public long finishedSoFar = 0;

		/**
		 * a CloudResource id.
		 */
		public int resourceId = -1;

		/**
		 * a CloudResource name.
		 */
		public String resourceName = null;

	}

	/**
	 * Sets the length or size (in MI) of this task to be executed in a
	 * CloudResource. It has to be the length for each individual Pe, <tt>not</tt>
	 * the total length (the sum of length to be executed by each Pe).
	 * 
	 * @param blockLength the length or size (in MI) of this task to be executed in
	 *                    a CloudResource
	 * @return <tt>true</tt> if it is successful, <tt>false</tt> otherwise
	 * 
	 * @see #getTaskTotalLength()
	 * @pre blockLength > 0
	 * @post $none
	 */
	public boolean setBlockLength(final long blockLength) {
		if (blockLength <= 0) {
			return false;
		}

		this.blockLength = blockLength;
		return true;
	}

	/**
	 * @return the pesLimit
	 */
	public int getPesLimit() {
		return pesLimit;
	}

	/**
	 * @param pesLimit the pesLimit to set
	 */
	public void setPesLimit(int pesLimit) {
		this.pesLimit = pesLimit;
	}

	/**
	 * Gets the time the task had to wait before start executing on a resource.
	 * 
	 * @return the waiting time
	 * @pre $none
	 * @post $none
	 */
	public double getWaitingTime() {
		if (index == -1) {
			return 0;
		}

		// use the latest resource submission time
		final double subTime = resList.get(index).submissionTime;
		return execStartTime - subTime;
	}

	/**
	 * Sets the classType or priority of this task for scheduling on a resource.
	 * 
	 * @param classType classType of this task
	 * @return <tt>true</tt> if it is successful, <tt>false</tt> otherwise
	 * 
	 * @pre classType > 0
	 * @post $none
	 */
	public boolean setClassType(final int classType) {
		boolean success = false;
		if (classType > 0) {
			this.classType = classType;
			success = true;
		}

		return success;
	}

	/**
	 * Gets the classtype or priority of this task for scheduling on a resource.
	 * 
	 * @return classtype of this task
	 * @pre $none
	 * @post $none
	 */
	public int getClassType() {
		return classType;
	}

	/**
	 * Sets the number of PEs required to run this task. <br>
	 * NOTE: The task length is computed only for 1 Pe for simplicity. <br>
	 * For example, consider a task that has a length of 500 MI and requires 2 PEs.
	 * This means each Pe will execute 500 MI of this task.
	 * 
	 * @param numberOfBlocks number of Pe
	 * @return <tt>true</tt> if it is successful, <tt>false</tt> otherwise
	 * 
	 * @pre numPE > 0
	 * @post $none
	 */
	public boolean setNumberOfBlocks(final int numberOfBlocks) {
		if (numberOfBlocks > 0) {
			this.numberOfBlocks = numberOfBlocks;
			return true;
		}
		return false;
	}

	/**
	 * Gets the number of PEs required to run this task.
	 * 
	 * @return number of PEs
	 * 
	 * @pre $none
	 * @post $none
	 */
	public int getNumberOfBlocks() {
		return numberOfBlocks;
	}

	/**
	 * Gets the transaction history of this task. The layout of this history is in a
	 * readable table column with <tt>time</tt> and <tt>description</tt> as headers.
	 * 
	 * @return a String containing the history of this task object.
	 * @pre $none
	 * @post $result != null
	 */
	public String getTaskHistory() {
		String msg = null;
		if (history == null) {
			msg = "No history is recorded for Task #" + taskId;
		} else {
			msg = history.toString();
		}

		return msg;
	}

	/**
	 * Gets the length of this task that has been executed so far from the latest
	 * CloudResource. This method is useful when trying to move this task into
	 * different CloudResources or to cancel it.
	 * 
	 * @return the length of a partially executed task or the full task length if it
	 *         is completed
	 * @pre $none
	 * @post $result >= 0.0
	 */
	public long getTaskFinishedSoFar() {
		if (index == -1) {
			return blockLength;
		}

		final long finish = resList.get(index).finishedSoFar;
		if (finish > blockLength) {
			return blockLength;
		}

		return finish;
	}

	/**
	 * Checks whether this task has finished execution or not.
	 * 
	 * @return <tt>true</tt> if this task has finished execution, <tt>false</tt>
	 *         otherwise
	 * @pre $none
	 * @post $none
	 */
	public boolean isFinished() {
		if (index == -1) {
			return false;
		}

		boolean completed = false;

		// if result is 0 or -ve then this task has finished
		final long finish = resList.get(index).finishedSoFar;
		final long result = getTaskTotalLength() - finish;
		if (result <= 0.0) {
			completed = true;
		}
		return completed;
	}

	/**
	 * Sets the length of this task that has been executed so far. This method is
	 * used by ResGpuTask class when an application is decided to cancel or to move
	 * this task into different CloudResources.
	 * 
	 * @param length length of this task
	 * @pre length >= 0.0
	 * @post $none
	 */
	public void setTaskFinishedSoFar(final long length) {
		// if length is -ve then ignore
		if (length < 0.0 || index < 0) {
			return;
		}

		final Resource res = resList.get(index);
		res.finishedSoFar = length;

		if (record) {
			write("Sets the length's finished so far to " + length);
		}
	}

	/**
	 * Gets the latest resource ID that processes this task.
	 * 
	 * @return the resource ID or <tt>-1</tt> if none
	 * @pre $none
	 * @post $result >= -1
	 */
	public int getResourceId() {
		if (index == -1) {
			return -1;
		}
		return resList.get(index).resourceId;
	}

	/**
	 * Gets the input size of this task.
	 * 
	 * @return the input size of this task
	 * @pre $none
	 * @post $result >= 1
	 */
	public long getTaskInputSize() {
		return taskInputSize;
	}

	/**
	 * Gets the output size of this task.
	 * 
	 * @return the task output size
	 * @pre $none
	 * @post $result >= 1
	 */
	public long getTaskOutputSize() {
		return taskOutputSize;
	}

	/**
	 * Sets the resource parameters for which the task is going to be executed. From
	 * the second time this method is called, every call make the task to be
	 * migrated to the indicated resource.<br>
	 * 
	 * NOTE: This method <tt>should</tt> be called only by a resource entity, not
	 * the user or owner of this task.
	 * 
	 * @param resourceID the CloudResource ID
	 * @param cost       the cost running this CloudResource per second
	 * 
	 * @pre resourceID >= 0
	 * @pre cost > 0.0
	 * @post $none
	 */
	public void setResourceParameter(final int resourceID, final double cost) {
		final Resource res = new Resource();
		res.resourceId = resourceID;
		res.costPerSec = cost;
		res.resourceName = CloudSim.getEntityName(resourceID);

		// add into a list if moving to a new grid resource
		resList.add(res);

		if (index == -1 && record) {
			write("Allocates this task to " + res.resourceName + " (ID #" + resourceID + ") with cost = $" + cost
					+ "/sec");
		} else if (record) {
			final int id = resList.get(index).resourceId;
			final String name = resList.get(index).resourceName;
			write("Moves task from " + name + " (ID #" + id + ") to " + res.resourceName + " (ID #" + resourceID
					+ ") with cost = $" + cost + "/sec");
		}

		index++; // initially, index = -1
	}

	/**
	 * Sets the submission (arrival) time of this task into a CloudResource.
	 * 
	 * @param clockTime the submission time
	 * @pre clockTime >= 0.0
	 * @post $none
	 */
	public void setSubmissionTime(final double clockTime) {
		if (clockTime < 0.0 || index < 0) {
			return;
		}

		final Resource res = resList.get(index);
		res.submissionTime = clockTime;

		if (record) {
			write("Sets the submission time to " + num.format(clockTime));
		}
	}

	/**
	 * Gets the submission (arrival) time of this task from the latest
	 * CloudResource.
	 * 
	 * @return the submission time or <tt>0.0</tt> if none
	 * @pre $none
	 * @post $result >= 0.0
	 */
	public double getSubmissionTime() {
		if (index == -1) {
			return 0.0;
		}
		return resList.get(index).submissionTime;
	}

	/**
	 * Sets the execution start time of this task inside a CloudResource. <br/>
	 * <b>NOTE:</b> With new functionalities, such as being able to cancel / to
	 * pause / to resume this task, the execution start time only holds the latest
	 * one. Meaning, all previous execution start time are ignored.
	 * 
	 * @param clockTime the latest execution start time
	 * @pre clockTime >= 0.0
	 * @post $none
	 */
	public void setExecStartTime(final double clockTime) {
		execStartTime = clockTime;
		if (record) {
			write("Sets the execution start time to " + num.format(clockTime));
		}
	}

	/**
	 * Gets the latest execution start time.
	 * 
	 * @return the latest execution start time
	 * @pre $none
	 * @post $result >= 0.0
	 */
	public double getExecStartTime() {
		return execStartTime;
	}

	/**
	 * Sets the task's execution parameters. These parameters are set by the
	 * CloudResource before departure or sending back to the original task's owner.
	 * 
	 * @param wallTime   the time of this task resides in a CloudResource (from
	 *                   arrival time until departure time).
	 * @param actualTime the total execution time of this task in a CloudResource.
	 * 
	 * @see Resource#wallClockTime
	 * @see Resource#actualGpuTime
	 * 
	 * @pre wallTime >= 0.0
	 * @pre actualTime >= 0.0
	 * @post $none
	 */
	public void setExecParam(final double wallTime, final double actualTime) {
		if (wallTime < 0.0 || actualTime < 0.0 || index < 0) {
			return;
		}

		final Resource res = resList.get(index);
		res.wallClockTime = wallTime;
		res.actualGpuTime = actualTime;

		if (record) {
			write("Sets the wall clock time to " + num.format(wallTime) + " and the actual GPU time to "
					+ num.format(actualTime));
		}
	}

	/**
	 * Sets the execution status code of this task.
	 * 
	 * @param newStatus the status code of this task
	 * @throws Exception Invalid range of task status
	 * @pre newStatus >= 0 && newStatus <= 8 @ post $none
	 * 
	 * @todo It has to throw an specific (unckecked) exception
	 */
	public void setTaskStatus(final int newStatus) throws Exception {
		// if the new status is same as current one, then ignore the rest
		if (status == newStatus) {
			return;
		}

		// throws an exception if the new status is outside the range
		if (newStatus < GpuTask.CREATED || newStatus > GpuTask.FAILED_RESOURCE_UNAVAILABLE) {
			throw new Exception("GPUTask.setTaskStatus() : Error - Invalid integer range for task status.");
		}

		if (newStatus == GpuTask.SUCCESS) {
			finishTime = CloudSim.clock();
		}

		if (record) {
			write("Sets Task status from " + getTaskStatusString() + " to " + GpuTask.getStatusString(newStatus));
		}

		status = newStatus;
	}

	/**
	 * Gets the status code of this task.
	 * 
	 * @return the status code of this task
	 * @pre $none
	 * @post $result >= 0
	 * @deprecated Use the getter {@link #getStatus()} instead
	 */
	@Deprecated
	public int getTaskStatus() {
		return status;
	}

	/**
	 * Gets the string representation of the current task status code.
	 * 
	 * @return the task status code as a string or <tt>null</tt> if the status code
	 *         is unknown
	 * @pre $none
	 * @post $none
	 */
	public String getTaskStatusString() {
		return GpuTask.getStatusString(status);
	}

	/**
	 * Gets the string representation of the given task status code.
	 * 
	 * @param status the task status code
	 * @return the task status code as a string or <tt>null</tt> if the status code
	 *         is unknown
	 * @pre $none
	 * @post $none
	 */
	public static String getStatusString(final int status) {
		String statusString = null;
		switch (status) {
		case GpuTask.CREATED:
			statusString = "Created";
			break;

		case GpuTask.READY:
			statusString = "Ready";
			break;

		case GpuTask.INEXEC:
			statusString = "InExec";
			break;

		case GpuTask.SUCCESS:
			statusString = "Success";
			break;

		case GpuTask.QUEUED:
			statusString = "Queued";
			break;

		case GpuTask.FAILED:
			statusString = "Failed";
			break;

		case GpuTask.CANCELED:
			statusString = "Canceled";
			break;

		case GpuTask.PAUSED:
			statusString = "Paused";
			break;

		case GpuTask.RESUMED:
			statusString = "Resumed";
			break;

		case GpuTask.FAILED_RESOURCE_UNAVAILABLE:
			statusString = "Failed_resource_unavailable";
			break;
		default:
			break;
		}

		return statusString;
	}

	/**
	 * Gets the length of this task.
	 * 
	 * @return the length of this task
	 * @pre $none
	 * @post $result >= 0.0
	 */
	public long getBlockLength() {
		return blockLength;
	}

	/**
	 * Gets the total length (across all PEs) of this task. It considers the
	 * {@link #blockLength} of the task to be executed in each Pe and the
	 * {@link #numberOfBlocks}.<br/>
	 * 
	 * For example, setting the taskLenght as 10000 MI and {@link #numberOfBlocks}
	 * to 4, each Pe will execute 10000 MI. Thus, the entire task has a total length
	 * of 40000 MI.
	 * 
	 * 
	 * @return the total length of this task
	 * 
	 * @see #setBlockLength(long)
	 * @pre $none
	 * @post $result >= 0.0
	 */
	public long getTaskTotalLength() {
		return getBlockLength() * getNumberOfBlocks();
	}

	/**
	 * Gets the cost/sec of running the task in the latest CloudResource.
	 * 
	 * @return the cost associated with running this task or <tt>0.0</tt> if none
	 * @pre $none
	 * @post $result >= 0.0
	 */
	public double getCostPerSec() {
		if (index == -1) {
			return 0.0;
		}
		return resList.get(index).costPerSec;
	}

	/**
	 * Gets the time of this task resides in the latest CloudResource (from arrival
	 * time until departure time).
	 * 
	 * @return the time of this task resides in a CloudResource
	 * @pre $none
	 * @post $result >= 0.0
	 */
	public double getWallClockTime() {
		if (index == -1) {
			return 0.0;
		}
		return resList.get(index).wallClockTime;
	}

	/**
	 * Gets all the CloudResource names that executed this task.
	 * 
	 * @return an array of CloudResource names or <tt>null</tt> if it has none
	 * @pre $none
	 * @post $none
	 */
	public String[] getAllResourceName() {
		final int size = resList.size();
		String[] data = null;

		if (size > 0) {
			data = new String[size];
			for (int i = 0; i < size; i++) {
				data[i] = resList.get(i).resourceName;
			}
		}

		return data;
	}

	/**
	 * Gets all the CloudResource IDs that executed this task.
	 * 
	 * @return an array of CloudResource IDs or <tt>null</tt> if it has none
	 * @pre $none
	 * @post $none
	 */
	public int[] getAllResourceId() {
		final int size = resList.size();
		int[] data = null;

		if (size > 0) {
			data = new int[size];
			for (int i = 0; i < size; i++) {
				data[i] = resList.get(i).resourceId;
			}
		}

		return data;
	}

	/**
	 * Gets the total execution time of this task in a given CloudResource ID.
	 * 
	 * @param resId a CloudResource entity ID
	 * @return the total execution time of this task in a CloudResource or
	 *         <tt>0.0</tt> if not found
	 * @pre resId >= 0
	 * @post $result >= 0.0
	 */
	public double getActualGPUTime(final int resId) {
		Resource resource = getResourceById(resId);
		if (resource != null) {
			return resource.actualGpuTime;
		}
		return 0.0;
	}

	/**
	 * Gets the cost running this task in a given CloudResource ID.
	 * 
	 * @param resId a CloudResource entity ID
	 * @return the cost associated with running this task or <tt>0.0</tt> if not
	 *         found
	 * @pre resId >= 0
	 * @post $result >= 0.0
	 */
	public double getCostPerSec(final int resId) {
		Resource resource = getResourceById(resId);
		if (resource != null) {
			return resource.costPerSec;
		}
		return 0.0;
	}

	/**
	 * Gets the length of this task that has been executed so far in a given
	 * CloudResource ID. This method is useful when trying to move this task into
	 * different CloudResources or to cancel it.
	 * 
	 * @param resId a CloudResource entity ID
	 * @return the length of a partially executed task or the full task length if it
	 *         is completed or <tt>0.0</tt> if not found
	 * @pre resId >= 0
	 * @post $result >= 0.0
	 */
	public long getTaskFinishedSoFar(final int resId) {
		Resource resource = getResourceById(resId);
		if (resource != null) {
			return resource.finishedSoFar;
		}
		return 0;
	}

	/**
	 * Gets the submission (arrival) time of this task in the given CloudResource
	 * ID.
	 * 
	 * @param resId a CloudResource entity ID
	 * @return the submission time or <tt>0.0</tt> if not found
	 * @pre resId >= 0
	 * @post $result >= 0.0
	 */
	public double getSubmissionTime(final int resId) {
		Resource resource = getResourceById(resId);
		if (resource != null) {
			return resource.submissionTime;
		}
		return 0.0;
	}

	/**
	 * Gets the time of this task resides in a given CloudResource ID (from arrival
	 * time until departure time).
	 * 
	 * @param resId a CloudResource entity ID
	 * @return the time of this task resides in the CloudResource or <tt>0.0</tt> if
	 *         not found
	 * @pre resId >= 0
	 * @post $result >= 0.0
	 */
	public double getWallClockTime(final int resId) {
		Resource resource = getResourceById(resId);
		if (resource != null) {
			return resource.wallClockTime;
		}
		return 0.0;
	}

	/**
	 * Gets the CloudResource name based on its ID.
	 * 
	 * @param resId a CloudResource entity ID
	 * @return the CloudResource name or <tt>null</tt> if not found
	 * @pre resId >= 0
	 * @post $none
	 */
	public String getResourceName(final int resId) {
		Resource resource = getResourceById(resId);
		if (resource != null) {
			return resource.resourceName;
		}
		return null;
	}

	/**
	 * Gets the resource by id.
	 * 
	 * @param resourceId the resource id
	 * @return the resource by id
	 */
	public Resource getResourceById(final int resourceId) {
		for (Resource resource : resList) {
			if (resource.resourceId == resourceId) {
				return resource;
			}
		}
		return null;
	}

	/**
	 * Gets the finish time of this task in a CloudResource.
	 * 
	 * @return the finish or completion time of this task or <tt>-1</tt> if not
	 *         finished yet.
	 * @pre $none
	 * @post $result >= -1
	 */
	public double getFinishTime() {
		return finishTime;
	}

	// //////////////////////// PROTECTED METHODS //////////////////////////////
	/**
	 * Writes this particular history transaction of this task into a log.
	 * 
	 * @param str a history transaction of this task
	 * @pre str != null
	 * @post $none
	 */
	protected void write(final String str) {
		if (!record) {
			return;
		}

		if (num == null || history == null) { // Creates the history or
			// transactions of this task
			newline = System.getProperty("line.separator");
			num = new DecimalFormat("#0.00#"); // with 3 decimal spaces
			history = new StringBuffer(1000);
			history.append("Time below denotes the simulation time.");
			history.append(System.getProperty("line.separator"));
			history.append("Time (sec)       Description task #" + taskId);
			history.append(System.getProperty("line.separator"));
			history.append("------------------------------------------");
			history.append(System.getProperty("line.separator"));
			history.append(num.format(CloudSim.clock()));
			history.append("   Creates task ID #" + taskId);
			history.append(System.getProperty("line.separator"));
		}

		history.append(num.format(CloudSim.clock()));
		history.append("   " + str + newline);
	}

	/**
	 * Get the status of the task.
	 * 
	 * @return status of the task
	 * @pre $none
	 * @post $none
	 * 
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * Gets the ID of this task.
	 * 
	 * @return task Id
	 * @pre $none
	 * @post $none
	 */
	public int getTaskId() {
		return taskId;
	}

	/**
	 * @return the id of the vgpu that executes the task, or -1 for the first vgpu
	 *         of the VM
	 */
	public int getVgpuId() {
		return vgpuId;
	}

	/**
	 * A task of a VM with more than one vgpu runs on the first one unless it is
	 * bound to another.
	 *
	 * @param vgpuId the id of the vgpu of the VM that executes the task
	 */
	public void setVgpuId(int vgpuId) {
		this.vgpuId = vgpuId;
	}

	/**
	 * @return the cloudlet
	 */
	public GpuCloudlet getCloudlet() {
		return gpuCloudlet;
	}

	/**
	 * @param cloudlet the cloudlet to set
	 */
	public void setCloudlet(GpuCloudlet cloudlet) {
		this.gpuCloudlet = cloudlet;
		if (cloudlet.getGpuTask() == null) {
			cloudlet.setGpuTask(this);
		}
	}

	/**
	 * Returns the execution time of the task.
	 * 
	 * @return time in which the task was running
	 * @pre $none
	 * @post $none
	 */
	public double getActualGPUTime() {
		return getFinishTime() - getExecStartTime();
	}

	/**
	 * Sets the resource parameters for which this task is going to be executed.
	 * <br>
	 * NOTE: This method <tt>should</tt> be called only by a resource entity, not
	 * the user or owner of this task.
	 * 
	 * @param resourceID the CloudResource ID
	 * @param costPerCPU the cost per second of running this task
	 * @param costPerBw  the cost per byte of data transfer to the GPU
	 * 
	 * @pre resourceID >= 0
	 * @pre cost > 0.0
	 * @post $none
	 */
	public void setResourceParameter(final int resourceID, final double costPerGPU, final double costPerBw) {
		setResourceParameter(resourceID, costPerGPU);
	}

	/**
	 * Gets the utilization model of gpu.
	 * 
	 * @return the utilization model gpu
	 */
	public UtilizationModel getUtilizationModelGpu() {
		return utilizationModelGpu;
	}

	/**
	 * Sets the utilization model of gpu.
	 * 
	 * @param utilizationModelCpu the new utilization model of gpu
	 */
	public void setUtilizationModelGpu(final UtilizationModel utilizationModelGpu) {
		this.utilizationModelGpu = utilizationModelGpu;
	}

	/**
	 * Gets the utilization model of GDDRAM.
	 * 
	 * @return the utilization model gddram
	 */
	public UtilizationModel getUtilizationModelGddram() {
		return utilizationModelGddram;
	}

	/**
	 * Sets the utilization model of GDDRAM.
	 * 
	 * @param utilizationModelCpu the new utilization model of gddram
	 */
	public void setUtilizationModelGddram(final UtilizationModel utilizationModelGddram) {
		this.utilizationModelGddram = utilizationModelGddram;
	}

	/**
	 * Gets the utilization model of GDDRAM BW.
	 * 
	 * @return the utilization model GDDRAM BW
	 */
	public UtilizationModel getUtilizationModelBw() {
		return utilizationModelBw;
	}

	/**
	 * Sets the utilization model of GDDRAM BW.
	 * 
	 * @param utilizationModelBw the new utilization model of GDDRAM BW
	 */
	public void setUtilizationModelBw(final UtilizationModel utilizationModelBw) {
		this.utilizationModelBw = utilizationModelBw;
	}

	/**
	 * Gets the utilization percentage of gpu.
	 * 
	 * @param time the time
	 * @return the utilization of gpu
	 */
	public double getUtilizationOfGpu(final double time) {
		return getUtilizationModelGpu().getUtilization(time);
	}

	/**
	 * Gets the utilization percentage of memory.
	 * 
	 * @param time the time
	 * @return the utilization of memory
	 */
	public double getUtilizationOfGddram(final double time) {
		return getUtilizationModelGddram().getUtilization(time);
	}

	/**
	 * Gets the utilization percentage of GDDRAM bw.
	 * 
	 * @param time the time
	 * @return the utilization of GDDRAM bw
	 */
	public double getUtilizationOfBw(final double time) {
		return getUtilizationModelBw().getUtilization(time);
	}

	/**
	 * @return the requestedGddramSize
	 */
	public long getRequestedGddramSize() {
		return requestedGddramSize;
	}

}
