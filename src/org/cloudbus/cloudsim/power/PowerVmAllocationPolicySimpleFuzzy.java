/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.power;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Vm;
import org.workflowsim.fuzzy.FuzzyEntity;
import org.workflowsim.fuzzy.vms.GeneralParametersVmsFuzzy;
import org.workflowsim.fuzzy.vms.WorkflowSimVmsFUZZY;

import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;
import net.sourceforge.jFuzzyLogic.rule.Rule;
import net.sourceforge.jFuzzyLogic.rule.RuleBlock;
import net.sourceforge.jFuzzyLogic.rule.RuleExpression;
import net.sourceforge.jFuzzyLogic.rule.RuleTerm;
import net.sourceforge.jFuzzyLogic.rule.Variable;
import net.sourceforge.jFuzzyLogic.ruleAccumulationMethod.RuleAccumulationMethodMax;

/**
 *
 * This class uses a FRBS to choose a Host among all those available to hold a VM.
 * 
 * The aim is to choose the Host that have the best ratio between all the Antecedents,
 * being those vmMIPS, hostMIPS, utilization and power.
 * 
 */
public class PowerVmAllocationPolicySimpleFuzzy extends PowerVmAllocationPolicyAbstract
{
    public PowerVmAllocationPolicySimpleFuzzy(List<? extends Host> list)
    {
		super(list);
	}
    
    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
	
    @Override    
	public boolean allocateHostForVm(Vm vm)
	{
		double maxMetric = -1;
        PowerHost ChoosenHost = null;
        //Log.formatLine("In ChooseHostForNewVm function...");
        for (PowerHost host : this.<PowerHost> getHostList())
        {
        	double totalMips = 0;
        	for (Pe pe : host.getPeList())
        	{
        		totalMips += pe.getPeProvisioner().getAvailableMips();
        	}
        	
        	double tmpMetric;
        	if (totalMips >= vm.getMips())
        	{
        		tmpMetric = getFuzzyMetric(host, vm);
        	}
        	else
        	{
        		tmpMetric = 0;
        	}
        	//Log.printLine("MetricHost value for HOST #" + host.getId() + " = " + tmpMetric);
        	if(maxMetric != -1 && tmpMetric > maxMetric)
        	{
            	maxMetric = tmpMetric;
             	ChoosenHost = host;
         		//Log.printLine("Temp CHOSEN HOST is : Host #" + ChosenHost.getId());
        	}
        	else if(maxMetric == -1)
        	{
        		maxMetric = tmpMetric;
        		ChoosenHost = host;
        	}
        }
        
        Log.printLine("CHOSEN HOST for VM " + vm.getId() + " is : Host #" + ChoosenHost.getId());
        boolean allocationOK = false;
        allocationOK = allocateHostForVm(vm, ChoosenHost);
        
        if(allocationOK)
            return true;
        if(!allocationOK && ChoosenHost.isEnableDVFS())               
        {
            Log.printLine("Not enough free MIPS in the choosen HOST!");
            Log.printLine("Trying to decrease VMs size in this HOST!");
            ChoosenHost = TryDVFSEnableHost(ChoosenHost, vm);
            if(allocateHostForVm(vm, ChoosenHost))
            {
                Log.printLine("VMs size decreased successfully!");
                return true;
            }
            else
                Log.printLine("Error, VMs size not enough decreased successfully!");
        }
        return false;
	}
    
	private double getFuzzyMetric(PowerHost h, Vm vm)
	{
		
		// Get antecedent values
		double vmMIPS = vm.getMips();
		double hostMIPS = h.getTotalMips();
		double util = h.getUtilizationOfCpu();
		double power = h.getPower();
		
		double selection = 0;
		
		//Normalize Antecedents to range [0, 10]
		List<Double> toNormalise = new ArrayList<Double>();
		List<Double> normalised = new ArrayList<Double>();
		toNormalise.add(vmMIPS);		//0
		toNormalise.add(hostMIPS);		//1
		toNormalise.add(util);			//2
		toNormalise.add(power);			//3
		normalised = normaliseAntecedents(toNormalise);
		
		FunctionBlock functionBlock = (FunctionBlock)null;
		
		if(WorkflowSimVmsFUZZY.getFunctionBlock() == null)
		{
			String fileName = WorkflowSimVmsFUZZY.getPath();
			FIS fis = FIS.load(fileName, true);
			
			if (fis == null) // Error while loading?
			{
				System.err.println("Can't load file: '" + fileName + "'");
				return -1;
			}
			
			//Get function block
			functionBlock = fis.getFunctionBlock(null);
			
			RuleBlock ruleBlock = new RuleBlock(functionBlock);
			HashMap<String, RuleBlock> ruleBlocks = new HashMap<String, RuleBlock>();
			FuzzyEntity entity = WorkflowSimVmsFUZZY.getEntity();
			int rulesNum = entity.getRules().size();
			//List<Rule> rules = new ArrayList<Rule>();
			Rule rule;
			int antecedentsNum = normalised.size();
			Variable name;
			String weigth;
			List<RuleTerm> terms;
			//RuleTerm consequent;
			RuleExpression antecedents = new RuleExpression();
			for (int i = 0; i < rulesNum; i++)
			{
				rule = new Rule("Rule" + (i + 1), ruleBlock);
				terms = new ArrayList<RuleTerm>();
				//antecedentsNum = entity.getRule(i).getAntecedents().size();
				for (int j = 0; j < antecedentsNum; j++)
				{
					name = functionBlock.getVariable(entity.getRule(i).getAntecedent(j).getName());
					weigth = entity.getRule(i).getAntecedent(j).getWeigth();
					//Add all RuleTerms of the antecedents
					terms.add(new RuleTerm(name, weigth, false));
				}
				antecedents = new RuleExpression(terms.get(0), terms.get(1), antecedents.getRuleConnectionMethod());
				for (int k = 2; k < antecedentsNum; k++)
				{
					//Add the RuleTerms of the antecedents to the RuleExpression
					antecedents = new RuleExpression(antecedents, terms.get(k), antecedents.getRuleConnectionMethod());
				}
				//Add antecedents to the Rule
				rule.setAntecedents(antecedents);
				name = functionBlock.getVariable(entity.getRule(i).getConsequent().getName());
				weigth = entity.getRule(i).getConsequent().getWeigth();
				//Add consequent to the Rule
				rule.addConsequent(name, weigth, false);
				ruleBlock.add(rule);
			}
			ruleBlock.setRuleAccumulationMethod(new RuleAccumulationMethodMax());
			ruleBlocks.put("RuleBlock1", ruleBlock);
			functionBlock.setRuleBlocks(ruleBlocks);
			
			WorkflowSimVmsFUZZY.setFunctionBlock(functionBlock);
		}
		else
		{
			functionBlock = WorkflowSimVmsFUZZY.getFunctionBlock();
		}
		
		// Set inputs
		functionBlock.setVariable("vmMIPS", normalised.get(0));
		functionBlock.setVariable("hostMIPS", normalised.get(1));
		functionBlock.setVariable("utilization", normalised.get(2));
		functionBlock.setVariable("power", normalised.get(3));
		
		// Evaluate
		functionBlock.evaluate();
		
		// Get output evaluation
		selection = functionBlock.getVariable("selection").getValue();
		return selection;
	}
	
	private List<Double> normaliseAntecedents(List<Double> toNormalise)
	{
		List<Double> normalised = new ArrayList<Double>();
		// 0) VM MIPS
		normalised.add((toNormalise.get(0) - GeneralParametersVmsFuzzy.getMinVmMips()) /
				(GeneralParametersVmsFuzzy.getMaxVmMips() - GeneralParametersVmsFuzzy.getMinVmMips()) * 10);
		// 1) Host MIPS
		normalised.add((toNormalise.get(1) - GeneralParametersVmsFuzzy.getMinHostMips()) /
				(GeneralParametersVmsFuzzy.getMaxHostMips() - GeneralParametersVmsFuzzy.getMinHostMips()) * 10);
		// 2) Utilization
		normalised.add((toNormalise.get(2) - GeneralParametersVmsFuzzy.getMinUtil()) /
				(GeneralParametersVmsFuzzy.getMaxUtil() - GeneralParametersVmsFuzzy.getMinUtil()) * 10);
		// 3) Power
		normalised.add((toNormalise.get(3) - GeneralParametersVmsFuzzy.getMinPower()) /
				(GeneralParametersVmsFuzzy.getMaxPower() - GeneralParametersVmsFuzzy.getMinPower()) * 10);
		return normalised;
	}
}