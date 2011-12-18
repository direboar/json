package controllers;

import play.*;
import play.libs.WS;
import play.mvc.*;
import util.Atom2Json;

import java.io.StringWriter;
import java.util.*;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.sun.corba.se.impl.encoding.OSFCodeSetRegistry.Entry;
import com.sun.corba.se.impl.oa.poa.AOMEntry;

import models.*;

/**
 * Google Bookの旧API（atom形式の通信だけサポート）をJsonに変換するWebサービス
 * @author eiji
 */
public class GoogleBook extends Controller {

	/**
	 * 本Webサービスの説明ページを表示する
	 */
	public static void index() {
		render();
	}

	/**
	 * 本Webサービスの戻り値の例を表示する
	 */
	public static void returnValue() {
		render();
	}

	private static boolean isEmpty(String string) {
		return string == null || string.trim().equals("");
	}

	/**
	 * サンプル画面から本サービスを使用する
	 * @param q
	 * @param startIndex
	 * @param maxResults
	 * @param minViewability
	 * @param lr
	 * @param callback
	 */
	public static void feedsForm(String q, String startIndex,
			String maxResults, String minViewability, String lr, String callback) {

		String url = "http://books.google.com/books/feeds/volumes?";
		if (!isEmpty(q))
			url += "&q=" + q;
		if (!isEmpty(startIndex))
			url += "&start-index=" + startIndex;
		if (!isEmpty(maxResults))
			url += "&max-results=" + maxResults;
		if (!isEmpty(minViewability))
			url += "&min-viewability=" + minViewability;
		if (!isEmpty(lr))
			url += "&lr=" + lr;
		Document document = WS.url(url).get().getXml("UTF-8");

		Atom2Json atom2Json = new Atom2Json();
		atom2Json.addNonArrayElemName("gbs:contentVersion");
		atom2Json.addNonArrayElemName("dc:title");
		atom2Json.addNonArrayElemName("dc:subject");
		atom2Json.addNonArrayElemName("dc:publisher");
		atom2Json.addNonArrayElemName("dc:language");
		atom2Json.addNonArrayElemName("dc:description");
		atom2Json.addNonArrayElemName("dc:date");
		atom2Json.addNonArrayElemName("dc:creator");

		if (!isEmpty(callback)) {
			atom2Json.setCallback(callback);
		}

		renderJSON(atom2Json.toJson(document));
	}

	/**
	 * googlebookに連携するサービス
	 * @throws Exception
	 */
	public static void feeds() throws Exception {
		String url = "http://books.google.com/books/feeds/volumes?";
		String callback = null;
		Map<String, String> param = params.allSimple();
		for (Map.Entry<String, String> entry : param.entrySet()) {
			if (entry.getKey().equals("callback")) {
				callback = entry.getValue();
			} else {
				url += "&" + entry.getKey() + "=" + entry.getValue();
			}
		}

		Document document = WS.url(url).get().getXml("UTF-8");

		Atom2Json atom2Json = new Atom2Json();
		atom2Json.addNonArrayElemName("gbs:contentVersion");
		atom2Json.addNonArrayElemName("dc:title");
		atom2Json.addNonArrayElemName("dc:subject");
		atom2Json.addNonArrayElemName("dc:publisher");
		atom2Json.addNonArrayElemName("dc:language");
		atom2Json.addNonArrayElemName("dc:description");
		atom2Json.addNonArrayElemName("dc:date");
		atom2Json.addNonArrayElemName("dc:creator");
		if (callback != null) {
			atom2Json.setCallback(callback);
		}

		renderJSON(atom2Json.toJson(document));
	}

}