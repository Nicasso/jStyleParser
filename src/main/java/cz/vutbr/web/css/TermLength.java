package cz.vutbr.web.css;

/**
 * Holds float value with associated units (CSS length)
 * @author burgetr
 * @author kapy
 *
 */
public interface TermLength extends TermLengthOrPercent {
	public void setLocation(CodeLocation location);
    
    public CodeLocation getLocation();
}
