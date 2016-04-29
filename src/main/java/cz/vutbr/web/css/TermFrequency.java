package cz.vutbr.web.css;

/**
 * Holds frequency term
 * @author kapy
 *
 */
public interface TermFrequency extends TermFloatValue {
	public void setLocation(CodeLocation location);
    
    public CodeLocation getLocation();
}
