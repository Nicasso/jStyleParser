package cz.vutbr.web.csskit;

import cz.vutbr.web.css.CSSNodeVisitor;
import cz.vutbr.web.css.RuleNameSpace;
import cz.vutbr.web.css.RuleSet;

public class RuleNameSpaceImpl extends AbstractRuleBlock<RuleSet> implements RuleNameSpace {
	
	private String prefix;
	private String uri;
	
	public RuleNameSpaceImpl(String prefix, String uri) {
		this.prefix = prefix;
		this.uri = uri;
	}

	@Override
	public String toString(int depth) {
		return "COOL";
	}
	
	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	/**
	 * Accept method required by the visitor pattern for traversing the CSS Tree. 
	 * 
	 * @param visitor
	 * 	The visitor interface
	 * @return
	 * 	The current CSS Object
	 */
	@Override
	public Object accept(CSSNodeVisitor visitor) {
		return visitor.visit(this);
	}
	
}
