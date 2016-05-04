package cz.vutbr.web.css;


/**
 * Contains imports associated with medias. Acts as collection
 * of associated medias
 * 
 * @author kapy
 */
public interface RuleCharset extends RuleBlock<String>, PrettyOutput {
  
    public String getCharset();

    public RuleCharset setCharset(String charset);

}
