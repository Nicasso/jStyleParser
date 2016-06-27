package cz.vutbr.web;

import java.io.IOException;
import java.util.Iterator;

import cz.vutbr.web.css.CSSComment;
import cz.vutbr.web.css.CSSException;
import cz.vutbr.web.css.CSSFactory;
import cz.vutbr.web.css.CSSNodeVisitor;
import cz.vutbr.web.css.CombinedSelector;
import cz.vutbr.web.css.Declaration;
import cz.vutbr.web.css.MediaExpression;
import cz.vutbr.web.css.MediaQuery;
import cz.vutbr.web.css.MediaSpec;
import cz.vutbr.web.css.Rule;
import cz.vutbr.web.css.RuleBlock;
import cz.vutbr.web.css.RuleCharset;
import cz.vutbr.web.css.RuleCounterStyle;
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
import cz.vutbr.web.css.StyleSheet;
import cz.vutbr.web.css.Term;
import cz.vutbr.web.css.TermAngle;
import cz.vutbr.web.css.TermAudio;
import cz.vutbr.web.css.TermCalc;
import cz.vutbr.web.css.TermColor;
import cz.vutbr.web.css.TermExpression;
import cz.vutbr.web.css.TermFloatValue;
import cz.vutbr.web.css.TermFrequency;
import cz.vutbr.web.css.TermFunction;
import cz.vutbr.web.css.TermIdent;
import cz.vutbr.web.css.TermInteger;
import cz.vutbr.web.css.TermLength;
import cz.vutbr.web.css.TermList;
import cz.vutbr.web.css.TermNumber;
import cz.vutbr.web.css.TermPercent;
import cz.vutbr.web.css.TermResolution;
import cz.vutbr.web.css.TermString;
import cz.vutbr.web.css.TermTime;
import cz.vutbr.web.css.TermURI;
import cz.vutbr.web.css.Selector.ElementAttribute;
import cz.vutbr.web.css.Selector.ElementClass;
import cz.vutbr.web.css.Selector.ElementDOM;
import cz.vutbr.web.css.Selector.ElementID;
import cz.vutbr.web.css.Selector.ElementName;
import cz.vutbr.web.css.Selector.KeyframesIdent;
import cz.vutbr.web.css.Selector.KeyframesPercentage;
import cz.vutbr.web.css.Selector.PseudoPage;
import cz.vutbr.web.css.Selector.SelectorPart;
import cz.vutbr.web.csskit.RuleArrayList;

public class Nico2 implements CSSNodeVisitor  {
	
	public Nico2() {
		loadStylesheet("style.css");
	}

	private void loadStylesheet(String fileName) {

		StyleSheet style = null;

		if (fileName != null) {
			try {
				style = CSSFactory.parse(fileName, "UTF-8");
				System.out.println("----------------------------");
				System.out.println("NAME: "+style.getName());
				if (style.getComment() != null) {
					System.out.println("Stylesheet comment: " + style.getComment().getText());
					System.out.println("Stylesheet comment location: " + style.getComment().getLocation().toString());
				}
				
				System.out.println("ERRORS: " + style.getCSSErrors().size());
				for (int i = 0; i < style.getCSSErrors().size(); i++) {
					System.out.println(style.getCSSErrors().get(i).getMessage() + " - "
							+ style.getCSSErrors().get(i).getLocation().toString());
				}
				style.accept(this);
			} catch (CSSException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {
				style = CSSFactory.parse(fileName, "UTF-8");
				style.accept(this);
			} catch (CSSException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	@Override
	public Void visit(Declaration node) {
		System.out.println("Declaration");
		System.out.println("\t" + node.getProperty());
		
		if (node.getComment() != null) {
			System.out.println("\t" + node.getComment().getText());
		}

		for (Iterator<Term<?>> it = node.iterator(); it.hasNext();) {
			Term<?> t = it.next();
			t.accept(this);
		}
		
		return null;
	}

	@Override
	public Void visit(CombinedSelector node) {
		System.out.println("CombinedSelector");

		for (Iterator<Selector> it = node.iterator(); it.hasNext();) {
			Selector s = it.next();
			s.accept(this);
		}
		return null;
	}

	@Override
	public Void visit(MediaExpression node) {
		System.out.println("MediaExpression");
		System.out.println(node.getFeature());
		
		for (Iterator<Term<?>> it = node.iterator(); it.hasNext();) {
			Term<?> t = it.next();
			t.accept(this);
		}
		return null;
	}

	@Override
	public Void visit(MediaQuery node) {
		System.out.println("MediaQuery");
		System.out.println(node.getType());
		
		for (Iterator<MediaExpression> it = node.iterator(); it.hasNext();) {
			MediaExpression m = it.next();
			m.accept(this);
		}
		
		return null;
	}

	@Override
	public Void visit(RuleFontFace node) {
		System.out.println("RuleFontFace");
		
		if (node.getComment() != null) {
			System.out.println("\t" + node.getComment().getText());
		}

		for (Iterator<Declaration> it = node.iterator(); it.hasNext();) {
			Declaration d = it.next();
			d.accept(this);
		}
		return null;
	}

	@Override
	public Void visit(RuleMedia node) {
		System.out.println("RuleMedia");
		
		if (node.getComment() != null) {
			System.out.println("\t" + node.getComment().getText());
		}

		for (Iterator<MediaQuery> it = node.getMediaQueries().iterator(); it.hasNext();) {
			MediaQuery m = it.next();
			m.accept(this);
		}

		for (Iterator<RuleSet> it = node.iterator(); it.hasNext();) {
			RuleSet r = it.next();
			r.accept(this);
		}
		return null;
	}

	@Override
	public Void visit(RulePage node) {
		System.out.println("RulePage");
		
		if (node.getComment() != null) {
			System.out.println("\t" + node.getComment().getText());
		}

		for (Iterator<Rule<?>> it = node.iterator(); it.hasNext();) {
			Rule<?> r = it.next();
			r.accept(this);
		}
		return null;
	}

	@Override
	public Void visit(RuleSet node) {
		System.out.println("RuleSet");
		
		if (node.getComment() != null) {
			System.out.println("\t" + node.getComment().getText());
		}

		for (CombinedSelector cs : node.getSelectors()) {
			cs.accept(this);
		}

		for (Declaration cs : node) {
			cs.accept(this);
		}
		return null;
	}

	@Override
	public Void visit(RuleViewport node) {
		System.out.println("RuleViewport");
		
		if (node.getComment() != null) {
			System.out.println("\t" + node.getComment().getText());
		}

		for (Iterator<Declaration> it = node.iterator(); it.hasNext();) {
			Declaration d = it.next();
			d.accept(this);
		}
		return null;
	}

	@Override
	public Void visit(Selector node) {
		System.out.println("Selector");
		System.out.println("\t"+node.getCombinator());

		for (Iterator<SelectorPart> it = node.iterator(); it.hasNext();) {
			SelectorPart m = it.next();
			m.accept(this);
		}
		
		return null;
	}

	@Override
	public Void visit(StyleSheet node) {
		System.out.println("StyleSheet");
		
		if (node.getComment() != null) {
			System.out.println("\t" + node.getComment().getText());
		}
		
		for (Iterator<RuleBlock<?>> it = node.iterator(); it.hasNext();) {
			RuleBlock<?> r = it.next();
			r.accept(this);
		}
		return null;
	}

	@Override
	public Void visit(TermAngle node) {
		System.out.println("TermAngle");
		System.out.println("\t" + node.getValue() + " " + node.getUnit());
		return null;
	}
	
	@Override
	public Object visit(TermAudio node) {
		System.out.println("TermAudio");
		System.out.println("\t" + node.getValue() + " " + node.getUnit());
		
		return null;
	}

	@Override
	public Void visit(TermColor node) {
		System.out.println("TermColor");
		System.out.println("\t" + node.getValue());
		
		return null;
	}

	@Override
	public Void visit(TermExpression node) {
		System.out.println("TermExpression");
		System.out.println("\t" + node.getValue());
		
		return null;
	}

	@Override
	public Void visit(TermFrequency node) {
		System.out.println("TermFrequency");
		System.out.println("\t" + node.getValue() + " " + node.getUnit());
		
		return null;
	}

	@Override
	public Void visit(TermFunction node) {
		System.out.println("TermFunction");
		System.out.println(node.getFunctionName());

		for (Iterator<Term<?>> it = node.iterator(); it.hasNext();) {
			Term<?> t = it.next();
			t.accept(this);
		}
		return null;
	}

	@Override
	public Void visit(TermIdent node) {
		System.out.println("TermIdent");
		System.out.println("\t" + node.getValue());
		
		return null;
	}

	@Override
	public Void visit(TermInteger node) {
		System.out.println("TermInteger");
		// For some strange reason termInteger contains floats...
		System.out.println("\t" + node.getValue().intValue()+ " " + node.getUnit());
		
		return null; 
	}
	
	@Override
	public Void visit(TermLength node) {
		System.out.println("TermLength");
		System.out.println("\t" + node.getValue() + " " + node.getUnit());
		
		return null;
		
	}

	@Override
	public Void visit(TermNumber node) {
		System.out.println("TermNumber");
		System.out.println("\t" + node.getValue() + " " + node.getUnit());
		
		return null;
		
	}

	@Override
	public Void visit(TermPercent node) {
		System.out.println("TermPercent");
		System.out.println("\t" + node.getValue() + " " + node.getUnit());
		
		return null;
	}

	@Override
	public Void visit(TermResolution node) {
		System.out.println("TermResolution");
		System.out.println("\t" + node.getValue() + " " + node.getUnit());
		
		return null;
		
	}

	@Override
	public Void visit(TermString node) {
		System.out.println("TermString");
		System.out.println("\t" + node.getValue());
		
		return null;
	}

	@Override
	public Void visit(TermTime node) {
		System.out.println("TermTime");
		System.out.println("\t" + node.getValue() + " " + node.getUnit());
		
		return null;
	}

	@Override
	public Void visit(TermURI node) {
		System.out.println("TermURI");
		System.out.println("\t" + node.getValue());

		return null;
	}

	@Override
	public Void visit(ElementAttribute node) {
		System.out.println("ElementAttribute");
		System.out.println("\t" + node.getAttribute() + " " + node.getOperator() + " " + node.getValue());
		
		return null;
	}

	@Override
	public Void visit(ElementClass node) {
		System.out.println("ElementClass");
		System.out.println("\t" + node.getClassName());
		
		return null;
	}

	@Override
	public Void visit(ElementID node) {
		System.out.println("ElementID");
		System.out.println("\t" + node.getID());
		
		return null;
	}

	@Override
	public Void visit(ElementName node) {
		System.out.println("ElementName");
		System.out.println("\t" + node.getName());
		
		return null;
	}

	@Override
	public Void visit(PseudoPage node) {
		System.out.println("PseudoPage");
		System.out.println("\t value: " + node.getValue());
		System.out.println("\t elem: " + node.getDeclaration().isPseudoElement());
		
		return null;
	}

	@Override
	public Object visit(RuleImport node) {
		System.out.println("RuleImport");
		System.out.println("\t" + node.getURI());
		
		if (node.getComment() != null) {
			System.out.println("\t" + node.getComment().getText());
		}
		
		if (node.getMediaQueries().size() != 0) {
			for (Iterator<MediaQuery> it = node.getMediaQueries().iterator(); it.hasNext();) {
				MediaQuery m = it.next();
				m.accept(this);
			}
			
		}
		return null;
	}

	@Override
	public Object visit(CSSComment node) {
		System.out.println("CSSComment");
		System.out.println("\t" + node.getText());
		
		return null;
	}

	@Override
	public Object visit(RuleCharset node) {
		System.out.println("RuleCharset");
		System.out.println("\t" + node.getCharset());
		
		if (node.getComment() != null) {
			System.out.println("\t" + node.getComment().getText());
		}
		
		return null;
	}

	@Override
	public Object visit(TermCalc node) {
		System.out.println("TermCalc");
		System.out.println("\t" + node.getValue());
		return null;
	}

	@Override
	public Object visit(RuleCounterStyle node) {
		System.out.println("RuleCounterStyle");
		
		if (node.getComment() != null) {
			System.out.println("\t" + node.getComment().getText());
		}
		
		for (Iterator<Declaration> it = node.iterator(); it.hasNext();) {
			Declaration d = it.next();
			d.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(RuleNameSpace node) {
		System.out.println("RuleNameSpace");
		System.out.println("\t" + node.getUri());
		
		if (node.getComment() != null) {
			System.out.println("\t" + node.getComment().getText());
		}
		
		return null;
	}

	@Override
	public Object visit(KeyframesPercentage node) {
		System.out.println("keyframesPercentage");
		System.out.println("\t" + node.getPercentage());
		
		return null;
	}

	@Override
	public Object visit(KeyframesIdent node) {
		System.out.println("KeyframesIdent");
		System.out.println("\t" + node.getIdent());
		
		return null;
	}

	@Override
	public Object visit(RuleKeyframes node) {
		System.out.println("RuleKeyframes");
		
		if (node.getComment() != null) {
			System.out.println("\t" + node.getComment().getText());
		}

		for (Iterator<RuleSet> it = node.iterator(); it.hasNext();) {
			RuleSet r = it.next();
			r.accept(this);
		}
		
		return null;
	}
	
}
