package cz.vutbr.web.csskit;

import cz.vutbr.web.css.CSSNodeVisitor;
import cz.vutbr.web.css.CodeLocation;
import cz.vutbr.web.css.TermCalc;

public class TermCalcImpl extends TermImpl<String> implements TermCalc {

	protected TermCalcImpl() {
	}

	@Override
	public TermCalc setValue(String value) {
		if (value == null) {
			throw new IllegalArgumentException(
					"Invalid value for TermCalc(null)");
		}
		this.value = value;
		return this;
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
	
    protected CodeLocation location;
	
	public CodeLocation getLocation() {
		return location;
	}

	public void setLocation(CodeLocation location) {
		this.location = location;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(operator!=null) sb.append(operator.value());
		sb.append("calc(").append(value).append(")");
		return sb.toString();
	}
}