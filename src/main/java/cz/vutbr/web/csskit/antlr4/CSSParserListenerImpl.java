package cz.vutbr.web.csskit.antlr4;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import org.abego.treelayout.internal.util.java.lang.string.StringUtil;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.codehaus.plexus.interpolation.util.StringUtils;
import org.fit.net.DataURLHandler;

import cz.vutbr.web.css.CSSComment;
import cz.vutbr.web.css.CSSException;
import cz.vutbr.web.css.CSSFactory;
import cz.vutbr.web.css.CodeLocation;
import cz.vutbr.web.css.CombinedSelector;
import cz.vutbr.web.css.Declaration;
import cz.vutbr.web.css.MediaExpression;
import cz.vutbr.web.css.MediaQuery;
import cz.vutbr.web.css.NetworkProcessor;
import cz.vutbr.web.css.RuleBlock;
import cz.vutbr.web.css.RuleFactory;
import cz.vutbr.web.css.RuleImport;
import cz.vutbr.web.css.RuleList;
import cz.vutbr.web.css.RuleMargin;
import cz.vutbr.web.css.RuleSet;
import cz.vutbr.web.css.Selector;
import cz.vutbr.web.css.StyleSheet;
import cz.vutbr.web.css.Term;
import cz.vutbr.web.css.TermCalc;
import cz.vutbr.web.css.TermColor;
import cz.vutbr.web.css.TermFactory;
import cz.vutbr.web.css.TermFunction;
import cz.vutbr.web.css.TermIdent;
import cz.vutbr.web.css.TermString;
import cz.vutbr.web.csskit.CSSError;
import cz.vutbr.web.csskit.CommentImpl;
import cz.vutbr.web.csskit.DefaultNetworkProcessor;
import cz.vutbr.web.csskit.RuleArrayList;
import cz.vutbr.web.csskit.TermColorImpl;
import cz.vutbr.web.csskit.antlr4.CSSParser.CalcproductContext;
import cz.vutbr.web.csskit.antlr4.CSSParser.CalcsumContext;
import cz.vutbr.web.csskit.antlr4.CSSParser.CalcvalueContext;
import cz.vutbr.web.csskit.antlr4.CSSParser.Charset_nameContext;
import cz.vutbr.web.csskit.antlr4.CSSParser.CommentContext;
import cz.vutbr.web.csskit.antlr4.CSSParser.Keyframe_selectorContext;
import cz.vutbr.web.csskit.antlr4.CSSParser.Keyframe_selectorsContext;
import cz.vutbr.web.csskit.antlr4.CSSParser.Keyframes_blockContext;
import cz.vutbr.web.csskit.antlr4.CSSParser.OperatorContext;
import cz.vutbr.web.csskit.antlr4.CSSParserFactory.SourceType;

public class CSSParserListenerImpl implements CSSParserListener {

	// factories for building structures
	private RuleFactory rf = CSSFactory.getRuleFactory();
	private TermFactory tf = CSSFactory.getTermFactory();

	private enum MediaQueryState {
		START, TYPE, AND, EXPR, TYPEOREXPR
	}

	// structures after parsing
	private List<String> importPaths = new ArrayList<>();
	private List<List<MediaQuery>> importMedia = new ArrayList<>();
	private RuleList rules = null;

	// block preparator
	private Preparator preparator;
	private List<MediaQuery> wrapMedia;

	// prevent imports inside the style sheet
	private boolean preventImports = false;

	// logger
	private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());

	// temp variables for construction
	private Term.Operator tmpOperator;
	private CombinedSelector tmpCombinedSelector;
	private boolean tmpCombinedSelectorInvalid;
	private Selector tmpSelector;
	private List<CombinedSelector> tmpCombinedSelectorList;
	private List<Declaration> tmpDeclarations;
	private declaration_scope tmpDeclarationScope;
	private atstatement_scope tmpAtStatementOrRuleSetScope;
	private RuleList tmpRuleList;
	private List<RuleMargin> tmpMargins;
	private RuleMargin tmpMarginRule;

	private CodeLocation tmpStyleSheetLocation;

	private boolean preventStyleSheetComment;
	private CSSComment tmpStyleSheetComment;
	private CSSComment tmpDeclarationComment;
	private CSSComment tmpStatementComment;
	private CSSComment tmpMediaRuleComment;
	private CSSComment tmpKeyframeComment;

	private List<CSSError> errorList;

	private Stack<terms_scope> terms_stack = new Stack<>();
	private List<cz.vutbr.web.css.Term<?>> tmpTermList;

	private Boolean stmtIsValid = false;
	private Selector.Combinator tmpCombinator = null;
	private mediaquery_scope tmpMediaQueryScope;
	private List<MediaQuery> mediaQueryList = null;
	private MediaExpression tmpMediaExpression = null;

	private static class terms_scope {
		List<Term<?>> list;
		Term<?> term;
		Term.Operator op;
		int unary;
		boolean dash;
	}

	private static class mediaquery_scope {
		cz.vutbr.web.css.MediaQuery q;
		MediaQueryState state;
		boolean invalid;
	}

	private String extractTextUnescaped(String text) {
		return org.unbescape.css.CssEscape.unescapeCss(text);
	}

	/**
	 * check if string is valid ID
	 *
	 * @param id
	 *            ID to validate and unescapes
	 * @return unescaped id or null
	 */
	private String extractIdUnescaped(String id) {
		if (!id.isEmpty() && !Character.isDigit(id.charAt(0))) {
			return org.unbescape.css.CssEscape.unescapeCss(id);
		}
		return null;
	}

	private Declaration.Source extractSource(CSSToken ct) {
		return new Declaration.Source(ct.getBase(), ct.getLine(), ct.getCharPositionInLine());
	}

	private URL extractBase(TerminalNode node) {
		CSSToken ct = (CSSToken) node.getSymbol();
		return ct.getBase();
	}

	private static class declaration_scope {
		cz.vutbr.web.css.Declaration d;
		boolean invalid;
	}

	private declaration_scope getDeclarationScopeAndInit() {
		declaration_scope tmp = new declaration_scope();
		tmp.d = rf.createDeclaration();
		tmp.invalid = false;
		tmp.d.unlock();
		return tmp;
	}

	private static class atstatement_scope {
		cz.vutbr.web.css.RuleBlock<?> stm;
	}

	/**
	 * remove terminal node emtpy tokens from input list
	 *
	 * @param inputArrayList
	 *            original list
	 * @return list without terminal node type = S (space)
	 */
	private List<ParseTree> filterSpaceTokens(List<ParseTree> inputArrayList) {

		return inputArrayList.stream().filter(item -> (!(item instanceof TerminalNode)
				|| ((TerminalNodeImpl) item).getSymbol().getType() != CSSLexer.S)).collect(Collectors.toList());
	}

	/**
	 * check if rule context contains error node
	 *
	 * @param ctx
	 *            rule context
	 * @return contains context error node
	 */
	private boolean ctxHasErrorNode(ParserRuleContext ctx) {
		for (int i = 0; i < ctx.children.size(); i++) {
			if (ctx.getChild(i) instanceof ErrorNode) {
				return true;
			}
		}
		return false;
	}

	// list of context childern without spaces modified on enterEveryRule
	// generated from ctx.childern
	private List childernWithoutSpaces;

	/**
	 * Constructor
	 *
	 * @param preparator
	 *            The preparator to be used for creating the rules.
	 * @param wrapMedia
	 *            The media queries to be used for wrapping the created rules
	 *            (e.g. in case of parsing and imported style sheet) or null
	 *            when no wrapping is required.
	 */
	public CSSParserListenerImpl(Preparator preparator, List<MediaQuery> wrapMedia) {
		this.preparator = preparator;
		this.wrapMedia = wrapMedia;
		this.errorList = new ArrayList<CSSError>();
	}

	private void addCSSError(ParserRuleContext ctx, String message) {
		errorList.add(new CSSError(getCodeLocation(ctx, 0), message));
	}

	// used in parseMediaQuery
	public CSSParserListenerImpl() {
	}

	// counter of spaces for pretty debug printing
	private int spacesCounter = 0;

	/**
	 * generate spaces for pretty debug printing
	 *
	 * @param count
	 *            number of generated spaces
	 * @return string with spaces
	 */
	private String generateSpaces(int count) {
		String spaces = "";
		for (int i = 0; i < count; i++) {
			spaces += " ";
		}
		return spaces;
	}

	/**
	 * get parsed rulelist
	 *
	 * @return parsed rules
	 */
	public RuleList getRules() {
		return rules;
	}

	public List<CSSError> getErrorList() {
		return errorList;
	}

	public CSSComment getStyleSheetComment() {
		return tmpStyleSheetComment;
	}

	public CodeLocation getStyleSheetLocation() {
		return tmpStyleSheetLocation;
	}

	/**
	 * get mediaquery list
	 *
	 * @return media query list
	 */
	public List<MediaQuery> getMedia() {
		return mediaQueryList;
	}

	public List<String> getImportPaths() {
		return importPaths;
	}

	public List<List<MediaQuery>> getImportMedia() {
		return importMedia;
	}

	private void logEnter(String entry) {
		System.out.println("Enter: " + entry);
		log.trace("Enter: " + generateSpaces(spacesCounter) + "{}", entry);
	}

	private void logLeave(String leaving) {
		System.out.println("Exit: " + leaving);
		log.trace("Leave: " + generateSpaces(spacesCounter) + "{}", leaving);
	}

	// override generated methods

	@Override
	public void enterInlinestyle(CSSParser.InlinestyleContext ctx) {
		logEnter("inlinestyle: " + ctx.getText());
		rules = new RuleArrayList();
	}

	@Override
	public void exitInlinestyle(CSSParser.InlinestyleContext ctx) {
		if (ctx.declarations() != null) {
			RuleBlock<?> rb = preparator.prepareInlineRuleSet(tmpDeclarations, null);
			if (rb != null) {
				rules.add(rb);
			}
		}
		log.debug("\n***\n{}\n***\n", rules);
		logLeave("inlinestyle: " + ctx.getText());
		log.trace("EXITING INLINESTYLE ----------------------------------");
		tmpDeclarations = null;
	}

	@Override
	public void enterStylesheet(CSSParser.StylesheetContext ctx) {
		logEnter("stylesheet: " + ctx.getText());
		preventStyleSheetComment = false;
		rules = new RuleArrayList();
		tmpStyleSheetLocation = getCodeLocation(ctx, 0);
	}

	@Override
	public void exitStylesheet(CSSParser.StylesheetContext ctx) {
		log.debug("\n***\n{}\n***\n", rules);
		logLeave("stylesheet");
		log.trace("EXITING STYLESHEET ----------------------------------");
	}

	@Override
	public void enterStatement(CSSParser.StatementContext ctx) {
		logEnter("statement: " + ctx.getText());
		preventStyleSheetComment = true;
		stmtIsValid = true;
		tmpRuleList = new RuleArrayList();
	}

	@Override
	public void exitStatement(CSSParser.StatementContext ctx) {
		// statement: ruleset | atstatement
		
		if (ctx.ruleset() != null) {
			if (stmtIsValid) {
				//System.out.println(tmpRuleList.size());
				for (RuleBlock<?> rule : tmpRuleList) {
					if (rule != null) {
						if (tmpStatementComment != null) {
							rule.setComment(tmpStatementComment);
							rule.setLocation(getCodeLocation(ctx, 0));
							tmpStatementComment = null;
						}
						log.debug("exitStatement |ADDING statement {}", rule);
						rules.add(rule);
					} else {
						log.debug("exitStatement |ommited null statement ");
					}
				}
			} else {
				log.debug("exitStatement | statement is not valid, so not adding it");
			}
		} else {
			if (tmpAtStatementOrRuleSetScope.stm != null) {
				log.debug("exitStatement | ADDING statement {}", tmpAtStatementOrRuleSetScope.stm);

				if (tmpStatementComment != null) {
					tmpAtStatementOrRuleSetScope.stm.setComment(tmpStatementComment);
					tmpAtStatementOrRuleSetScope.stm.setLocation(getCodeLocation(ctx, 0));
					tmpStatementComment = null;
				}

				rules.add(tmpAtStatementOrRuleSetScope.stm);
			}
		}

	}

	@Override
	public void enterRuleset(CSSParser.RulesetContext ctx) {
		logEnter("ruleset: " + ctx.getText());
		stmtIsValid = true;
		// init scope
		tmpAtStatementOrRuleSetScope = new atstatement_scope();
		tmpCombinedSelectorList = new ArrayList<>();
	}

	@Override
	public void exitRuleset(CSSParser.RulesetContext ctx) {
		// logLeave("ruleset" +ctx.getText());
		// complete ruleset and add to ruleList
		
		if (ctxHasErrorNode(ctx)) {
			log.debug("invalidating ruleset");
			addCSSError(ctx, "Ruleset syntax error: " + ctx.getText());
			return;
		}
		
		tmpAtStatementOrRuleSetScope.stm = preparator.prepareRuleSet(tmpCombinedSelectorList, tmpDeclarations,
				(this.wrapMedia != null && !this.wrapMedia.isEmpty()), this.wrapMedia);
		
		if (tmpAtStatementOrRuleSetScope.stm != null && !ctxHasErrorNode(ctx)) {
			tmpAtStatementOrRuleSetScope.stm.setLocation(getCodeLocation(ctx, 0));
			tmpRuleList.add(tmpAtStatementOrRuleSetScope.stm);	
		}
		
		// cleanup tmpDeclarations
		tmpDeclarations = null;
	}

	@Override
	public void enterDeclarations(CSSParser.DeclarationsContext ctx) {
		logEnter("declarations: " + ctx.getText());
		tmpDeclarations = new ArrayList<>();
		// if is ruleset
		// exit combinator if needed
	}

	@Override
	public void exitDeclarations(CSSParser.DeclarationsContext ctx) {
		logLeave("declarations: " + ctx.getText());
	}

	@Override
	public void enterDeclaration(CSSParser.DeclarationContext ctx) {
		logEnter("declaration: " + ctx.getText());
		tmpDeclarationScope = getDeclarationScopeAndInit();

		if (ctxHasErrorNode(ctx) || ctx.noprop() != null) {
			log.debug("invalidating declaration");
			addCSSError(ctx, "Declaration syntax error: " + ctx.getText());
			tmpDeclarationScope.invalid = true;
			//System.out.println("INVALID DECLARATION");
		}
	}

	@Override
	public void exitDeclaration(CSSParser.DeclarationContext ctx) {

		//System.out.println("DEZE");
		
		if (ctx.terms() != null) {
			//System.out.println("YAY: "+tmpTermList.size());
			// log.debug("Totally added {} terms",
			// terms_stack.peek().list.size());
			tmpDeclarationScope.d.replaceAll(tmpTermList);

		}
		if (!tmpDeclarationScope.invalid) {
			//System.out.println("YAY2: ");
			log.debug("Returning declaration: {}.", tmpDeclarationScope.d);

			if (tmpDeclarationComment != null) {
				tmpDeclarationScope.d.setComment(tmpDeclarationComment);
				tmpDeclarationComment = null;
			}

			tmpDeclarationScope.d.setLocation(getCodeLocation(ctx, 0));
			tmpDeclarations.add(tmpDeclarationScope.d);
			log.debug("Inserted declaration #{} ", tmpDeclarations.size() + 1);
		} else {
			//System.out.println("DECLARATION IS INVALID");
			log.debug("Declaration was invalidated or already invalid");
		}

	}

	@Override
	public void enterImportant(CSSParser.ImportantContext ctx) {
		if (ctxHasErrorNode(ctx)) {
			tmpDeclarationScope.invalid = true;
			addCSSError(ctx, "Important syntax error: " + ctx.getText());
			//System.out.println("INVALID IMPORTANT");
		} else {
			tmpDeclarationScope.d.setImportant(true);
			log.debug("Setting property to IMPORTANT");
		}
	}

	@Override
	public void exitImportant(CSSParser.ImportantContext ctx) {

	}

	@Override
	/**
	 * Property of declaration
	 */
	public void enterProperty(CSSParser.PropertyContext ctx) {
		logEnter("property: " + ctx.getText());
		String property = extractTextUnescaped(ctx.getText());
//		if (ctx.MINUS() != null) {
//			String newProp = "";
//			for(int i = 0; i < ctx.MINUS().size(); i++) {
//				newProp += "-";
//			}
//			newProp += property;
//			property = newProp;
//		}
		tmpDeclarationScope.d.setProperty(property);
		//Token token = ctx.IDENT().getSymbol();
		//tmpDeclarationScope.d.setSource(extractSource((CSSToken) token));
		log.debug("Setting property: {}", tmpDeclarationScope.d.getProperty());
	}

	@Override
	public void exitProperty(CSSParser.PropertyContext ctx) {
		// empty
	}

	@Override
	public void enterTerms(CSSParser.TermsContext ctx) {
		logEnter("terms: " + ctx.getText());
		tmpTermList = new ArrayList<>();
		terms_stack.push(new terms_scope());
		terms_stack.peek().list = new ArrayList<>();
		terms_stack.peek().term = null;
		terms_stack.peek().op = Term.Operator.SPACE;
		terms_stack.peek().unary = 1;
		terms_stack.peek().dash = false;

		if (ctxHasErrorNode(ctx)) {
			log.debug("invalidating terms");
			addCSSError(ctx, "Terms syntax error: " + ctx.getText());
			tmpDeclarationScope.invalid = true;
			//System.out.println("INVALID TERMS");
		}
	}

	@Override
	public void exitTerms(CSSParser.TermsContext ctx) {
		tmpTermList = terms_stack.peek().list;
		log.debug("Totally added {} terms", tmpTermList.size());
		terms_stack.pop();
		logLeave("terms");
		tmpOperator = null;
	}

	@Override
	public void enterTermValuePart(CSSParser.TermValuePartContext ctx) {
		logEnter("termValuePart: " + ctx.getText());
	}

	@Override
	public void exitTermValuePart(CSSParser.TermValuePartContext ctx) {
		logLeave("termValuePart: " + ctx.getText());
	}

	@Override
	public void enterTermCurlyBlock(CSSParser.TermCurlyBlockContext ctx) {
		tmpDeclarationScope.invalid = true;
		//System.out.println("enterTermCurlyBlock INVALID");
	}

	@Override
	public void exitTermCurlyBlock(CSSParser.TermCurlyBlockContext ctx) {

	}

	@Override
	public void enterTermAtKeyword(CSSParser.TermAtKeywordContext ctx) {
		tmpDeclarationScope.invalid = true;
		//System.out.println("enterTermAtKeyword INVALID");
	}

	@Override
	public void exitTermAtKeyword(CSSParser.TermAtKeywordContext ctx) {

	}

	@Override
	public void enterFunct(CSSParser.FunctContext ctx) {
		logEnter("funct: " + ctx.getText());
	}

	@Override
	public void exitFunct(CSSParser.FunctContext ctx) {
		if (ctxHasErrorNode(ctx)) {
			log.debug("invalidating terms");
			addCSSError(ctx, "Function syntax error: " + ctx.getText());
			tmpDeclarationScope.invalid = true;
			//System.out.println("exitFunct INVALID MAIN");
		} else {
			if (ctx.EXPRESSION() != null) {
				// EXPRESSION
				//System.out.println("EXPRESSION: ");
				//System.out.println(ctx.getText());
	//			//System.out.println(ctx.actualExpression.toString());
	//			//System.out.println(ctx.actualExpression.getText());
	//			//System.out.println(ctx.actualExpression);
				//throw new UnsupportedOperationException("EXPRESSIONS are not allowed yet");
				// todo
				TermFunction function = tf.createFunction();
				// log.debug("function name to " + fname);
				function.setFunctionName("expression");
				if (terms_stack.peek().unary == -1) // if started with minus,
													// add the minus to the
													// function name
					function.setFunctionName('-' + function.getFunctionName());
	
				TermString val = tf.createString(ctx.getText());
				val.setLocation(getCodeLocation(ctx, 0));
	
	//			terms_stack.peek().list.add(terms_stack.peek().term);
	//			tmpTermList = terms_stack.peek().list;
	//			log.debug("Totally added {} terms", tmpTermList.size());
	//			terms_stack.pop();
	//			logLeave("terms");
	//			tmpOperator = null;
	//	
				List<cz.vutbr.web.css.Term<?>> values = new ArrayList<>();
				values.add(val);
				function.setValue(values);
				
				terms_stack.peek().term = function;
				terms_stack.peek().term.setLocation(getCodeLocation(ctx, 0));
				
				log.debug("Setting function: {}", function.toString());
			} else if (ctx.CALC() != null || ctx.calcsum() != null) {
				//System.out.println("CALC HERE: " + ctx.getText());
	
				TermCalc calc = tf.createCalc(ctx.calcsum().getText());
				terms_stack.peek().term = calc;
				terms_stack.peek().term.setLocation(getCodeLocation(ctx, 0));
			} else {
				String fname = extractTextUnescaped(ctx.FUNCTION().getText());
	
				if (fname.equalsIgnoreCase("url")) {
					if (terms_stack.peek().unary == -1 || tmpTermList == null || tmpTermList.size() != 1) {
						tmpDeclarationScope.invalid = true;
						//System.out.println("exitFunct INVALID");
					} else {
						Term<?> term = tmpTermList.get(0);
						if (term instanceof TermString) {
							log.debug("creating url");
							terms_stack.peek().term = tf.createURI(extractTextUnescaped(((TermString) term).getValue()),
									extractBase(ctx.FUNCTION()));
							terms_stack.peek().term.setLocation(getCodeLocation(ctx, 0));
						} else {
							tmpDeclarationScope.invalid = true;
							//System.out.println("exitFunct INVALID2");
						}
					}
				} else if (fname.equalsIgnoreCase("rgb(")) {
					TermFunction function = tf.createFunction();
					function.setFunctionName("rgb");
					function.setValue(tmpTermList);
					TermColor color = TermColorImpl.getColorByFunction(function);
					// color.setOriginalFormat(ctx.getText());
					terms_stack.peek().term = color;
					terms_stack.peek().term.setLocation(getCodeLocation(ctx, 0));
				} else if (fname.equalsIgnoreCase("rgba(")) {
					TermFunction function = tf.createFunction();
					function.setFunctionName("rgba");
					function.setValue(tmpTermList);
					TermColor color = TermColorImpl.getColorByFunction(function);
					// color.setOriginalFormat(ctx.getText());
					terms_stack.peek().term = color;
					terms_stack.peek().term.setLocation(getCodeLocation(ctx, 0));
				} else {
					TermFunction function = tf.createFunction();
					// log.debug("function name to " + fname);
					function.setFunctionName(fname);
					if (terms_stack.peek().unary == -1) // if started with minus,
														// add the minus to the
														// function name
						function.setFunctionName('-' + function.getFunctionName());
					if (tmpTermList != null) {
						// log.debug("setting function value to : {}", tmpTermList);
						function.setValue(tmpTermList);
					}
					terms_stack.peek().term = function;
					terms_stack.peek().term.setLocation(getCodeLocation(ctx, 0));
					log.debug("Setting function: {}", function.toString());
	
				}
				// function
			}
		
		}
		logLeave("funct");
	}

	@Override
	public void enterValuepart(CSSParser.ValuepartContext ctx) {
		logEnter("valueparts: >" + ctx.getText() + "<");
		try {
			// So that it doesn't break with this ugly CSS hack: padding: 5px 5px
			// 5px 5px\0;
			String text = ctx.getText().replaceAll("(\\\\[0-9])+$", "").trim();
	
			if (ctx.MINUS() != null) {
				terms_stack.peek().unary = -1;
				terms_stack.peek().dash = true;
			}
			terms_stack.peek().op = tmpOperator;
			if (ctx.COMMA() != null) {
				log.debug("VP - comma");
				//System.out.println("COMMAAAAAAAAAAAAAAAAAAAAA");
				terms_stack.peek().op = Term.Operator.COMMA;
			} else if (ctx.SLASH() != null) {
				//System.out.println("SLASSSSSSSSSSSSH");
				terms_stack.peek().op = Term.Operator.SLASH;
			} else if (ctx.string() != null) {
				// string
				log.debug("VP - string");
				terms_stack.peek().term = tf.createString(extractTextUnescaped(text));
				terms_stack.peek().term.setLocation(getCodeLocation(ctx, 0));
			} else if (ctx.IDENT() != null) {
				log.debug("VP - ident");
				terms_stack.peek().term = tf.createIdent(extractTextUnescaped(text), terms_stack.peek().dash);
				terms_stack.peek().term.setLocation(getCodeLocation(ctx, 0));
			} else if (ctx.HASH() != null) {
				log.debug("VP - hash");
				TermColor color = tf.createColor(text);
				// color.setOriginalFormat(ctx.getText());
				terms_stack.peek().term = color;
				terms_stack.peek().term.setLocation(getCodeLocation(ctx, 0));
				if (terms_stack.peek().term == null) {
					tmpDeclarationScope.invalid = true;
					//System.out.println("enterValuepart INVALID");
				}
			} else if (ctx.PERCENTAGE() != null) {
				log.debug("VP - percentage");
				terms_stack.peek().term = tf.createPercent(text, terms_stack.peek().unary);
				terms_stack.peek().term.setLocation(getCodeLocation(ctx, 0));
			} else if (ctx.DIMENSION() != null) {
				log.debug("VP - dimension");
				String dim = text.trim();
				//System.out.println(text);
				//System.out.println(ctx.getText());
				if (!text.trim().equals(ctx.getText().trim()) && isNumeric(dim)) {
					terms_stack.peek().term = tf.createDimension(dim + "px", terms_stack.peek().unary);
					terms_stack.peek().term.setLocation(getCodeLocation(ctx, 0));
					if (terms_stack.peek().term == null) {
						log.info("Unable to create dimension from {}, unary {}", dim, terms_stack.peek().unary);
						tmpDeclarationScope.invalid = true;
						//System.out.println("INVALID DIMENSION");
					}
				} else {
					terms_stack.peek().term = tf.createDimension(dim, terms_stack.peek().unary);
					// TODO: IE HACK like padding: 5px 5px 5px 5px\0; BREAKS HERE!
					//System.out.println(terms_stack.peek().term);
					//System.out.println("-------------");
					//System.out.println(ctx.getText());
					//System.out.println(dim);
					terms_stack.peek().term.setLocation(getCodeLocation(ctx, 0));
					if (terms_stack.peek().term == null) {
						log.info("Unable to create dimension from {}, unary {}", dim, terms_stack.peek().unary);
						tmpDeclarationScope.invalid = true;
						//System.out.println("INVALID DIMENSION AGAIN");
					}
				}
			} else if (ctx.NUMBER() != null) {
				log.debug("VP - number");
				//System.out.println("NUMBER");
				terms_stack.peek().term = tf.createNumeric(text, terms_stack.peek().unary);
				terms_stack.peek().term.setLocation(getCodeLocation(ctx, 0));
			} else if (ctx.URI() != null) {
				log.debug("VP - uri");
				terms_stack.peek().term = tf.createURI(extractTextUnescaped(text), extractBase(ctx.URI()));
				terms_stack.peek().term.setLocation(getCodeLocation(ctx, 0));
			} else if (ctx.funct() != null) {
				terms_stack.peek().term = null;
				// served in function
				log.debug("function is server later");
			} else {
				log.error("unhandled valueparts");
				terms_stack.peek().term = null;
				tmpDeclarationScope.invalid = true;
				addCSSError(ctx, "Value part syntax error: " + ctx.getText());
				//System.out.println("INVALID VALUEPARTS");
			}
		} catch(Exception e) {
			terms_stack.peek().term = null;
			tmpDeclarationScope.invalid = true;
			addCSSError(ctx, "Value part syntax error: " + ctx.getText());
		}
	}

	public boolean isNumeric(final CharSequence cs) {
		final int sz = cs.length();
		for (int i = 0; i < sz; i++) {
			if (Character.isDigit(cs.charAt(i)) == false) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void exitValuepart(CSSParser.ValuepartContext ctx) {
		// try convert color from current term
		// //System.out.println("VP: "+ctx.getText());
		// //System.out.println(getCodeLocation(ctx,0));
		// //System.out.println("");
		if (terms_stack.peek().term != null) {
			TermColor termColor = null;
			if (terms_stack.peek().term instanceof TermIdent) { // red
				termColor = tf.createColor((TermIdent) terms_stack.peek().term);
				// termColor.setOriginalFormat(ctx.getText());
			} else if (terms_stack.peek().term instanceof TermFunction) { // rgba(0,0,0)
				termColor = tf.createColor((TermFunction) terms_stack.peek().term);
				// termColor.setOriginalFormat(ctx.getText());
			}
			if (termColor != null) {
				log.debug("term color is OK - creating - " + termColor.toString());
				terms_stack.peek().term = termColor;
				terms_stack.peek().term.setLocation(getCodeLocation(ctx, 0));
			}

		}

		// save valuepartr to termslist
		if (!tmpDeclarationScope.invalid && terms_stack.peek().term != null) {
			log.debug("adding valuepart " + terms_stack.peek().term);
			// set operator and add term to term list
			terms_stack.peek().term.setOperator(terms_stack.peek().op);
			terms_stack.peek().list.add(terms_stack.peek().term);
			// reinitialization
			terms_stack.peek().op = Term.Operator.SPACE;
			tmpOperator = Term.Operator.SPACE;
			terms_stack.peek().unary = 1;
			terms_stack.peek().dash = false;
			terms_stack.peek().term = null;
		} else {
			log.debug("tmpTermScope.term is null");
		}
		logLeave("valuePart");
	}

	@Override
	public void enterCombined_selector(CSSParser.Combined_selectorContext ctx) {
		String combinedSelector = ctx.getText();
		logEnter("combinedselector : " + combinedSelector);
		tmpCombinedSelectorInvalid = false;
		tmpCombinedSelector = (CombinedSelector) rf.createCombinedSelector().unlock();
		tmpCombinedSelector.setLocation(getCodeLocation(ctx, 0));
	}

	@Override
	public void exitCombined_selector(CSSParser.Combined_selectorContext ctx) {
		if (!tmpCombinedSelectorInvalid) {
			tmpCombinedSelector.setLocation(getCodeLocation(ctx, 0));
			tmpCombinedSelectorList.add(tmpCombinedSelector);
			log.debug("Returing combined selector: {}.", tmpCombinedSelector);
		} else {
			log.debug("Combined selector is invalid");
		}
		tmpCombinator = null;
	}

	@Override
	public void enterCombinatorChild(CSSParser.CombinatorChildContext ctx) {
		tmpCombinator = Selector.Combinator.CHILD;
	}

	@Override
	public void exitCombinatorChild(CSSParser.CombinatorChildContext ctx) {

	}

	@Override
	public void enterCombinatorAdjacent(CSSParser.CombinatorAdjacentContext ctx) {
		tmpCombinator = Selector.Combinator.ADJACENT;
	}

	@Override
	public void exitCombinatorAdjacent(CSSParser.CombinatorAdjacentContext ctx) {

	}

	@Override
	public void enterCombinatorPreceding(CSSParser.CombinatorPrecedingContext ctx) {
		tmpCombinator = Selector.Combinator.PRECEDING;
	}

	@Override
	public void exitCombinatorPreceding(CSSParser.CombinatorPrecedingContext ctx) {

	}

	@Override
	public void enterCombinatorDescendant(CSSParser.CombinatorDescendantContext ctx) {
		tmpCombinator = Selector.Combinator.DESCENDANT;
	}

	@Override
	public void exitCombinatorDescendant(CSSParser.CombinatorDescendantContext ctx) {

	}

	@Override
	public void enterSelectorWithIdOrAsterisk(CSSParser.SelectorWithIdOrAsteriskContext ctx) {
		enterSelector();
		Selector.ElementName en = rf.createElement(extractTextUnescaped(ctx.getChild(0).getText()));
		en.setLocation(getCodeLocation(ctx, 0));
		log.debug("Adding selector: {}", en.getName());
		tmpSelector.add(en);
	}

	@Override
	public void exitSelectorWithIdOrAsterisk(CSSParser.SelectorWithIdOrAsteriskContext ctx) {
		tmpSelector.setLocation(getCodeLocation(ctx, 0));
		exitSelector();
	}

	@Override
	public void enterSelectorWithoutIdOrAsterisk(CSSParser.SelectorWithoutIdOrAsteriskContext ctx) {
		enterSelector();
	}

	@Override
	public void exitSelectorWithoutIdOrAsterisk(CSSParser.SelectorWithoutIdOrAsteriskContext ctx) {
		tmpSelector.setLocation(getCodeLocation(ctx, 0));
		exitSelector();
	}

	// on every enterSelector submethod
	private void enterSelector() {
		logEnter("selector");
		tmpSelector = (Selector) rf.createSelector().unlock();
		if (tmpCombinator != null) {
			tmpSelector.setCombinator(tmpCombinator);
		}
	}

	// on every exitSelecotr submethod
	private void exitSelector() {
		tmpCombinedSelector.add(tmpSelector);
	}

	//////////////
	// SELPART
	/////////////
	@Override
	public void enterSelpartId(CSSParser.SelpartIdContext ctx) {
		logEnter("selpart id: " + ctx.getText());
		;
		String id = extractIdUnescaped(ctx.getText());
		if (id != null) {
			Selector.ElementID elem = rf.createID(extractTextUnescaped(ctx.getText()));
			elem.setLocation(getCodeLocation(ctx, 0));
			tmpSelector.add(elem);
		} else {
			tmpCombinedSelectorInvalid = true;
		}
	}

	@Override
	public void exitSelpartId(CSSParser.SelpartIdContext ctx) {
		// do nothing
	}

	@Override
	public void enterSelpartClass(CSSParser.SelpartClassContext ctx) {
		logEnter("selpart class: " + ctx.getText());
		Selector.ElementClass elem = rf.createClass(extractTextUnescaped(ctx.getText()));
		elem.setLocation(getCodeLocation(ctx, 0));
		tmpSelector.add(elem);
	}

	@Override
	public void exitSelpartClass(CSSParser.SelpartClassContext ctx) {
		// do nothing
	}

	@Override
	public void enterSelpartAttrib(CSSParser.SelpartAttribContext ctx) {
		logEnter("selpart attrib: " + ctx.getText());
		// do nothing
	}

	@Override
	public void exitSelpartAttrib(CSSParser.SelpartAttribContext ctx) {
		// do nothing
	}

	@Override
	public void enterSelpartPseudo(CSSParser.SelpartPseudoContext ctx) {
		logEnter("selpart pseudo: " + ctx.getText());
	}

	@Override
	public void exitSelpartPseudo(CSSParser.SelpartPseudoContext ctx) {
		// do nothing
	}

	@Override
	public void enterSelpartInvalid(CSSParser.SelpartInvalidContext ctx) {
		logEnter("Selpart invalid" + ctx.getText());
	}

	@Override
	public void exitSelpartInvalid(CSSParser.SelpartInvalidContext ctx) {
		// do nothing
	}

	@Override
	public void enterAttribute(CSSParser.AttributeContext ctx) {
		// attributes can be like [attr] or [attr operator value]
		// see http://www.w3.org/TR/CSS2/selector.html#attribute-selectors
		logEnter("attribute: " + ctx.getText());
		// initialize attribute
		String attributeName = extractTextUnescaped(ctx.children.get(0).getText());
		String value = null;
		boolean isStringValue = false;
		Selector.Operator op = Selector.Operator.NO_OPERATOR;
		// is attribute like [attr=value]
		if (childernWithoutSpaces.size() == 3) {
			CommonToken opToken = (CommonToken) ((TerminalNodeImpl) childernWithoutSpaces.get(1)).symbol;
			isStringValue = (childernWithoutSpaces.get(2) instanceof CSSParser.StringContext);
			if (isStringValue) {
				value = ((ParserRuleContext) childernWithoutSpaces.get(2)).getText();
			} else {

				value = ((TerminalNode) childernWithoutSpaces.get(2)).getText();
			}
			value = extractTextUnescaped(value);
			switch (opToken.getType()) {
			case CSSParser.EQUALS: {
				op = Selector.Operator.EQUALS;
				break;
			}
			case CSSParser.INCLUDES: {
				op = Selector.Operator.INCLUDES;
				break;
			}
			case CSSParser.DASHMATCH: {
				op = Selector.Operator.DASHMATCH;
				break;
			}
			case CSSParser.CONTAINS: {
				op = Selector.Operator.CONTAINS;
				break;
			}
			case CSSParser.STARTSWITH: {
				op = Selector.Operator.STARTSWITH;
				break;
			}
			case CSSParser.ENDSWITH: {
				op = Selector.Operator.ENDSWITH;
				break;
			}
			default: {
				op = Selector.Operator.NO_OPERATOR;
			}
			}
		}
		Selector.ElementAttribute elemAttr = rf.createAttribute(value, isStringValue, op, attributeName);
		elemAttr.setLocation(getCodeLocation(ctx, 0));
		tmpSelector.add(elemAttr);
	}

	@Override
	public void exitAttribute(CSSParser.AttributeContext ctx) {

	}

	@Override
	// pseudocolon (IDENT | FUNCTION S* (IDENT | MINUS? NUMBER | MINUS? INDEX)
	// S* RPAREN)
	public void enterPseudo(CSSParser.PseudoContext ctx) {
		if (ctxHasErrorNode(ctx)) {
			stmtIsValid = false;
			addCSSError(ctx, "Pseudo syntax error: " + ctx.getText());
		}
		logEnter("pseudo: " + ctx.getText());
		// childcount == 2
		// first item is pseudocolon | : or ::
		Boolean isPseudoElem = ctx.getChild(0).getText().length() != 1;
		Selector.PseudoPage tmpPseudo;
		String first = extractTextUnescaped(ctx.getChild(1).getText());
		if (ctx.FUNCTION() == null) {
			// ident
			tmpPseudo = rf.createPseudoPage(first, null);
			if (tmpPseudo == null || tmpPseudo.getDeclaration() == null) {
				log.error("invalid pseudo declaration: " + first);
				stmtIsValid = false;
				tmpPseudo = null;
			} else if (isPseudoElem && !tmpPseudo.getDeclaration().isPseudoElement()) {
				log.error("pseudo class cannot be used as pseudo element");
				tmpPseudo = null; // * pseudoClasses are not allowed here *//*
			}
		} else {
			// function
			if (isPseudoElem) {
				log.error("pseudo element cannot be used as a function");
				tmpPseudo = null;
			} else {
				// function
				// var first is function name
				String value = "";
				if (ctx.IDENT() != null) {
					value = ctx.IDENT().getText();
				} else {
					if (ctx.MINUS() != null) {
						value = "-";
					}
					if (ctx.NUMBER() != null) {
						value += ctx.NUMBER().getText();
					} else if (ctx.INDEX() != null) {
						value += ctx.INDEX().getText();
					} else if (ctx.selector() != null) {
						value += ctx.selector().getText();
					} else {
						//throw new UnsupportedOperationException("unknown state");
						addCSSError(ctx, "Pseudo selector syntax error: " + ctx.getText());
					}
				}
				tmpPseudo = rf.createPseudoPage(value, first);
			}
		}
		// kontrola, zda probehla sematicka kontrola spravne
		if (tmpPseudo != null) {
			log.debug("Setting pseudo: {}", tmpPseudo.toString());
			tmpPseudo.setLocation(getCodeLocation(ctx, 0));
			tmpSelector.add(tmpPseudo);
		}
	}

	@Override
	public void exitPseudo(CSSParser.PseudoContext ctx) {
		// check if is in declaration
		if (tmpDeclarationScope != null && tmpDeclarationScope.d != null && tmpDeclarationScope.invalid) {
			//System.out.println("INVALID PSEUDO");
			stmtIsValid = false;
		}
	}

	@Override
	public void enterPseudocolon(CSSParser.PseudocolonContext ctx) {
		logEnter("pseudocolon: " + ctx.getText());
	}

	@Override
	public void exitPseudocolon(CSSParser.PseudocolonContext ctx) {

	}

	@Override
	public void enterString(CSSParser.StringContext ctx) {
		logEnter("string: " + ctx.getText());
	}

	@Override
	public void exitString(CSSParser.StringContext ctx) {

	}

	@Override
	public void enterAny(CSSParser.AnyContext ctx) {
		logEnter("any: " + ctx.getText());
	}

	@Override
	public void exitAny(CSSParser.AnyContext ctx) {

	}

	@Override
	public void enterNostatement(CSSParser.NostatementContext ctx) {
		logEnter("nostatement: " + ctx.getText());
		stmtIsValid = false;
	}

	@Override
	public void exitNostatement(CSSParser.NostatementContext ctx) {

	}

	@Override
	public void enterNoprop(CSSParser.NopropContext ctx) {
		logEnter("noprop: " + ctx.getText());
	}

	@Override
	public void exitNoprop(CSSParser.NopropContext ctx) {

	}

	@Override
	public void enterNorule(CSSParser.NoruleContext ctx) {
		logEnter("norule: " + ctx.getText());
		addCSSError(ctx, "No rule syntax error: " + ctx.getText());
	}

	@Override
	public void exitNorule(CSSParser.NoruleContext ctx) {
		
	}

	// <editor-fold desc="nomediaquery - done">
	@Override
	public void enterNomediaquery(CSSParser.NomediaqueryContext ctx) {
		// done in enterMedia_term
	}

	@Override
	public void exitNomediaquery(CSSParser.NomediaqueryContext ctx) {
		// done
	}
	// </editor-fold>

	@Override
	public void enterAtstatement(CSSParser.AtstatementContext ctx) {
		logEnter("atstatement: " + ctx.getText());
		// init scope
		tmpAtStatementOrRuleSetScope = new atstatement_scope();

	}

	@Override
	public void exitAtstatement(CSSParser.AtstatementContext ctx) {
		log.debug("exit atstatement: " + ctx.getText());
		if (ctxHasErrorNode(ctx)) {
			log.debug("atstatement is not valid");
			addCSSError(ctx, "At-statement syntax error: " + ctx.getText());
			return;
		}
		if (ctx.CHARSET() != null) {
			String charset = ctx.charset_name().getText();
			tmpAtStatementOrRuleSetScope.stm = preparator.prepareRuleCharset(charset);
			tmpAtStatementOrRuleSetScope.stm.setLocation(getCodeLocation(ctx, 0));
		} else if (ctx.IMPORT() != null) {
			String iuri = extractTextUnescaped(ctx.import_uri().getText());
			
			iuri = iuri.replaceAll("(\"|')", "");
			
			//System.out.println("DEZE HIER: "+ctx.import_uri().getText());
			//System.out.println("DEZE HIER2: "+iuri);
			
			importMedia.add(mediaQueryList);
			importPaths.add(iuri);

			// List<MediaQuery> importMediaQueryList = new ArrayList<>();

			if (ctx.media() == null) {
				// media is not set, set empty
				mediaQueryList = new ArrayList<>();
			}
			
			List<RuleSet> mediaRulesList = new ArrayList<>();
			if (ctx.media_rule() != null) {
				for (RuleBlock<?> rule : tmpRuleList) {
					mediaRulesList.add((RuleSet) rule);
				}
			}

//			StyleSheet a = null;
//			
//			File f = new File(iuri);
//			URL base;
//			URL url = null;
//			try {
//				base = f.toURI().toURL();
//				url = DataURLHandler.createURL(base, iuri);
//			} catch (MalformedURLException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
//			
//			System.out.println("loADING THE FOLLOWING IMPORT: "+url);
//			
//			try {
//				a = CSSFactory.parse(url, "UTF-8");
//				tmpAtStatementOrRuleSetScope.stm = (RuleImport) preparator.prepareRuleImport(iuri, mediaQueryList, a);
//				tmpAtStatementOrRuleSetScope.stm.setLocation(getCodeLocation(ctx, 0));
//			} catch (CSSException e) {
//				a = (StyleSheet) CSSFactory.getRuleFactory().createStyleSheet().unlock();
//				addCSSError(ctx, "Syntax error: " + ctx.getText());
//			} catch (IOException e) {
//				a = (StyleSheet) CSSFactory.getRuleFactory().createStyleSheet().unlock();
//				addCSSError(ctx, "Load error: " + ctx.getText()+" - "+e.getMessage());
//			}
			
			tmpAtStatementOrRuleSetScope.stm = (RuleImport) preparator.prepareRuleImport(iuri, mediaQueryList, null);
			tmpAtStatementOrRuleSetScope.stm.setLocation(getCodeLocation(ctx, 0));
		} else if (ctx.NAMESPACE() != null) {
			String prefix = "";
			if (ctx.prefix != null) {
				prefix = extractTextUnescaped(ctx.prefix.getText());
			}
			String uri;
			if (ctx.STRING() != null) {
				uri = extractTextUnescaped(ctx.STRING().getText());
			} else if (ctx.URI() != null) {
				uri = extractTextUnescaped(ctx.URI().getText());
			} else {
				uri = "";
			}
			tmpAtStatementOrRuleSetScope.stm = preparator.prepareRuleNamespace(prefix, uri);
			tmpAtStatementOrRuleSetScope.stm.setLocation(getCodeLocation(ctx, 0));
		} else if (ctx.COUNTERSTYLE() != null) {
			log.debug("exitAtstatement COUNTERSTYLE");
			String name = extractTextUnescaped(ctx.name.getText());
			//System.out.println("COUNTERS STYLO");
			//System.out.println(tmpDeclarations.toString());
			tmpAtStatementOrRuleSetScope.stm = preparator.prepareRuleCounterStyle(name, tmpDeclarations);
			tmpAtStatementOrRuleSetScope.stm.setLocation(getCodeLocation(ctx, 0));
		} else if (ctx.page() != null) {
			// implemented in exitPage

		} else if (ctx.VIEWPORT() != null) {
			tmpAtStatementOrRuleSetScope.stm = preparator.prepareRuleViewport(tmpDeclarations);
			tmpAtStatementOrRuleSetScope.stm.setLocation(getCodeLocation(ctx, 0));
		} else if (ctx.FONTFACE() != null) {
			tmpAtStatementOrRuleSetScope.stm = preparator.prepareRuleFontFace(tmpDeclarations);
			tmpAtStatementOrRuleSetScope.stm.setLocation(getCodeLocation(ctx, 0));
		} else if (ctx.MEDIA() != null) {
			log.debug("exitAtstatement MEDIA");
			if (ctx.media() == null) {
				// media is not set, set empty
				mediaQueryList = new ArrayList<>();
			}
			List<RuleSet> mediaRulesList = new ArrayList<>();
			if (ctx.media_rule() != null) {
				for (RuleBlock<?> rule : tmpRuleList) {
					mediaRulesList.add((RuleSet) rule);
				}

			}
			tmpAtStatementOrRuleSetScope.stm = preparator.prepareRuleMedia(mediaRulesList, mediaQueryList);
			tmpAtStatementOrRuleSetScope.stm.setLocation(getCodeLocation(ctx, 0));
			this.preventImports = true;
		} else if (ctx.KEYFRAMES() != null) {
			log.debug("exitAtstatement KEYFRAMES");
			//System.out.println("exitAtstatement KEYFRAMES");

			List<RuleSet> keyFrameList = new ArrayList<>();
			if (ctx.keyframes_block() != null) {
				for (RuleBlock<?> rule : tmpRuleList) {
					keyFrameList.add((RuleSet) rule);
				}
			}

			tmpAtStatementOrRuleSetScope.stm = preparator.prepareRuleKeyFrames(ctx.name.getText(), keyFrameList);
			
//			if (tmpStatementComment != null) {
//				tmpAtStatementOrRuleSetScope.stm.setComment(tmpStatementComment);
//				tmpStatementComment = null;
//			}
			
			tmpAtStatementOrRuleSetScope.stm.setLocation(getCodeLocation(ctx, 0));
		} else {
			// unknown atrule
			log.debug("Skipping invalid at statement");
			tmpAtStatementOrRuleSetScope.stm = null;
		}
	}

	@Override
	public void enterImport_uri(CSSParser.Import_uriContext ctx) {
		logEnter("Import_uri");
		// done in exitAtStatement
	}

	@Override
	public void exitImport_uri(CSSParser.Import_uriContext ctx) {
		logLeave("Import_uri");
		// done in exitAtStatement
	}

	@Override
	public void enterPage(CSSParser.PageContext ctx) {
		logEnter("page: " + ctx.getText());
		// initialize margin rules
		tmpMargins = null;
		tmpDeclarations = null;
	}

	@Override
	public void exitPage(CSSParser.PageContext ctx) {
		String name = null;

		if (ctx.IDENT() != null && ctx.COLON() != null) {
			name = (ctx.IDENT().getText());
		}

		RuleBlock<?> rb = preparator.prepareRulePage(tmpDeclarations, tmpMargins, name, ":");
		rb.setLocation(getCodeLocation(ctx, 0));
		if (rb != null) {
			rules.add(rb);
		}

		this.preventImports = true;
	}

	@Override
	public void enterPage_pseudo(CSSParser.Page_pseudoContext ctx) {
		logEnter("page_pseudo: " + ctx.getText());
	}

	@Override
	public void exitPage_pseudo(CSSParser.Page_pseudoContext ctx) {

	}

	@Override
	public void enterMargin_rule(CSSParser.Margin_ruleContext ctx) {
		logEnter("margin_rule: " + ctx.getText());
		if (tmpMargins == null) {
			tmpMargins = new ArrayList<>();
		}
	}

	@Override
	public void exitMargin_rule(CSSParser.Margin_ruleContext ctx) {
		if (tmpMarginRule != null) {
			tmpMargins.add(tmpMarginRule);
			log.debug("Inserted margin rule #{} into @page", tmpMargins.size() + 1);
			tmpMarginRule = null;
		}
	}

	@Override
	public void enterInlineset(CSSParser.InlinesetContext ctx) {
		// TODO: whole rule should be removed due to
		// https://www.w3.org/TR/css-style-attr/#syntax
		logEnter("inlineset: " + ctx.getText());
	}

	@Override
	public void exitInlineset(CSSParser.InlinesetContext ctx) {
		// https://www.w3.org/TR/css-style-attr/#syntax
	}

	@Override
	public void enterMedia(CSSParser.MediaContext ctx) {
		logEnter("media: " + ctx.getText());
		mediaQueryList = new ArrayList<>();
	}

	@Override
	public void exitMedia(CSSParser.MediaContext ctx) {
		tmpMediaQueryScope = null;
		log.debug("Totally returned {} media queries.", mediaQueryList.size());
		logLeave("media");
	}

	@Override
	public void enterMedia_query(CSSParser.Media_queryContext ctx) {
		logEnter("media_query: " + ctx.getText());
		tmpMediaQueryScope = new mediaquery_scope();
		tmpMediaQueryScope.q = rf.createMediaQuery();
		tmpMediaQueryScope.q.setLocation(getCodeLocation(ctx, 0));
		tmpMediaQueryScope.q.unlock();
		tmpMediaQueryScope.state = MediaQueryState.START;
		tmpMediaQueryScope.invalid = false;
	}

	@Override
	public void exitMedia_query(CSSParser.Media_queryContext ctx) {
		logLeave("exitMedia_query1");
		if (tmpMediaQueryScope.invalid) {
			/// mediaquery invalid add NOT ALL
			tmpMediaQueryScope.q = rf.createMediaQuery();
			tmpMediaQueryScope.q.unlock();
			tmpMediaQueryScope.q.setType("all");
			tmpMediaQueryScope.q.setNegative(true);
			log.debug("mediaQuery INVALID - addding NOT ALL");
		}
		log.debug("Adding media query {}", tmpMediaQueryScope.q);
		tmpMediaQueryScope.q.setLocation(getCodeLocation(ctx, 0));
		mediaQueryList.add(tmpMediaQueryScope.q);
	}

	@Override
	public void enterMedia_term(CSSParser.Media_termContext ctx) {
		logEnter("media_term: " + ctx.getText());
		stmtIsValid = true;
		if (ctx.IDENT() != null) {
			log.debug("mediaterm ident");
			String m = extractTextUnescaped(ctx.IDENT().getText());
			MediaQueryState state = tmpMediaQueryScope.state;
			if (m.equalsIgnoreCase("ONLY") && state == MediaQueryState.START) {
				tmpMediaQueryScope.state = MediaQueryState.TYPEOREXPR;
			} else if (m.equalsIgnoreCase("NOT") && state == MediaQueryState.START) {
				tmpMediaQueryScope.q.setNegative(true);
				tmpMediaQueryScope.state = MediaQueryState.TYPEOREXPR;
			} else if (m.equalsIgnoreCase("AND") && state == MediaQueryState.AND) {
				tmpMediaQueryScope.state = MediaQueryState.EXPR;
			} else if (state == MediaQueryState.START || state == MediaQueryState.TYPE
					|| state == MediaQueryState.TYPEOREXPR) {
				tmpMediaQueryScope.q.setType(m);
				tmpMediaQueryScope.state = MediaQueryState.AND;
			} else {
				log.debug("Invalid media query: found ident: {} state: {}", m, state);
				tmpMediaQueryScope.invalid = true;
			}
			tmpMediaQueryScope.q.setLocation(getCodeLocation(ctx, 0));
		} else if (ctx.media_expression() != null) {
			// in enterMedia_expression
			// empty here
		} else if (ctx.nomediaquery() != null) {
			// nomediaquery -> mediaquery is invalid
			tmpMediaQueryScope.invalid = true;
		}
	}

	@Override
	public void exitMedia_term(CSSParser.Media_termContext ctx) {
		if (ctx.media_expression() != null) {
			if (tmpMediaQueryScope.state == MediaQueryState.START || tmpMediaQueryScope.state == MediaQueryState.EXPR
					|| tmpMediaQueryScope.state == MediaQueryState.TYPEOREXPR) {
				if (tmpMediaExpression.getFeature() != null) // the expression
																// is valid
				{
					tmpMediaQueryScope.q.add(tmpMediaExpression);
					tmpMediaQueryScope.state = MediaQueryState.AND;
				} else {
					log.trace("Invalidating media query for invalud expression");
					tmpMediaQueryScope.invalid = true;
				}
			} else {
				log.trace("Invalid media query: found expr, state: {}", tmpMediaQueryScope.state);
				tmpMediaQueryScope.invalid = true;
			}
		}
	}

	@Override
	public void enterMedia_expression(CSSParser.Media_expressionContext ctx) {
		logEnter("media_expression: " + ctx.getText());

		// create temp media expression storage
		tmpMediaExpression = rf.createMediaExpression();
		tmpMediaExpression.setLocation(getCodeLocation(ctx, 0));
		// create temp declaration storage
		tmpDeclarationScope = getDeclarationScopeAndInit();
		// set property to declaration
		tmpDeclarationScope.d.setProperty(extractTextUnescaped(ctx.IDENT().getText()));
		Token token = ctx.IDENT().getSymbol();
		tmpDeclarationScope.d.setSource(extractSource((CSSToken) token));

		tmpDeclarationScope.d.setLocation(getCodeLocation(ctx, 0));
	}

	@Override
	public void exitMedia_expression(CSSParser.Media_expressionContext ctx) {
		if (ctx.terms() != null) {
			// terms were specified so set terms list
			tmpDeclarationScope.d.replaceAll(tmpTermList);
		}
		if (tmpDeclarationScope.d != null) { // if the declaration is valid
			tmpMediaExpression.setFeature(tmpDeclarationScope.d.getProperty());
			tmpMediaExpression.replaceAll(tmpDeclarationScope.d);
		}
		if (ctxHasErrorNode(ctx)) {
			addCSSError(ctx, "Media expression syntax error: " + ctx.getText());
			log.debug("media_expression is invalid");
			tmpMediaQueryScope.invalid = true;
		}
	}

	@Override
	public void enterMedia_rule(CSSParser.Media_ruleContext ctx) {
		logEnter("media_rule: " + ctx.getText());
	}

	@Override
	public void exitMedia_rule(CSSParser.Media_ruleContext ctx) {
		List<RuleSet> tmpAtStatementRules = null;
		
		if (ctx.ruleset() != null) {
			if (stmtIsValid) {
				//System.out.println(tmpRuleList.size());
				for (RuleBlock<?> rule : tmpRuleList) {
					if (rule != null) {
						if (tmpMediaRuleComment != null) {
							rule.setComment(tmpMediaRuleComment);
							rule.setLocation(getCodeLocation(ctx, 0));
							tmpMediaRuleComment = null;
						}
						log.debug("exitStatement |ADDING statement {}", rule);
						rules.add(rule);
					} else {
						log.debug("exitStatement |ommited null statement ");
					}
				}
			} else {
				log.debug("exitStatement | statement is not valid, so not adding it");
			}
		} else {
			if (tmpAtStatementOrRuleSetScope.stm != null) {
				log.debug("exitStatement | ADDING statement {}", tmpAtStatementOrRuleSetScope.stm);

				if (tmpMediaRuleComment != null) {
					tmpAtStatementOrRuleSetScope.stm.setComment(tmpMediaRuleComment);
					tmpAtStatementOrRuleSetScope.stm.setLocation(getCodeLocation(ctx, 0));
					tmpMediaRuleComment = null;
				}

				rules.add(tmpAtStatementOrRuleSetScope.stm);
			}
		}
	}

	@Override
	public void enterUnknown_atrule(CSSParser.Unknown_atruleContext ctx) {
		logEnter("unknown_atrule: " + ctx.getText());
		// done in exitAtstatement
	}

	@Override
	public void exitUnknown_atrule(CSSParser.Unknown_atruleContext ctx) {
		// empty
	}

	@Override
	public void visitTerminal(TerminalNode terminalNode) {
		// empty
	}

	@Override
	public void visitErrorNode(ErrorNode errorNode) {
		// empty
	}

	@Override
	public void enterEveryRule(ParserRuleContext parserRuleContext) {
		spacesCounter += 2;
		if (parserRuleContext.getChildCount() == 0) {
			return;
		}
		childernWithoutSpaces = filterSpaceTokens(parserRuleContext.children);
	}

	@Override
	public void exitEveryRule(ParserRuleContext parserRuleContext) {
		spacesCounter -= 2;
	}

	@Override
	public void enterComment(CommentContext ctx) {
		logEnter("comment: " + ctx.getText());
	}

	@Override
	public void exitComment(CommentContext ctx) {
		logLeave("comment: " + ctx.getText());
		cz.vutbr.web.css.CodeLocation location = getCodeLocation(ctx, 0);

		String context = ctx.getParent().getClass().getSimpleName();
		System.out.println("CONTEXT: "+context);
		if (tmpStyleSheetComment == null && preventStyleSheetComment == false) {
			tmpStyleSheetComment = new CommentImpl(ctx.getText(), location);
		} else if (context.equals("StatementContext")) {
			tmpStatementComment = new CommentImpl(ctx.getText(), location);
		} else if (context.equals("DeclarationContext")) {
			tmpDeclarationComment = new CommentImpl(ctx.getText(), location);
		} else if (context.equals("Keyframes_blockContext")) {
			tmpKeyframeComment = new CommentImpl(ctx.getText(), location);
		} else if (context.equals("Media_ruleContext")) {
			tmpMediaRuleComment = new CommentImpl(ctx.getText(), location);
		}
	}

	private String[] splitLines(String str) {
		String[] lines = str.split("\r\n|\r|\n");
		return lines;
	}

	private cz.vutbr.web.css.CodeLocation getCodeLocation(ParserRuleContext ctx, int realLength) {

		String[] lines = splitLines(ctx.getText());

		int offset = ctx.getStart().getStartIndex();

		int length;
		// if (lengthen) {
		// length = ctx.getStop().getStopIndex() -
		// ctx.getStart().getStartIndex() + 2;
		// } else {
		// length = ctx.getStop().getStopIndex() -
		// ctx.getStart().getStartIndex();
		// }

		length = ctx.getText().trim().length();
		if (length == 0) {
			return new CodeLocation(offset, length, 0, 0, 0, 0);
		}
		// length = realLength;

		int startLine = ctx.getStart().getLine();
		int endLine = (startLine + lines.length - 1);
		int startPosition = ctx.getStart().getCharPositionInLine();

		int endPosition;
		if (ctx.getStart().getLine() == (ctx.getStart().getLine() + lines.length - 1)) {
			endPosition = startPosition + ctx.getStop().getStopIndex() - ctx.getStart().getStartIndex();
		} else {
			endPosition = lines[lines.length - 1].length() + 1;
		}

		// //System.out.println(ctx.getText());
		// //System.out.println("");
		// //System.out.println("LINES: "+lines.length);
		// //System.out.println("LINES-1: "+(lines.length-1));
		// //System.out.println("");
		// //System.out.println("");
		// //System.out.println("OFFSET: "+offset);
		// //System.out.println("LENGTH: "+(ctx.getStop().getStopIndex() -
		// ctx.getStart().getStartIndex()));
		// //System.out.println("STARTLINE: "+startLine);
		// //System.out.println("ENDLINE: "+endLine);
		// //System.out.println("STARTPOS:"+startPosition);
		// //System.out.println("ENDPOS: "+endPosition);
		// //System.out.println("");
		// //System.out.println(startLine<=endLine);
		// //System.out.println(startPosition<=endPosition);
		// //System.out.println("");
		// //System.out.println("");
		// //System.out.println("");

		return new CodeLocation(offset, length, startLine, startPosition, endLine, endPosition);
	}

	@Override
	public void enterCharset_name(Charset_nameContext ctx) {
		logEnter("CHARSET NAME");
	}

	@Override
	public void exitCharset_name(Charset_nameContext ctx) {
		logEnter("CHARSET NAME");
	}

	@Override
	public void enterCalcsum(CalcsumContext ctx) {
		logEnter("Calcsum");
	}

	@Override
	public void exitCalcsum(CalcsumContext ctx) {
		if (ctxHasErrorNode(ctx)) {
			log.debug("invalidating terms");
			addCSSError(ctx, "Calc sum syntax error: " + ctx.getText());
			tmpDeclarationScope.invalid = true;
			//System.out.println("INVALID CALCSUM");
		}
	}

	@Override
	public void enterCalcproduct(CalcproductContext ctx) {
		logEnter("Calcproduct");
	}

	@Override
	public void exitCalcproduct(CalcproductContext ctx) {
		if (ctxHasErrorNode(ctx)) {
			log.debug("invalidating terms");
			addCSSError(ctx, "Calc product syntax error: " + ctx.getText());
			tmpDeclarationScope.invalid = true;
			//System.out.println("INVALID CALCPRODUCT");
		}
	}

	@Override
	public void enterCalcvalue(CalcvalueContext ctx) {
		logEnter("Calcvalue");
	}

	@Override
	public void exitCalcvalue(CalcvalueContext ctx) {
		if (ctxHasErrorNode(ctx)) {
			log.debug("invalidating terms");
			addCSSError(ctx, "Calc value syntax error: " + ctx.getText());
			tmpDeclarationScope.invalid = true;
			//System.out.println("INVALID CALCVALUE");
		}
	}

	@Override
	public void enterKeyframe_selectors(Keyframe_selectorsContext ctx) {
		logEnter("keyframes_selectors: " + ctx.getText());

		tmpCombinedSelectorInvalid = false;
		tmpCombinedSelector = (CombinedSelector) rf.createCombinedSelector().unlock();
		tmpCombinedSelector.setLocation(getCodeLocation(ctx, 0));
	}

	@Override
	public void exitKeyframe_selectors(Keyframe_selectorsContext ctx) {
		if (!tmpCombinedSelectorInvalid) {
			tmpCombinedSelector.setLocation(getCodeLocation(ctx, 0));
			tmpCombinedSelectorList.add(tmpCombinedSelector);
			log.debug("Returing combined selector: {}.", tmpCombinedSelector);
		} else {
			log.debug("Combined selector is invalid");
		}
		tmpCombinator = null;
	}

	@Override
	public void enterKeyframe_selector(Keyframe_selectorContext ctx) {
		logEnter("keyframes_selector: " + ctx.getText());

		tmpSelector = (Selector) rf.createSelector().unlock();

		if (ctx.FROM_SYM() != null || ctx.TO_SYM() != null) {
			Selector.KeyframesIdent elem = rf.createKeyFramesIdent(ctx.getText());
			elem.setLocation(getCodeLocation(ctx, 0));
			tmpSelector.add(elem);
		} else if (ctx.PERCENTAGE() != null) {
			Selector.KeyframesPercentage elem = rf
					.createKeyFramesPercentage(ctx.getText().substring(0, ctx.getText().length() - 1));
			elem.setLocation(getCodeLocation(ctx, 0));
			tmpSelector.add(elem);
		}

		tmpSelector.setLocation(getCodeLocation(ctx, 0));

	}

	@Override
	public void exitKeyframe_selector(Keyframe_selectorContext ctx) {
		tmpCombinedSelector.add(tmpSelector);
	}

	@Override
	public void enterKeyframes_block(Keyframes_blockContext ctx) {
		logEnter("keyframes_block: " + ctx.getText());

		stmtIsValid = true;
		// init scope
		tmpAtStatementOrRuleSetScope = new atstatement_scope();
		tmpCombinedSelectorList = new ArrayList<>();
	}

	@Override
	public void exitKeyframes_block(Keyframes_blockContext ctx) {
		tmpAtStatementOrRuleSetScope.stm = preparator.prepareRuleSet(tmpCombinedSelectorList, tmpDeclarations,
				(this.wrapMedia != null && !this.wrapMedia.isEmpty()), this.wrapMedia);
		
		if (tmpAtStatementOrRuleSetScope.stm != null && !ctxHasErrorNode(ctx)) {
			tmpAtStatementOrRuleSetScope.stm.setLocation(getCodeLocation(ctx, 0));
			
			if (tmpKeyframeComment != null) {
				tmpAtStatementOrRuleSetScope.stm.setComment(tmpKeyframeComment);
				tmpKeyframeComment = null;
			}
			
			tmpRuleList.add(tmpAtStatementOrRuleSetScope.stm);
		}
		
		// cleanup tmpDeclarations
		tmpDeclarations = null;
	}

	@Override
	public void enterOperator(OperatorContext ctx) {
		logEnter("Operator: '" + ctx.getText().trim() + "'");
	}

	@Override
	public void exitOperator(OperatorContext ctx) {
		logLeave("Operator: '" + ctx.getText().trim() + "'");
		String op = ctx.getText().trim();
		if (op.equals("/")) {
			tmpOperator = Term.Operator.SLASH;
		} else if (op.equals(",")) {
			tmpOperator = Term.Operator.COMMA;
		} else {
			tmpOperator = Term.Operator.SPACE;
		}
		//System.out.println("LEAVE OPERATOR: " + tmpOperator);
	}

}
