package cz.vutbr.web.csskit;

import cz.vutbr.web.css.CSSNodeVisitor;
import cz.vutbr.web.css.Declaration;
import cz.vutbr.web.css.RuleCounterStyle;

public class RuleCounterStyleImpl extends AbstractRuleBlock<Declaration> implements RuleCounterStyle {
	
	private String name;

	public RuleCounterStyleImpl(String name) {
		super();
		this.name = name;
	}

	@Override
	public String toString(int depth) {
		return "cool";
	}
	
	

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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
