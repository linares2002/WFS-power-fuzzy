package org.workflowsim.fuzzy;

public class FuzzyVariable
{
	private String name;
	private String weigth;
	
	public FuzzyVariable()
	{
		name = "";
		weigth = "";
	}
	
	public FuzzyVariable(String name, String weigth)
	{
		this.name = name;
		this.weigth = weigth;
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public String getWeigth()
	{
		return weigth;
	}
	
	public void setWeigth(String weigth)
	{
		this.weigth = weigth;
	}
}