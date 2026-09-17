# GPUCloudSim: An Extension Of CloudSim For Modeling And Simulation Of GPUs In Cloud Data Centers #

In order to satisfy graphical and computational requirements of end-users, today cloud providers offer GPU-enabled services. Evaluating new GPU provisioning, scheduling and placement policies on real GPU-equipped testbeds is costly, limits experiments to the scale of the testbed and makes the reproduction of results difficult. Hence, simulation may be used.

GPUCloudSim extends [CloudSim](http://cloudbus.org/cloudsim/) with models and policies for GPUs in cloud data centers, from video cards and physical GPUs (pGPUs) to virtual GPUs (vGPUs) and GPU applications, allowing its users to focus on GPU-specific design issues that they want to investigate.


# Main features #

  * Support for modeling and simulation of GPU-equipped hosts with multiple video cards and pGPUs
  * Support for modeling and simulation of GPU-enabled VMs and GPU applications
  * Support for modeling and simulation of CPU-GPU memory transfers over PCIe
  * Support for modeling and simulation of NVIDIA GRID K1 and K2 boards, vGPU types and pass-through
  * Support for user-defined GPU memory, GPU memory bandwidth and PCIe bandwidth provisioning policies
  * Support for user-defined vGPU scheduling policies, including space-shared, time-shared and fair-share
  * Support for user-defined GPU task scheduling policies
  * Support for user-defined video card allocation and pGPU selection policies
  * Support for user-defined GPU-aware VM placement policies and placement windows
  * Support for modeling and simulation of GPU remoting ([Siavashi et al. 2023](https://doi.org/10.1016/j.jpdc.2022.10.008))
  * Support for modeling and simulation of energy-aware GPU-equipped data centers
  * Support for modeling and simulation of vGPU virtualization overhead
  * Support for modeling and simulation of interference among co-running GPU tasks


# Download #

This package contains CloudSim 7.0.1 with the latest version of the GPU extension. The extension is located in [`org.cloudbus.cloudsim.gpu`](modules/cloudsim/src/main/java/org/cloudbus/cloudsim/gpu) and its examples in [`org.cloudbus.cloudsim.examples.gpu`](modules/cloudsim-examples/src/main/java/org/cloudbus/cloudsim/examples/gpu).


# Publications #

If you used the extension, please consider citing the following paper,

  * **Siavashi, A., Momtazpour, M. GPUCloudSim: an extension of CloudSim for modeling and simulation of GPUs in cloud data centers. J Supercomput 75, 2535–2561 (2019).**


# Disclaimer #

This code is provided as is, and no guarantee is given that this code will perform in the desired way.
