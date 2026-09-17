package org.cloudbus.cloudsim.gpu;

import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;

/**
 * To represent an application with both host and device execution
 * requirements, GpuCloudlet extends {@link Cloudlet} to include a
 * {@link GpuTask}.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class GpuCloudlet extends Cloudlet {

	/**
	 * A tag associated with the GpuCloudlet. A tag can be used to describe the
	 * application.
	 */
	private String tag;

	/**
	 * The GPU part of the application.
	 */
	private GpuTask gpuTask;

	/**
	 * Create a GpuCloudlet. {@link Cloudlet} represents the host portion of the
	 * application while {@link GpuTask} represents the device portion.
	 * 
	 * @param gpuCloudletId       gpuCloudlet id
	 * @param cloudletLength      length of the host portion
	 * @param pesNumber           number of threads
	 * @param cloudletFileSize    size of the application
	 * @param cloudletOutputSize  size of the application when executed
	 * @param utilizationModelCpu CPU utilization model of host portion
	 * @param utilizationModelRam RAM utilization model of host portion
	 * @param utilizationModelBw  BW utilization model of host portion
	 * @param gpuTask             the device portion of the application
	 */
	public GpuCloudlet(int gpuCloudletId, long cloudletLength, int pesNumber, long cloudletFileSize,
			long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam,
			UtilizationModel utilizationModelBw, GpuTask gpuTask) {
		super(gpuCloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu,
				utilizationModelRam, utilizationModelBw);
		setGpuTask(gpuTask);
	}

	/**
	 * Create a GpuCloudlet. {@link Cloudlet} represents the host portion of the
	 * application while {@link GpuTask} represents the device portion.
	 * 
	 * @param gpuCloudletId       gpuCloudlet id
	 * @param cloudletLength      length of the host portion
	 * @param pesNumber           number of threads
	 * @param cloudletFileSize    size of the application
	 * @param cloudletOutputSize  size of the application when executed
	 * @param utilizationModelCpu CPU utilization model of host portion
	 * @param utilizationModelRam RAM utilization model of host portion
	 * @param utilizationModelBw  BW utilization model of host portion
	 * @param gpuTask             the device portion of the application
	 * @param record
	 * @param fileList
	 */
	public GpuCloudlet(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize,
			long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam,
			UtilizationModel utilizationModelBw, GpuTask gpuTask, boolean record, List<String> fileList) {
		super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu,
				utilizationModelRam, utilizationModelBw, record, fileList);
		setGpuTask(gpuTask);
	}

	/**
	 * Create a GpuCloudlet. {@link Cloudlet} represents the host portion of the
	 * application while {@link GpuTask} represents the device portion.
	 * 
	 * @param gpuCloudletId       gpuCloudlet id
	 * @param cloudletLength      length of the host portion
	 * @param pesNumber           number of threads
	 * @param cloudletFileSize    size of the application
	 * @param cloudletOutputSize  size of the application when executed
	 * @param utilizationModelCpu CPU utilization model of host portion
	 * @param utilizationModelRam RAM utilization model of host portion
	 * @param utilizationModelBw  BW utilization model of host portion
	 * @param gpuTask             the device portion of the application
	 * @param fileList
	 */
	public GpuCloudlet(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize,
			long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam,
			UtilizationModel utilizationModelBw, GpuTask gpuTask, List<String> fileList) {
		super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu,
				utilizationModelRam, utilizationModelBw, fileList);
		setGpuTask(gpuTask);
	}

	/**
	 * Create a GpuCloudlet. {@link Cloudlet} represents the host portion of the
	 * application while {@link GpuTask} represents the device portion.
	 * 
	 * @param gpuCloudletId       gpuCloudlet id
	 * @param cloudletLength      length of the host portion
	 * @param pesNumber           number of threads
	 * @param cloudletFileSize    size of the application
	 * @param cloudletOutputSize  size of the application when executed
	 * @param utilizationModelCpu CPU utilization model of host portion
	 * @param utilizationModelRam RAM utilization model of host portion
	 * @param utilizationModelBw  BW utilization model of host portion
	 * @param gpuTask             the device portion of the application
	 * @param record
	 */
	public GpuCloudlet(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize,
			long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam,
			UtilizationModel utilizationModelBw, GpuTask gpuTask, boolean record) {
		super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu,
				utilizationModelRam, utilizationModelBw, record);
		setGpuTask(gpuTask);
	}

	/**
	 * Create a GpuCloudlet. {@link Cloudlet} represents the host portion of the
	 * application.
	 * 
	 * @param gpuCloudletId       gpuCloudlet id
	 * @param cloudletLength      length of the host portion
	 * @param pesNumber           number of threads
	 * @param cloudletFileSize    size of the application
	 * @param cloudletOutputSize  size of the application when executed
	 * @param utilizationModelCpu CPU utilization model of host portion
	 * @param utilizationModelRam RAM utilization model of host portion
	 * @param utilizationModelBw  BW utilization model of host portion
	 * @param record
	 */
	public GpuCloudlet(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize,
			long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam,
			UtilizationModel utilizationModelBw, boolean record) {
		super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu,
				utilizationModelRam, utilizationModelBw, record);
	}

	/**
	 * @return the device portion
	 */
	public GpuTask getGpuTask() {
		return gpuTask;
	}

	/**
	 * @return true if the GpuCloudlet has a device portion
	 */
	public boolean hasGpuTask() {
		return getGpuTask() != null;
	}

	/**
	 * @param gpuTask the device portion
	 */
	protected void setGpuTask(GpuTask gpuTask) {
		this.gpuTask = gpuTask;
		if (gpuTask != null && gpuTask.getCloudlet() == null) {
			gpuTask.setCloudlet(this);
		}
	}

	/**
	 * @return the tag
	 */
	public String getTag() {
		return tag;
	}

	/**
	 * @param tag the tag to set
	 */
	public void setTag(String tag) {
		this.tag = tag;
	}

}
