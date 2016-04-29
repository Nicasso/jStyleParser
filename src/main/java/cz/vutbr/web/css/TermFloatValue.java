/**
 * 
 */
package cz.vutbr.web.css;

/**
 * A basic informace of any term type with the float value.
 * 
 * @author burgetr
 */
public interface TermFloatValue extends TermNumeric<Float>
{
	public void setLocation(CodeLocation location);
    
    public CodeLocation getLocation();
}
