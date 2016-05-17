package cz.vutbr.web.css;

public interface RuleKeyframes extends RuleBlock<RuleSet>, PrettyOutput {

	public String getName();	
	
	public void setName(String name);
	
}