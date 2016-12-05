package org.workflowsim.fuzzy;

import java.util.ArrayList;
import java.util.List;

public class FuzzyRule
{
	private List<FuzzyVariable> antecedents;
	private FuzzyVariable consequent;
	
	public FuzzyRule()
	{
		antecedents = new ArrayList<FuzzyVariable>();
		consequent = new FuzzyVariable();
	}
	
	public void addAntecedent(FuzzyVariable antecedent)
	{
		antecedents.add(antecedent);
	}
	
	public FuzzyVariable getAntecedent(int index)
	{
		return antecedents.get(index);
	}
	
	public List<FuzzyVariable> getAntecedents()
	{
		return antecedents;
	}
	
	public void setConsequent(FuzzyVariable consequent)
	{
		this.consequent = consequent;
	}
	
	public FuzzyVariable getConsequent()
	{
		return consequent;
	}
}