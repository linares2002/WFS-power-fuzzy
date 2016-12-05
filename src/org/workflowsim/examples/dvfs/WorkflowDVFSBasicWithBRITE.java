/**
 * Copyright 2012-2013 University Of Southern California
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.workflowsim.examples.dvfs;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.HarddriveStorage;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.NetworkTopology;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicySimpleWattPerMipsMetric;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPower_BAZAR;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.xml.DvfsDatas;
import org.workflowsim.CondorVM;
import org.workflowsim.Job;
import org.workflowsim.Task;
import org.workflowsim.WorkflowEngine;
import org.workflowsim.WorkflowPlanner;
import org.workflowsim.dvfs.WorkflowDVFSDatacenter;
import org.workflowsim.utils.ClusteringParameters;
import org.workflowsim.utils.OverheadParameters;
import org.workflowsim.utils.Parameters;
import org.workflowsim.utils.Parameters.ClassType;
import org.workflowsim.utils.ReplicaCatalog;

/**
 * This WorkflowSimExample creates a workflow planner, a workflow engine,
 * one scheduler, one datacenter and 20 vms. You should change daxPath at
 * least. You may change other parameters as well.
 *
 * @author Weiwei Chen
 * @since WorkflowSim Toolkit 1.0
 * @date Apr 9, 2013
 */
public class WorkflowDVFSBasicWithBRITE {
	
    ////////////////////////// STATIC METHODS ///////////////////////
    /**
     * Creates main() to run this example. This example has only one datacenter
     * and one storage
     */
	
	private static DvfsDatas ConfigDvfs;
	
    public static void main(String[] args) {
        try {
        	
        	//Log.disable();
        	
            // First step: Initialize the WorkflowSim package.
            /**
             * However, the exact number of vms may not necessarily be vmNum If
             * the data center or the host doesn't have sufficient resources the
             * exact vmNum would be smaller than that. Take care.
             */
            int vmNum = 20; //number of vms;
            /**
             * Should change this based on real physical path
             */
            
            String daxPath = "C:/Users/LabInv1/Downloads/WorkflowSim-1.0/config/dax/Montage_25.xml";
            File daxFile = new File(daxPath);
            if (!daxFile.exists()) {
            	Log.printLine("Warning: Please replace daxPath with the physical path in your working environment!");
                return;
            }
            
            /**
             * Since we are using MINMIN scheduling algorithm, the planning
             * algorithm should be INVALID such that the planner would not
             * override the result of the scheduler.
             */
            Parameters.SchedulingAlgorithm sch_method = Parameters.SchedulingAlgorithm.MINMIN;
            Parameters.PlanningAlgorithm pln_method = Parameters.PlanningAlgorithm.INVALID;
            ReplicaCatalog.FileSystem file_system = ReplicaCatalog.FileSystem.SHARED;
            
            /**
             * No overheads
             */
            OverheadParameters op = new OverheadParameters(0, null, null, null, null, 0);
            
            /**
             * No Clustering
             */
            ClusteringParameters.ClusteringMethod method = ClusteringParameters.ClusteringMethod.NONE;
            ClusteringParameters cp = new ClusteringParameters(0, 0, method, null);
            
            /**
             * Initialize static parameters
             */
            Parameters.init(vmNum, daxPath, null,
                    null, op, cp, sch_method, pln_method,
                    null, 0);
            ReplicaCatalog.init(file_system);
            
            // First step: Initialize the CloudSim package. It should be called
            // before creating any entities.
            int num_user = 1; // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; // mean trace events
            
            // Initialize the CloudSim library
            CloudSim.init(num_user, calendar, trace_flag);
            
            WorkflowDVFSDatacenter datacenter0 = createDatacenter("Datacenter_0");
            datacenter0.setDisableMigrations(true);
            Log.printLine("WorkflowDatacenter Entity's ID: " + datacenter0.getId());
            
            /**
             * Create a WorkflowPlanner with one schedulers.
             */
            WorkflowPlanner wfPlanner = new WorkflowPlanner("planner_0", 1);
            Log.printLine("WorkflowPlanner Entity's ID: " + wfPlanner.getId());
            Log.printLine("ClusteringEngine Entity's ID: " + wfPlanner.getClusteringEngineId());
            Log.printLine("WorkflowEngine Entity's ID: " + wfPlanner.getWorkflowEngineId());
            Log.printLine("WorkflowScheduler Entity's ID: " + wfPlanner.getWorkflowEngine().getSchedulerId(0));
            /**
             * Create a WorkflowEngine.
             */
            WorkflowEngine wfEngine = wfPlanner.getWorkflowEngine();
            /**
             * Create a list of VMs. The userId of a vm is basically the id of
             * the scheduler that controls this vm.
             */
            List<CondorVM> vmlist0 = createVM(wfEngine.getSchedulerId(0), Parameters.getVmNum());
            
            /**
             * Submits this list of vms to this WorkflowEngine.
             */
            wfEngine.submitVmList(vmlist0, 0);
            
            /**
             * Binds the data centers with the scheduler.
             */
            wfEngine.bindSchedulerDatacenter(datacenter0.getId(), 0);
            Log.printLine("Total number of entities: " + CloudSim.getNumEntities());
            
            //Sixth step: configure network
			//load the network topology file
			NetworkTopology.buildNetworkTopology("brite/topology.brite");

			//maps CloudSim entities to BRITE nodes
			//WorkflowDatacenter will correspond to BRITE node 0
			int briteNode = 0;
			NetworkTopology.mapNode(datacenter0.getId(), briteNode);

			//WorkflowPlanner will correspond to BRITE node 1
			briteNode = 1;
			NetworkTopology.mapNode(wfPlanner.getId(), briteNode);
			
			//ClusteringEngine will correspond to BRITE node 2
			briteNode = 2;
			NetworkTopology.mapNode(wfPlanner.getClusteringEngineId(), briteNode);
			
			//WorkflowEngine will correspond to BRITE node 3
			briteNode = 3;
			NetworkTopology.mapNode(wfPlanner.getWorkflowEngineId(), briteNode);
			
			//WorkflowScheduler will correspond to BRITE node 4
			briteNode = 4;
			NetworkTopology.mapNode(wfPlanner.getWorkflowEngine().getSchedulerId(0), briteNode);
            
            double lastClock = CloudSim.startSimulation();
            List<Job> outputList0 = wfEngine.getJobsReceivedList();
            CloudSim.stopSimulation();
            
            Log.enable();
            
            Log.printLine();
			
            Log.printLine(String.format("Total simulation time: %.2f sec", lastClock));
            Log.printLine(String.format("Power Sum: %.8f W", datacenter0.getPower()));
            Log.printLine(String.format("Power Average: %.8f W", datacenter0.getPower() / (lastClock*100)));
            Log.printLine(String.format("Energy consumption: %.8f Wh", (datacenter0.getPower() / (lastClock*100)) * (lastClock*100 / 3600)));
			
            Log.printLine();
			
            printJobList(outputList0);
        } catch (Exception e) {
        	Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }
    
    protected static WorkflowDVFSDatacenter createDatacenter(String name) {
    	
        // Here are the steps needed to create a PowerDatacenter:
        // 1. We need to create a list to store one or more
        //    Machines
        List<PowerHost> hostList = new ArrayList<>();
        
        // 2. A Machine contains one or more PEs or CPUs/Cores. Therefore, should
        //    create a list to store these PEs before creating
        //    a Machine.
        for (int i = 1; i <= 20; i++) {
            List<Pe> peList1 = new ArrayList<>();
            double maxPower = 250; // 250W
    		double staticPowerPercent = 0.7; // 70%
            int mips = 1500;
            
            boolean enableDVFS = true; // is the Dvfs enable on the host
    		ArrayList<Double> freqs = new ArrayList<>(); // frequencies available by the CPU
    		freqs.add(59.925); // frequencies are defined in % , it make free to use Host MIPS like we want.
    		freqs.add(69.93);  // frequencies must be in increase order !
    		freqs.add(79.89);
    		freqs.add(89.89);
    		freqs.add(100.0);
    		
    		HashMap<Integer,String> govs = new HashMap<Integer,String>();  // Define wich governor is used by each CPU
    		govs.put(0, "ondemand");  // CPU 1 use OnDemand Dvfs mode
    		//govs.put(0, "powersave");
    		//govs.put(0, "performance");
    		//govs.put(0, "conservative");
    		
    		ConfigDvfs = new DvfsDatas();
			HashMap<String,Integer> tmp_HM_OnDemand = new HashMap<>();
			tmp_HM_OnDemand.put("up_threshold", 95);
			tmp_HM_OnDemand.put("sampling_down_factor", 100);
			HashMap<String,Integer> tmp_HM_Conservative = new HashMap<>();
			tmp_HM_Conservative.put("up_threshold", 80);
			tmp_HM_Conservative.put("down_threshold", 20);
			tmp_HM_Conservative.put("enablefreqstep", 0);
			tmp_HM_Conservative.put("freqstep", 5);
			HashMap<String,Integer> tmp_HM_UserSpace = new HashMap<>();
			tmp_HM_UserSpace.put("frequency", 3);
			ConfigDvfs.setHashMapOnDemand(tmp_HM_OnDemand);
			ConfigDvfs.setHashMapConservative(tmp_HM_Conservative);
			ConfigDvfs.setHashMapUserSpace(tmp_HM_UserSpace);
    		
            // 3. Create PEs and add these into the list.
            //for a quad-core machine, a list of 4 PEs is required:
            peList1.add(new Pe(0, new PeProvisionerSimple(mips), freqs, govs.get(0), ConfigDvfs)); // need to store Pe id and MIPS Rating
            //peList1.add(new Pe(1, new PeProvisionerSimple(mips), freqs, govs.get(0), ConfigDvfs));//
            
            int hostId = i;
            int ram = 2048; //host memory (MB)
            long storage = 1000000; //host storage
            int bw = 10000;
            hostList.add(
            	new PowerHost(
                	hostId,
                	new RamProvisionerSimple(ram),
                	new BwProvisionerSimple(bw),
                	storage,
                	peList1,
                	new VmSchedulerTimeShared(peList1),
                	new PowerModelSpecPower_BAZAR(peList1),
					false,
					enableDVFS
                )
            ); 	// This is our first machine
            	//hostId++;
        }
        
        // 4. Create a DatacenterCharacteristics object that stores the
        //    properties of a data center: architecture, OS, list of
        //    Machines, allocation policy: time- or space-shared, time zone
        //    and its price (G$/Pe time unit).
        String arch = "x86";      		// system architecture
        String os = "Linux";          	// operating system
        String vmm = "Xen";
        double time_zone = 10.0;        // time zone this resource located
        double cost = 3.0;              // the cost of using processing in this resource
        double costPerMem = 0.05;		// the cost of using memory in this resource
        double costPerStorage = 0.1;	// the cost of using storage in this resource
        double costPerBw = 0.1;			// the cost of using bw in this resource
        LinkedList<Storage> storageList = new LinkedList<>();	//we are not adding SAN devices by now
        WorkflowDVFSDatacenter datacenter = null;
        
        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);
        
        // 5. Finally, we need to create a storage object.
        /**
         * The bandwidth within a data center in MB/s.
         */
        int maxTransferRate = 15; // the number comes from the futuregrid site, you can specify your bw
        
        try {
            // Here we set the bandwidth to be 15MB/s
            HarddriveStorage s1 = new HarddriveStorage(name, 1e12);
            s1.setMaxTransferRate(maxTransferRate);
            storageList.add(s1);
            datacenter = new WorkflowDVFSDatacenter(name, characteristics, new PowerVmAllocationPolicySimpleWattPerMipsMetric(hostList), storageList, 0.01);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return datacenter;
    }
    
    protected static List<CondorVM> createVM(int userId, int vms) {
        //Creates a container to store VMs. This list is passed to the broker later
        LinkedList<CondorVM> list = new LinkedList<>();
        
        //VM Parameters
        long size = 10000; //image size (MB)
        int ram = 512; //vm memory (MB)
        int mips = 1000;
        long bw = 1000;
        int pesNumber = 1; //number of cpus
        String vmm = "Xen"; //VMM name
        
        //create VMs
        CondorVM[] vm = new CondorVM[vms];
        for (int i = 0; i < vms; i++) {
            double ratio = 1.0;
            vm[i] = new CondorVM(i, userId, mips * ratio, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared());
            list.add(vm[i]);
        }
        return list;
    }
    
    /**
     * Prints the job objects
     *
     * @param list list of jobs
     */
    protected static void printJobList(List<Job> list) {
        //String indent = "    ";
        String indent = "\t";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Job ID" + indent + indent + "Task ID" + indent + indent + "STATUS" + indent + indent
                + "Data center ID" + indent + "VM ID" + indent + indent
                + "Time" + indent + indent + "Start Time" + indent + "Finish Time" + indent + "Depth");
        DecimalFormat dft = new DecimalFormat("###.##");
        for (Job job : list) {
        	Log.print(job.getCloudletId() + indent + indent);
            if (job.getClassType() == ClassType.STAGE_IN.value) {
            	Log.print("Stage-in");
                if (job.getCloudletStatus() == Cloudlet.SUCCESS) {
                	Log.print(indent + "SUCCESS");
                	Log.printLine(indent + indent + job.getResourceId() + indent + indent + job.getVmId()
                            + indent + indent + dft.format(job.getActualCPUTime())
                            + indent + indent + dft.format(job.getExecStartTime()) + indent + indent
                            + dft.format(job.getFinishTime()) + indent + indent + job.getDepth());
                } else if (job.getCloudletStatus() == Cloudlet.FAILED) {
                	Log.print("FAILED");
                	Log.printLine(indent + indent + job.getResourceId() + indent + indent + job.getVmId()
                            + indent + indent + dft.format(job.getActualCPUTime())
                            + indent + indent + dft.format(job.getExecStartTime()) + indent + indent
                            + dft.format(job.getFinishTime()) + indent + indent + job.getDepth());
                }
            }
            for (Task task : job.getTaskList()) {
            	Log.print(task.getCloudletId());
                if (job.getCloudletStatus() == Cloudlet.SUCCESS) {
                	Log.print(indent + indent + "SUCCESS");
                	Log.printLine(indent + indent + job.getResourceId() + indent + indent + job.getVmId()
                            + indent + indent + dft.format(job.getActualCPUTime())
                            + indent + indent + dft.format(job.getExecStartTime()) + indent + indent
                            + dft.format(job.getFinishTime()) + indent + indent + job.getDepth());
                } else if (job.getCloudletStatus() == Cloudlet.FAILED) {
                	Log.print("FAILED");
                	Log.printLine(indent + indent + job.getResourceId() + indent + indent + job.getVmId()
                            + indent + indent + dft.format(job.getActualCPUTime())
                            + indent + indent + dft.format(job.getExecStartTime()) + indent + indent
                            + dft.format(job.getFinishTime()) + indent + indent + job.getDepth());
                }
            }
            //Verbose.toPrint(indent);
            
            
        }
    }
}