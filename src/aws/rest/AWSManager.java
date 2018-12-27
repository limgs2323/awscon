package aws.rest;

import org.apache.commons.lang3.StringUtils;
/**
 * <pre>
 * 본 클래스는 AWS API 연결을 관리하기 위한 클래스입니다.
 * </pre>
 *
 */
public class AWSManager {
	/**
	 * 세션토큰 Header 명
	 */
	static final String cHEADERKEYSESSIONTOKEN = "x-amz-security-token";
	/**
	 * 기본 프로토콜
	 */
	String sProtocal = "https://";
	/**
	 * 연결 URI 만들기
	 * @param sign
	 * @return
	 */
	public String makeUri(AWSSignatureV4 sign) {
		StringBuilder sbUri = new StringBuilder();
		sbUri.append(sProtocal).append(sign.getHost()).append(sign.getCanonicalUri());
		if( "GET".equals(sign.getMethod()) ) {
			String sQueryParameter = sign.getConcatQueryParameter();
			if( StringUtils.isNotBlank(sQueryParameter) ){
				sbUri.append("?").append(sQueryParameter);
			}
		}
		return sbUri.toString();
	}
}
