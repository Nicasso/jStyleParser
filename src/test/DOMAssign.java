package test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.tidy.Tidy;

import cz.vutbr.web.css.CSSFactory;
import cz.vutbr.web.css.NodeData;
import cz.vutbr.web.css.TermColor;
import cz.vutbr.web.css.TermFactory;
import cz.vutbr.web.css.TermList;

public class DOMAssign {
	private static final Logger log = LoggerFactory.getLogger(DOMAssign.class);
	
	private static Document doc;
	private static ElementMap elements;
	
	private static TermFactory tf = CSSFactory.getTermFactory();
	
	@BeforeClass
	public static void init() throws FileNotFoundException {
		log.info("\n\n\n == DOMAssign test at {} == \n\n\n", new Date());
		Tidy parser = new Tidy();
        parser.setInputEncoding("utf-8");
		doc = parser.parseDOM(new FileInputStream("data/advanced/domassign.html"),
				null);
		elements = new ElementMap(doc);
	}
	
	@Test
	public void test() throws MalformedURLException {	
		
		Map<Element, NodeData> decl = CSSFactory.assignDOM(doc, 
				createBaseFromFilename("data/advanced/domassign.html"),"screen", true);
		
		NodeData data = decl.get(elements.getElementById("bp"));
		assertNotNull("Data for #bp exist", data);
		
		assertThat(data.getValue(TermList.class, "background-position").size(), is(2));
		
		assertThat(data.getValue(TermColor.class, "color"), is(tf.createColor(255, 255, 255)));
		
		data = decl.get(elements.getElementById("battlecruiser"));
		
		assertThat(tf.createColor(255, 255, 0), is(data.getValue(TermColor.class, "color")));
		
		data = decl.get(elements.getElementById("border"));
		assertNotNull("Data for #border exist", data);
		assertThat(data.getValue(TermColor.class, "border-bottom-color"),
				is(tf.createColor(255,255,255)));
		
	}
	
	private static URL createBaseFromFilename(String filename) {
		try {
			File f = new File(filename);
			return f.toURI().toURL();
		} catch (MalformedURLException e) {
			return null;
		}
	}
}