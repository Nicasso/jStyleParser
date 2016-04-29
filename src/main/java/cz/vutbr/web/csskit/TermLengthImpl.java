package cz.vutbr.web.csskit;

import cz.vutbr.web.css.CSSNodeVisitor;
import cz.vutbr.web.css.CodeLocation;
import cz.vutbr.web.css.TermLength;

public class TermLengthImpl extends TermFloatValueImpl implements TermLength {

	protected TermLengthImpl() {
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

    public boolean isPercentage()
    {
        return false;
    }
    
    protected CodeLocation location;
	
	public CodeLocation getLocation() {
		return location;
	}

	public void setLocation(CodeLocation location) {
		this.location = location;
	}
	
}
