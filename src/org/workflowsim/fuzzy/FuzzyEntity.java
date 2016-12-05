package org.workflowsim.fuzzy;

import java.util.ArrayList;
import java.util.List;

public final class FuzzyEntity
{
	private List<FuzzyRule> rules;
	
	public FuzzyEntity()
	{
		rules = new ArrayList<FuzzyRule>();
	}
	
	public void addRule(FuzzyRule rule)
	{
		rules.add(rule);
	}
	
	public FuzzyRule getRule(int index)
	{
		return rules.get(index);
	}
	
	public List<FuzzyRule> getRules()
	{
		return rules;
	}
}