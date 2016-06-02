package cz.vutbr.web.css;

import java.util.List;

/**
 * Contains imports associated with medias. Acts as collection
 * of associated medias
 * 
 * @author kapy
 */
public interface RuleImport extends RuleBlock<String>, PrettyOutput {
  
	/**
	 * Gets URI of import rule file
	 * @return URI of file to be imported
	 */
    public String getURI();
    
    public List<MediaQuery> getMediaQueries();

    /**
     * Sets URI of import rule
     * @param uri URI of file to be imported
     */
    public RuleImport setURI(String uri);
    
    //public void setLinkedStylesheet(StyleSheet linkedStylesheet);
    
    //public StyleSheet getLinkedStylesheet();

}
