package cz.vutbr.web.csskit;

import cz.vutbr.web.css.CSSNodeVisitor;
import cz.vutbr.web.css.CodeLocation;
import cz.vutbr.web.css.TermFrequency;

public class TermFrequencyImpl extends TermFloatValueImpl implements TermFrequency {

	protected TermFrequencyImpl() {
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
	
	@Override
	public TermFrequency setValue(Float value) {
		// value is negative
		if(value==null || (new Float(0.0f)).compareTo(value) > 0)
				throw new IllegalArgumentException("Null or negative value for CSS time");
		this.value = value;
		return this;
	}
	
    protected CodeLocation location;
	
	public CodeLocation getLocation() {
		return location;
	}

	public void setLocation(CodeLocation location) {
		this.location = location;
	}
	
}
