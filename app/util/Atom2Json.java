package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * AtomをJson形式に変換するユーティリティクラス
 * @author minokuba
 */
public class Atom2Json {

	/**
	 * atomで決まっている配列で扱わない要素名
	 */
	private static final String[] NON_ARRAY_ATTRIBUTE_NAMES = new String[] {
			"email","generator", "icon", "id", "logo", "name", "published", "rights", "title",
			"subtitle", "summary", "updated" ,"uri" };

	/**
	 *  atomのextensionElementについて、配列で扱わない要素名を管理する配列
	 */
	private List<String> customizedNonArrayAttributeNames = new ArrayList<String>();
	
	/**
	 * JSONPのコールバック関数
	 */
	private String callback = null;

	/**
	 * JSONP形式で返却する場合に、callback関数名を指定する
	 * @param callback
	 */
	public void setCallback(String callback) {
		this.callback = callback;
	}

	/**
	 * atomのextensionElementについて、配列で扱わない要素名を設定する
	 * @param elmentName
	 */
	public void addNonArrayElemName(String elmentName) {
		customizedNonArrayAttributeNames.add(elmentName);
	}


	/**
	 * 配列として扱うか？
	 * @param elementName
	 * @return
	 */
	private boolean isArray(String elementName) {
		if (Arrays.binarySearch(NON_ARRAY_ATTRIBUTE_NAMES, elementName) > 0) {
			return false;
		} else {
			if (this.customizedNonArrayAttributeNames.contains(elementName)) {
				return false;
			} else {
				return true;
			}
		}
	}

	/**
	 * atomのDOMオブジェクトをJson文字列に変換する。callbackが設定されている場合は、JSONP形式で返却する。
	 * @param document atomのDOMオブジェクト
	 * @return　JSONP文字列
	 */
	public String toJson(Document document) {
		JsonObject jsonObject = toJsonObject(document);
		Gson gson = new Gson();
		if (callback == null) {
			return gson.toJson(jsonObject);
		} else {
			return callback + "(" + gson.toJson(jsonObject) + ")";
		}
	}

	/**
	 * atomのDOMオブジェクトをJsonObject文字列に変換する。
	 * @param document atonのDocumentオブジェクト
	 * @return JsonObject(feedに対応する）
	 */
	public JsonObject toJsonObject(Document document) {
		Element feed = document.getDocumentElement();
		JsonElement jsonelementFeed = toJsonElement(feed);
		if (!(jsonelementFeed instanceof JsonObject)) {
			// TODO めんどいｗ
			throw new RuntimeException();
		} else {
			JsonObject jsonobjectFeed = (JsonObject) jsonelementFeed;
			handleChildrenElement(jsonobjectFeed, feed);
			return jsonobjectFeed;
		}
	}

	/**
	 * 指定したElementの子要素として持つElementを評価し、JsonObjectの属性に設定する。
	 * @param parentJson　処理対象のJsonObject
	 * @param parentElement parentJsonに対応するatomのElementオブジェクト
	 */
	private void handleChildrenElement(JsonObject parentJson,
			Element parentElement) {
		NodeList nodeChildren = parentElement.getChildNodes();
		for (int j = 0; j < nodeChildren.getLength(); j++) {
			Node childNode = nodeChildren.item(j);
			if (childNode instanceof Element) {
				Element childElement = (Element) childNode;

				JsonElement chilJson = toJsonElement(childElement);
				String elementName = childElement.getTagName();
				//複数回登場するElementの場合は配列として設定。そうでない場合は属性として設定。
				if (isArray(childElement.getTagName())) {
					JsonArray jsonArray = (JsonArray) parentJson.get(elementName);
					if (jsonArray == null) {
						jsonArray = new JsonArray();
						parentJson.add(elementName, jsonArray);
					}
					jsonArray.add(chilJson);
				} else {
					parentJson.add(elementName, chilJson);
				}
				
				//子要素のElementより生成したJsonElementがJsonObjectの場合は、再帰的に子要素を評価する。
				//TODO 実際はJsonObjectに変換されるケースとしては属性を持つが子要素がないケースも含まれるので、今の処理は処理効率上ちょっとイマイチ
				if (chilJson instanceof JsonObject) {
					handleChildrenElement((JsonObject) chilJson, childElement);
				}
			}
		}
	}

	/**
	 * XMLElementをJsonElementに変換する。
	 * 型の決定ルール:
	 * <ol>
	 *  <li>Elementに属性がある場合はJsonObjectに変換する<li>
	 *  <li>Elementの子ノードにElementがある場合は、JsonObject似変換する<li>
	 *  <li>上記以外の場合は、Element内のテキストコンテンツを扱う文字列型として扱う<li>
	 * </ol>
	 * JsonObjectの場合の属性値決定ルール
	 * <ol>
	 *  <li>Elementに属性がある場合は、全てJsonObjectの属性として設定する。<li>
	 *  <li>Elementの子ノードにElementがない場合は、Element内のテキストコンテンツをcontentという名前の属性でJsonObjectに設定する<li>
	 * </ol>
	 * @param element
	 * @return
	 */
	private JsonElement toJsonElement(Element element) {
		NamedNodeMap namedNodeMap = element.getAttributes();
		NodeList childrenNode = element.getChildNodes();

		boolean hasAttribute = namedNodeMap.getLength() != 0;
		boolean hasElement = false;
		for (int i = 0; i < childrenNode.getLength(); i++) {
			if (childrenNode.item(i) instanceof Element) {
				hasElement = true;
				break;
			}
		}

		if (hasAttribute || hasElement) {
			JsonObject jsonObject = new JsonObject();
			for (int j = 0; j < namedNodeMap.getLength(); j++) {
				Attr attr = (Attr) namedNodeMap.item(j);
				jsonObject.add(attr.getName(),
						new JsonPrimitive(attr.getValue()));
			}
			if(!hasElement){
				String content = element.getTextContent();
				if (content != null && !content.trim().equals("")) {
					jsonObject.add("content", new JsonPrimitive(content));
				}
			}
			return jsonObject;
		} else {
			String content = element.getTextContent();
			return new JsonPrimitive(content);
		}
	}

}
