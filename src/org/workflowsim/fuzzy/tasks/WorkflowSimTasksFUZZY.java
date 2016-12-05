package org.workflowsim.fuzzy.tasks;

import org.workflowsim.fuzzy.*;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import net.sourceforge.jFuzzyLogic.FunctionBlock;

public final class WorkflowSimTasksFUZZY
{
	private static FunctionBlock funcBlock = null;
	private static FuzzyEntity fuzzyEntity;
	private static String fclPath;
	
	private WorkflowSimTasksFUZZY(){}
	
	public static FuzzyEntity getEntity()
	{
		return fuzzyEntity;
	}
	
	public static void setPath(String fcl)
	{
		fclPath = fcl;
	}
	
	public static String getPath()
	{
		return fclPath;
	}
	
	public static FunctionBlock getFunctionBlock()
	{
		return funcBlock;
	}
	
	public static void setFunctionBlock(FunctionBlock funcBlock)
	{
		WorkflowSimTasksFUZZY.funcBlock = funcBlock;
	}
	
	public static void decodeJSONRules(String encodedRules)
	{
		JSONObject obj = new JSONObject(encodedRules);
		JSONArray rules = obj.getJSONArray("rules");
		FuzzyEntity entity;
		FuzzyRule rule;
		FuzzyVariable variable;
		List<String> antecedentNames = new ArrayList<String>();
		antecedentNames.add("MIPS");
		antecedentNames.add("power");
		antecedentNames.add("length");
		antecedentNames.add("time");
		antecedentNames.add("energy");
		String consequentName = "selection";
		List<String> antecedentWeigths = new ArrayList<String>();
		antecedentWeigths.add("low");
		antecedentWeigths.add("normal");
		antecedentWeigths.add("high");
		List<String> consequentWeigths = new ArrayList<String>();
		consequentWeigths.add("very_low");
		consequentWeigths.add("low");
		consequentWeigths.add("normal");
		consequentWeigths.add("high");
		consequentWeigths.add("very_high");
		entity = new FuzzyEntity();
		int weigth;
		for (int i = 0; i < rules.length(); i++)
		{
			rule = new FuzzyRule();
			for (int k = 0; k < antecedentNames.size(); k++)
			{
				variable = new FuzzyVariable();
				variable.setName(antecedentNames.get(k));
				weigth = Integer.parseInt(rules.getJSONObject(i).getString(antecedentNames.get(k))) - 1;
				variable.setWeigth(antecedentWeigths.get(weigth));
				rule.addAntecedent(variable);
			}
			variable = new FuzzyVariable();
			variable.setName(consequentName);
			weigth = Integer.parseInt(rules.getJSONObject(i).getString(consequentName)) - 1;
			variable.setWeigth(consequentWeigths.get(weigth));
			rule.setConsequent(variable);
			entity.addRule(rule);
		}
		fuzzyEntity = entity;
	}
}