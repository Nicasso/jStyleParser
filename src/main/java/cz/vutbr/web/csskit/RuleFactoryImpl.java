/**
 * 
 */
package cz.vutbr.web.csskit;

import java.util.List;

import org.w3c.dom.Element;

import cz.vutbr.web.css.CombinedSelector;
import cz.vutbr.web.css.Declaration;
import cz.vutbr.web.css.MediaExpression;
import cz.vutbr.web.css.MediaQuery;
import cz.vutbr.web.css.RuleCharset;
import cz.vutbr.web.css.RuleCounterStyle;
import cz.vutbr.web.css.RuleFactory;
import cz.vutbr.web.css.RuleFontFace;
import cz.vutbr.web.css.RuleImport;
import cz.vutbr.web.css.RuleKeyframes;
import cz.vutbr.web.css.RuleMargin;
import cz.vutbr.web.css.RuleMedia;
import cz.vutbr.web.css.RuleNameSpace;
import cz.vutbr.web.css.RulePage;
import cz.vutbr.web.css.RuleSet;
import cz.vutbr.web.css.RuleViewport;
import cz.vutbr.web.css.Selector;
import cz.vutbr.web.css.Selector.ElementAttribute;
import cz.vutbr.web.css.Selector.ElementClass;
import cz.vutbr.web.css.Selector.ElementDOM;
import cz.vutbr.web.css.Selector.ElementID;
import cz.vutbr.web.css.Selector.ElementName;
import cz.vutbr.web.css.Selector.KeyframesIdent;
import cz.vutbr.web.css.Selector.KeyframesPercentage;
import cz.vutbr.web.css.Selector.Operator;
import cz.vutbr.web.css.Selector.PseudoPage;
import cz.vutbr.web.css.StyleSheet;

/**
 * @author kapy
 *
 */
@SuppressWarnings("deprecation")
public class RuleFactoryImpl implements RuleFactory {

	private static RuleFactory instance;
	
	static {
		instance = new RuleFactoryImpl();
	}
	
	private RuleFactoryImpl() {
	}
	
	public static final RuleFactory getInstance() {
		return instance;
	}
	
	/* (non-Javadoc)
	 * @see cz.vutbr.web.css.RuleFactory#createDeclaration()
	 */
	public Declaration createDeclaration() {
		return new DeclarationImpl();
	}

	/* (non-Javadoc)
	 * @see cz.vutbr.web.css.RuleFactory#createDeclaration(cz.vutbr.web.css.Declaration)
	 */
	public Declaration createDeclaration(Declaration clone) {
		return new DeclarationImpl(clone);
	}

	/* (non-Javadoc)
	 * @see cz.vutbr.web.css.RuleFactory#createImport()
	 */
	public RuleImport createImport(String uri, List<MediaQuery> importMediaQueryList) {
		return new RuleImportImpl(uri, importMediaQueryList);
	}

	/* (non-Javadoc)
	 * @see cz.vutbr.web.css.RuleFactory#createMedia()
	 */
	public RuleMedia createMedia() {
		return new RuleMediaImpl();
	}

	public MediaQuery createMediaQuery() {
	    return new MediaQueryImpl();
	}
	
    public MediaExpression createMediaExpression() {
        return new MediaExpressionImpl();
    }
    
	/* (non-Javadoc)
	 * @see cz.vutbr.web.css.RuleFactory#createPage()
	 */
	public RulePage createPage() {
		return new RulePageImpl();
	}
	
	/* (non-Javadoc)
	 * @see cz.vutbr.web.css.RuleFactory#createMargin()
	 */
	public RuleMargin createMargin(String area) {
		return new RuleMarginImpl(area);
	}

    public RuleViewport createViewport() {
        return new RuleViewportImpl();
    }
    
	public RuleFontFace createFontFace() {
	    return new RuleFontFaceImpl();
	}
	
	/* (non-Javadoc)
	 * @see cz.vutbr.web.css.RuleFactory#createCombinedSelector()
	 */
	public CombinedSelector createCombinedSelector() {
		return new CombinedSelectorImpl();
	}

	/* (non-Javadoc)
	 * @see cz.vutbr.web.css.RuleFactory#createSet()
	 */
	public RuleSet createSet() {
		return new RuleSetImpl();
	}

	/* (non-Javadoc)
	 * @see cz.vutbr.web.css.RuleFactory#createSelector()
	 */
	public Selector createSelector() {
		return new SelectorImpl();
	}
	
	public ElementAttribute createAttribute(String value,
			boolean isStringValue, Operator operator, String attribute) {
		return new SelectorImpl.ElementAttributeImpl(value, isStringValue, operator, attribute);
	}
	
	public ElementClass createClass(String className) {
		return new SelectorImpl.ElementClassImpl(className);
	}

	public ElementName createElement(String elementName) {
		return new SelectorImpl.ElementNameImpl(elementName);
	}
	
	public ElementDOM createElementDOM(Element e, boolean inlinePriority) {
		return new SelectorImpl.ElementDOMImpl(e, inlinePriority);
	}

	
	public ElementID createID(String id) {
		return new SelectorImpl.ElementIDImpl(id);
	}
	
	public PseudoPage createPseudoPage(String pseudo, String functionName) {
		return new SelectorImpl.PseudoPageImpl(pseudo, functionName);
	}
	
	public StyleSheet createStyleSheet() {
		return new StyleSheetImpl();
	}
	
	public StyleSheet createStyleSheet(StyleSheet.Origin origin) {
		StyleSheet ret = new StyleSheetImpl();
		ret.setOrigin(origin);
		return ret;
	}

	@Override
	public RuleCharset createCharset(String charset) {
		return new RuleCharsetImpl(charset);
	}

	@Override
	public RuleNameSpace createNameSpace(String prefix, String uri) {
		return new RuleNameSpaceImpl(prefix, uri);
	}

	@Override
	public RuleCounterStyle createCounterStyle(String name) {
		return new RuleCounterStyleImpl(name);
	}

	@Override
	public KeyframesIdent createKeyFramesIdent(String ident) {
		return new SelectorImpl.KeyframesIdentImpl(ident);
	}

	@Override
	public KeyframesPercentage createKeyFramesPercentage(String percentage) {
		return new SelectorImpl.KeyframesPercentageImpl(percentage);
	}

	@Override
	public RuleKeyframes createKeyframes(String name) {
		return new RuleKeyframesImpl(name);
	}
}