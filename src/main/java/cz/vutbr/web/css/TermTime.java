package cz.vutbr.web.css;

/**
 * Holds CSS time value
 * @author kapy
 *
 */
public interface TermTime extends TermFloatValue {
	public void setLocation(CodeLocation location);
    
    public CodeLocation getLocation();

}
