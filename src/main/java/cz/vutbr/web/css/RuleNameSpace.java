package cz.vutbr.web.css;

public interface RuleNameSpace extends RuleBlock<RuleSet>, PrettyOutput {

	public String getPrefix();
	public String getUri();
	
}
