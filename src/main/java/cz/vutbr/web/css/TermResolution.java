/**
 * TermResolution.java
 *
 * Created on 2. 7. 2014, 14:56:10 by burgetr
 */
package cz.vutbr.web.css;

/**
 * A term representing a resolution.
 * @author burgetr
 */
public interface TermResolution extends TermFloatValue
{
	public void setLocation(CodeLocation location);
    
    public CodeLocation getLocation();
}
