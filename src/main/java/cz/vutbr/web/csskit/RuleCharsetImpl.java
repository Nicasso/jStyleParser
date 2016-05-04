package cz.vutbr.web.csskit;

import cz.vutbr.web.css.CSSNodeVisitor;
import cz.vutbr.web.css.RuleCharset;

public class RuleCharsetImpl extends AbstractRuleBlock<String> implements RuleCharset {
  
	private String charset;
	
	public RuleCharsetImpl(String charset) {
		super();
		this.charset = charset;
	}

	@Override
	public String getCharset() {
		return charset;
	}

	@Override
	public RuleCharset setCharset(String charset) {
		// sanity check
    	if(charset == null) 
        	return this; 

    	this.charset = charset;
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
    	
	public String toString(int depth) {
		
		StringBuilder sb = new StringBuilder();
    	
    	sb.append(OutputUtil.IMPORT_KEYWORD).append(OutputUtil.URL_OPENING)
    			.append(charset).append(OutputUtil.URL_CLOSING);
    	
    	// append medias
    	if(list.size()!=0) sb.append(OutputUtil.SPACE_DELIM);
    	sb = OutputUtil.appendList(sb, list, OutputUtil.MEDIA_DELIM); 
    	sb.append(OutputUtil.LINE_CLOSING);
    	
    	return sb.toString();
		
	}
	
    @Override
    public String toString() {
    	return toString(0);
    }

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((charset == null) ? 0 : charset.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof RuleCharsetImpl))
			return false;
		RuleCharsetImpl other = (RuleCharsetImpl) obj;
		if (charset == null) {
			if (other.charset != null)
				return false;
		} else if (!charset.equals(other.charset))
			return false;
		return true;
	}

}
