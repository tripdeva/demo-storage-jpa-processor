package kr.co.demo.storage.jpa.processor.util;

/**
 * 네이밍 변환 유틸리티
 *
 * <p>camelCase ↔ snake_case 변환 및 문자열 대소문자 처리를 제공합니다.
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * NamingUtils.toSnakeCase("orderNumber");  // → "order_number"
 * NamingUtils.capitalize("order");          // → "Order"
 * NamingUtils.uncapitalize("Order");        // → "order"
 * }</pre>
 *
 * @author demo-framework
 * @since 1.0.0
 */
public final class NamingUtils {

	/**
	 * 인스턴스 생성 방지
	 */
	private NamingUtils() {
	}

	/**
	 * camelCase를 snake_case로 변환합니다.
	 *
	 * <p>대문자 앞에 언더스코어를 추가하고 전체를 소문자로 변환합니다.
	 *
	 * <pre>
	 * toSnakeCase("orderNumber")  → "order_number"
	 * toSnakeCase("OrderItem")    → "order_item"
	 * toSnakeCase("HTMLParser")   → "h_t_m_l_parser"
	 * toSnakeCase(null)           → null
	 * toSnakeCase("")             → ""
	 * </pre>
	 *
	 * @param camelCase 변환할 camelCase 문자열
	 * @return 변환된 snake_case 문자열, 입력이 null이면 null
	 */
	public static String toSnakeCase(String camelCase) {
		if (camelCase == null || camelCase.isEmpty()) {
			return camelCase;
		}
		return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
	}

	/**
	 * 첫 글자를 대문자로 변환합니다.
	 *
	 * <pre>
	 * capitalize("order")  → "Order"
	 * capitalize("Order")  → "Order"
	 * capitalize(null)     → null
	 * capitalize("")       → ""
	 * </pre>
	 *
	 * @param str 변환할 문자열
	 * @return 첫 글자가 대문자인 문자열, 입력이 null이면 null
	 */
	public static String capitalize(String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	/**
	 * 첫 글자를 소문자로 변환합니다.
	 *
	 * <pre>
	 * uncapitalize("Order")  → "order"
	 * uncapitalize("order")  → "order"
	 * uncapitalize(null)     → null
	 * uncapitalize("")       → ""
	 * </pre>
	 *
	 * @param str 변환할 문자열
	 * @return 첫 글자가 소문자인 문자열, 입력이 null이면 null
	 */
	public static String uncapitalize(String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		return str.substring(0, 1).toLowerCase() + str.substring(1);
	}
}
