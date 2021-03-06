package cz.vutbr.web.css;

/**
 * Holds an expression() value
 * 
 * @author Radek Burget, 2009
 * 
 */
public interface TermExpression extends Term<String>
{
	public void setLocation(CodeLocation location);
    
    public CodeLocation getLocation();
}
