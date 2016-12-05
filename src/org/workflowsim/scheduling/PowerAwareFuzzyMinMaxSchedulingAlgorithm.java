package org.workflowsim.scheduling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.power.PowerHost;
import org.workflowsim.CondorVM;
import org.workflowsim.WorkflowSimTags;
import org.workflowsim.fuzzy.FuzzyEntity;
import org.workflowsim.fuzzy.tasks.GeneralParametersTasksFuzzy;
import org.workflowsim.fuzzy.tasks.WorkflowSimTasksFUZZY;

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
 * Modifies PowerAwareSequentialSchedulingAlgorithm to be an adjustment
 * of the MinMin classic algorithm but being power-aware. As MinMin, this
 * algorithm first find the shortest Cloudlet and schedule it to the VM
 * that achieves the lowest energy estimation, instead of lowest time.
 * 
 */

public class PowerAwareFuzzyMinMaxSchedulingAlgorithm extends BaseSchedulingAlgorithm
{
	public PowerAwareFuzzyMinMaxSchedulingAlgorithm()
	{
        super();
    }
	private final List<Boolean> hasChecked = new ArrayList<>();
	
    @Override
    public void run()
    {
    	int cloudletSize = getCloudletList().size();
    	hasChecked.clear();
        for (int t = 0; t < cloudletSize; t++)
        {
            hasChecked.add(false);
        }
    	int vmSize = getVmList().size();
        for (int c = 0; c < cloudletSize; c++)
        {
        	CondorVM selectedVm = (CondorVM) null;
        	double maximumSelection = -1;
        	double fuzzySelection = 0;
        	int minIndex = 0;
            Cloudlet minCloudlet = null;
            //Minimum initialization
            for (int j = 0; j < cloudletSize; j++)
            {
                Cloudlet cloudlet = (Cloudlet) getCloudletList().get(j);
                if (!hasChecked.get(j))
                {
                    minCloudlet = cloudlet;
                    minIndex = j;
                    break;
                }
            }
            //Check errors
            if (minCloudlet == null)
            {
                break;
            }
            //Selects the shortest Cloudlet
            for (int j = 0; j < cloudletSize; j++)
            {
                Cloudlet cloudlet = (Cloudlet) getCloudletList().get(j);
                if (hasChecked.get(j))
                {
                    continue;
                }
                long length = cloudlet.getCloudletLength();
                if (length < minCloudlet.getCloudletLength())
                {
                    minCloudlet = cloudlet;
                    minIndex = j;
                }
            }
            hasChecked.set(minIndex, true);
            //Find the lowest power consumption for the minimum Cloudlet
        	for (int v = 0; v < vmSize; v++)
        	{
        		CondorVM vm = (CondorVM) getVmList().get(v);
        		if (vm.getState() == WorkflowSimTags.VM_STATUS_IDLE)
        		{
        			fuzzySelection = getFuzzySelection(minCloudlet, vm);
        			if (fuzzySelection > maximumSelection)
        			{
        				maximumSelection = fuzzySelection;
        				selectedVm = vm;
        			}
        		}
        	}
        	if (selectedVm == null)
        	{
                break;
        	}
        	selectedVm.setState(WorkflowSimTags.VM_STATUS_BUSY);
            minCloudlet.setVmId(selectedVm.getId());
            getScheduledList().add(minCloudlet);
        }
    }
    
    public double getFuzzySelection(Cloudlet cloudlet, CondorVM vm)
	{
		
		if (GeneralParametersTasksFuzzy.getMaxTime() == 0)
		{
			GeneralParametersTasksFuzzy.setMaxTime(GeneralParametersTasksFuzzy.getMaxLength() / GeneralParametersTasksFuzzy.getMinVmMips());
			GeneralParametersTasksFuzzy.setMinTime(GeneralParametersTasksFuzzy.getMinLength() / GeneralParametersTasksFuzzy.getMaxVmMips());
			GeneralParametersTasksFuzzy.setMaxEnergy(GeneralParametersTasksFuzzy.getMaxPower() * GeneralParametersTasksFuzzy.getMaxTime());
			GeneralParametersTasksFuzzy.setMinEnergy(GeneralParametersTasksFuzzy.getMinPower() * GeneralParametersTasksFuzzy.getMinTime());
		}
		
		//Parameters calculation
		double energy = 0;
    	double mips = vm.getMips();
    	PowerHost host = (PowerHost)vm.getHost();
    	/*
    	 * The power will depend on the utilization
    	 * and the current frequency index selected in the DVFS.
    	 */
    	double power = host.getPower();
    	//Length is expressed in MI.
    	double length = cloudlet.getCloudletLength();
    	//Execution time of the Cloudlet in this VM.
    	double time = length / mips;
    	/*
    	 * The real energy consumption will depend on the length of the Cloudlet,
    	 * and so on the time spent in its execution.
    	 */
    	energy = power * time;
		
    	//FCL file read
		double selection = 0;
		
		//Normalize Antecedents to range [0, 10]
		List<Double> toNormalise = new ArrayList<Double>();
		List<Double> normalised = new ArrayList<Double>();
		toNormalise.add(mips);			//0
		toNormalise.add(power);			//1
		toNormalise.add(length);		//2
		toNormalise.add(time);			//3
		toNormalise.add(energy);		//4
		normalised = normaliseAntecedents(toNormalise);
		
		FunctionBlock functionBlock = (FunctionBlock)null;
		
		if(WorkflowSimTasksFUZZY.getFunctionBlock() == null)
		{
			String fileName = WorkflowSimTasksFUZZY.getPath();
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
			FuzzyEntity entity = WorkflowSimTasksFUZZY.getEntity();
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
			
			WorkflowSimTasksFUZZY.setFunctionBlock(functionBlock);
		}
		else
		{
			functionBlock = WorkflowSimTasksFUZZY.getFunctionBlock();
		}
		
		//Set inputs
		functionBlock.setVariable("MIPS", normalised.get(0));
		functionBlock.setVariable("power", normalised.get(1));
		functionBlock.setVariable("length", normalised.get(2));
		functionBlock.setVariable("time", normalised.get(3));
		functionBlock.setVariable("energy", normalised.get(4));
		
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
		normalised.add((toNormalise.get(0) - GeneralParametersTasksFuzzy.getMinVmMips()) /
				(GeneralParametersTasksFuzzy.getMaxVmMips() - GeneralParametersTasksFuzzy.getMinVmMips()) * 10);
		// 1) Power
		normalised.add((toNormalise.get(1) - GeneralParametersTasksFuzzy.getMinPower()) / 
				(GeneralParametersTasksFuzzy.getMaxPower() - GeneralParametersTasksFuzzy.getMinPower()) * 10);
		// 2) Length
		normalised.add((toNormalise.get(2) - GeneralParametersTasksFuzzy.getMinLength())/
				(GeneralParametersTasksFuzzy.getMaxLength() - GeneralParametersTasksFuzzy.getMinLength()) * 10);
		// 3) Time
		normalised.add((toNormalise.get(3) - GeneralParametersTasksFuzzy.getMinTime()) /
				(GeneralParametersTasksFuzzy.getMaxTime() - GeneralParametersTasksFuzzy.getMinTime()) * 10);
		// 4) Energy
		normalised.add((toNormalise.get(4) - GeneralParametersTasksFuzzy.getMinEnergy()) /
				(GeneralParametersTasksFuzzy.getMaxEnergy() - GeneralParametersTasksFuzzy.getMinEnergy()) * 10);
		return normalised;
	}
}