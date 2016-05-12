package cz.vutbr.web.css;

public interface TermCalc extends Term<String>
{
	public void setLocation(CodeLocation location);
    
    public CodeLocation getLocation();
}
