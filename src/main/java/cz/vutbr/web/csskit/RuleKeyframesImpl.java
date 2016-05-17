package cz.vutbr.web.csskit;

import cz.vutbr.web.css.CSSNodeVisitor;
import cz.vutbr.web.css.RuleKeyframes;
import cz.vutbr.web.css.RuleSet;
import cz.vutbr.web.css.StyleSheet;

public class RuleKeyframesImpl extends AbstractRuleBlock<RuleSet> implements RuleKeyframes {
	  
		private String name;
		
		/**
		 * Creates an empty object to be filled by interface methods
		 */
		protected RuleKeyframesImpl(String name) {
			this.name = name;
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
		public String getName() {
			return name;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}
		
	    @Override
	    public void setStyleSheet(StyleSheet stylesheet)
	    {
	        super.setStyleSheet(stylesheet);
	        //assign the style sheet recursively to the contained rule sets
	        for (RuleSet set : list)
	            set.setStyleSheet(stylesheet);
	    }

	    @Override
	    public String toString() {
	    	return this.toString(0);
	    }
	    
	    public String toString(int depth) {
	    	
	    	StringBuilder sb = new StringBuilder();
	    	
	    	// append medias
//	    	sb = OutputUtil.appendTimes(sb, OutputUtil.DEPTH_DELIM, depth);
//	    	sb.append(OutputUtil.MEDIA_KEYWORD);    	
//	    	sb = OutputUtil.app(sb, name, OutputUtil.MEDIA_DELIM);
	    	
	    	// append rules
	    	sb = OutputUtil.appendTimes(sb, OutputUtil.DEPTH_DELIM, depth);
	    	sb.append(OutputUtil.RULE_OPENING);
	    	sb = OutputUtil.appendList(sb, list, OutputUtil.RULE_DELIM, depth + 1);
	    	sb.append(OutputUtil.RULE_CLOSING);
	    	
	    	return sb.toString();
	    }

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((name == null) ? 0 : name.hashCode());
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
			if (!(obj instanceof RuleKeyframesImpl))
				return false;
			RuleKeyframesImpl other = (RuleKeyframesImpl) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}   
	    
	    

	}
