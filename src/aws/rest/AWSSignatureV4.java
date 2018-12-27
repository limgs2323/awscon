package aws.rest;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.RuntimeException;;
/**
 * <pre>
 * 본 클래스는 AWS API 연결할때 필요한 Signature를 생성하는 클래스입니다.
 * </pre>
 *
 */
public class AWSSignatureV4 {
	private static final Logger LOGGER = LoggerFactory.getLogger(AWSSignatureV4.class);
	/* 상수 */
	private static final String HMACALGORITHM = "AWS4-HMAC-SHA256";
	private static final String AWS4REQUEST = "aws4_request";
	private static final String HEADERKEYCONTENTTYPE = "content-type";
	private static final String HEADERKEYHOST = "host";
	private static final String HEADERKEYAUTHORIZATION = "Authorization";
	/* 기본값 */
	private static final String DEFAULTMETHOD = "GET";
	private static final String DEFAULTCANONICALURI = "/";
	private static final String DEAFULTCONTEXTTYPE = "application/x-www-form-urlencoded; charset=utf-8";
	/* Mandatory variables */  
	private String accessKeyId;
	private String secretAccessKey;
	private String method;
	private String canonicalUri;
	private String region;
	private String service;
	private String contextType;
	private String host;
	private String payload;
	/* Other variables */
	private String strSignedHeader;
	private String xAmzDate;
	private String currentDate;
	private TreeMap<String, String> queryParametes;
	private TreeMap<String, String> reqHeaders;

	public AWSSignatureV4() {
		this.method = DEFAULTMETHOD;
		this.canonicalUri = DEFAULTCANONICALURI;
		this.contextType = DEAFULTCONTEXTTYPE;
		this.payload = "";
		this.xAmzDate = getUTCTime();
		this.currentDate = getUTCDate();
		this.reqHeaders = new TreeMap<String, String>();
		this.queryParametes = new TreeMap<String, String>();
	}

	public String getCanonicalUri() {
		return canonicalUri;
	}
	public void setCanonicalUri(String canonicalUri) {
		this.canonicalUri = canonicalUri;
	}
	public String getContextType() {
		return contextType;
	}
	public void setContextType(String contextType) {
		this.contextType = contextType;
	}
	public String getService() {
		return service;
	}
	public void setService(String service) {
		this.service = service;
	}
	public String getRegion() {
		return region;
	}
	public void setRegion(String region) {
		this.region = region;
	}
	public String getAccessKeyId() {
		return accessKeyId;
	}
	public void setAccessKeyId(String accessKeyId) {
		this.accessKeyId = accessKeyId;
	}
	public String getSecretAccessKey() {
		return secretAccessKey;
	}
	public void setSecretAccessKey(String secretAccessKey) {
		this.secretAccessKey = secretAccessKey;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getPayload() {
		return payload;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public void setPayload(String payload) {
		this.payload = payload;
	}

	public void addQueryParameter(String key, String value){
		queryParametes.put(key, value);
	}
	public void addReqHeader(String key, String value){
		reqHeaders.put(key, value);
	}
	public String getPayloadSha256Hex(){
		return sha256Hex(payload);
	}

	public String getConcatQueryParameter() {
		StringBuilder sbqr = new StringBuilder("");
		if (queryParametes != null && !queryParametes.isEmpty()) {
			for (Map.Entry<String, String> entrySet : queryParametes.entrySet()) {
				String key = entrySet.getKey();
				String value = entrySet.getValue();
				sbqr.append(key).append("=").append(encodeParameter(value)).append("&");
			}
			sbqr.deleteCharAt(sbqr.lastIndexOf("&"));
		}
		return sbqr.toString();
	}	
	
	public Map<String, String> getSignatureHeaders() {
		/* Signature용 헤더에 기본으로 context-type, host, x-amz-date 넣음*/
		if( !reqHeaders.containsKey(HEADERKEYCONTENTTYPE) ) {
			addReqHeader(HEADERKEYCONTENTTYPE, contextType);
		}
		if( !reqHeaders.containsKey(HEADERKEYHOST) ) {
			addReqHeader(HEADERKEYHOST, host);
		}
		addReqHeader("x-amz-date", xAmzDate);
		/* Task 1 */
		String sCanonicalURL = taskOne();
		/* Task 2 */
		String sToSign = taskTwo(sCanonicalURL);
		/* Task 3 */
		String sSignature = stepThree(sToSign);
		/* Task 4 */
		String sAuthorization = stepFour(sSignature);
		addReqHeader(HEADERKEYAUTHORIZATION, sAuthorization);
		return reqHeaders;
	}
	
	private String taskOne() {
		// ************* TASK 1: CREATE A CANONICAL REQUEST *************
		// http://docs.aws.amazon.com/general/latest/gr/sigv4-create-canonical-request.html
		StringBuilder sb = new StringBuilder("");

		// Step 1 is to define the verb (GET, POST, etc.)--already done.
		sb.append(method).append("\n");
		// Step 2: Create canonical URI--the part of the URI from domain to query
		// string (use '/' if no path)
		sb.append(canonicalUri).append("\n");
		// Step 3: Create the canonical query string. In this example (a GET request),
		// request parameters are in the query string. Query string values must
		// be URL-encoded (space=%20). The parameters must be sorted by name.
		// For this example, the query string is pre-formatted in the request_parameters variable.
		if( "GET".equals(method) ) {
			sb.append(getConcatQueryParameter()).append("\n");
		} else {//Post는 QyeryString을 만들지 않는다.
			sb.append("").append("\n");
		}
		// Step 4: Create the canonical headers and signed headers. Header names
		// must be trimmed and lowercase, and sorted in code point order from
		// low to high. Note that there is a trailing \n.
		//https://docs.aws.amazon.com/ko_kr/general/latest/gr/sigv4-create-canonical-request.html
		//헤더 이름을 소문자로 변환했습니다.
		//헤더를 문자 코드별로 정렬했습니다.
		//선행 공백과 후행 공백을 my-header1 및 my-header2 값에서 제거했습니다.
		//a b c의 순차적 공백을 my-header1 및 my-header2 값에 대해 단일 공백으로 변환했습니다.
		StringBuilder sbSignHeaderKey = new StringBuilder("");
		StringBuilder sbSignHeader = new StringBuilder("");
		if (reqHeaders != null && !reqHeaders.isEmpty()) {
			for (Map.Entry<String, String> entrySet : reqHeaders.entrySet()) {
				String key = entrySet.getKey();
				String value = entrySet.getValue();
				sbSignHeaderKey.append(key).append(";");
				sbSignHeader.append(key).append(":").append(value).append("\n");
			}
		}
		sb.append(sbSignHeader.toString()).append("\n");
		//String sCanonicalHeaders = "host:" + sHost + "\n" + "x-amz-date:" + sAmzDate + "\n";
		// Step 5: Create the list of signed headers. This lists the headers
		// in the canonical_headers list, delimited with ";" and in alpha order.
		// Note: The request can include any headers; canonical_headers and
		// signed_headers lists those that you want to be included in the
		// hash of the request. "Host" and "x-amz-date" are always required.
		strSignedHeader = sbSignHeaderKey.substring(0, sbSignHeaderKey.length() - 1); // Remove last ";"
		sb.append(strSignedHeader).append("\n");
		//String sSignedHeaders = "host;x-amz-date";
		// Step 6: Create payload hash (hash of the request body content). For GET
		// requests, the payload is an empty string ("").
		sb.append(sha256Hex(payload));
		LOGGER.debug("PAYLOAD"+"\n"+payload);
		// Step 7: Combine elements to create canonical request
		LOGGER.debug("====TASK 1:============================================================================"+"\n"+sb.toString());
		return sb.toString();
	}
/* step1 sample
GET
/
Action=ListUsers&Version=2010-05-08
content-type:application/x-www-form-urlencoded; charset=utf-8
host:iam.amazonaws.com
x-amz-date:20150830T123600Z

content-type;host;x-amz-date
e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
*/		
	private String taskTwo(String canonicalUri) {
		// ************* TASK 2: CREATE THE STRING TO SIGN*************
		// Match the algorithm to the hashing algorithm you use, either SHA-1 or
		// SHA-256 (recommended)
		StringBuilder sb = new StringBuilder();
		sb.append(HMACALGORITHM).append("\n");
		sb.append(xAmzDate).append("\n");
		sb.append(currentDate).append("/").append(region).append("/").append(service).append("/").append(AWS4REQUEST).append("\n");
		sb.append(sha256Hex(canonicalUri));
		LOGGER.debug("====TASK 2:============================================================================"+"\n"+sb.toString());
		return sb.toString();
	}
/* step2 sample
AWS4-HMAC-SHA256
20150830T123600Z
20150830/us-east-1/iam/aws4_request
f536975d06c0309214f805bb90ccff089219ecd68b2577efef23edd43b7e1a59
 */
	private String stepThree(String strToSign) {
		// ************* TASK 3: CALCULATE THE SIGNATURE *************
		// Create the signing key using the function defined above.
		byte[] barrSignatureKey = getSignatureKey(secretAccessKey, currentDate, region, service);

		// Sign the string_to_sign using the signing_key
		String sSignature = bytesToHex(hmacSHA256(barrSignatureKey, strToSign));
		LOGGER.debug("=====TASK 3:==========================================================================="+"\n"+sSignature);
		return sSignature;
	}
	
	private String stepFour(String strSignature) {
		// ************* TASK 4: ADD SIGNING INFORMATION TO THE REQUEST *************
		// The signing information can be either in a query string value or in
		// a header named Authorization. This code shows how to use a header.
		// Create authorization header and add to request headers
		StringBuilder sb = new StringBuilder();
		sb.append(HMACALGORITHM).append(" ");
		sb.append("Credential=").append(accessKeyId).append("/").append(currentDate).append("/").append(region).append("/").append(service).append("/").append(AWS4REQUEST).append(", ");
		sb.append("SignedHeaders=").append(strSignedHeader).append(", ");
		sb.append("Signature=").append(strSignature);
		LOGGER.debug("=====TASK 4:==========================================================================="+"\n"+sb.toString());
		return sb.toString();
	}
/* task4 sample
Authorization: AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20150830/us-east-1/iam/aws4_request, SignedHeaders=content-type;host;x-amz-date, Signature=5d672d79c15b13162d9279b0855cfba6789a8edb4c82c400e06b5924a6f2b5d7	
 */
	private String sha256Hex(String data) {
		return DigestUtils.sha256Hex(data);
		/* org.apache.commons.codec.digest.DigestUtils 못쓸때 java.security.MessageDigest 
		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance("SHA-256");
			messageDigest.update(data.getBytes("UTF-8"));
			byte[] digest = messageDigest.digest();
			return String.format("%064x", new java.math.BigInteger(1, digest));
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
		*/
	}
	private byte[] hmacSHA256(byte[] key, String data) {
		try {
			String algorithm = "HmacSHA256";
			Mac mac = Mac.getInstance(algorithm);
			mac.init(new SecretKeySpec(key, algorithm));
			return mac.doFinal(data.getBytes("UTF8"));
		} catch(Exception e) {
			LOGGER.error(ExceptionUtils.getStackTrace(e));
			throw new RuntimeException(e);
		}
	}
	private byte[] getSignatureKey(String key, String date, String regionName, String serviceName) {
		try {
			byte[] kSecret = ("AWS4" + key).getBytes("UTF8");
			byte[] kDate = hmacSHA256(kSecret, date);
			byte[] kRegion = hmacSHA256(kDate, regionName);
			byte[] kService = hmacSHA256(kRegion, serviceName);
			byte[] kSigning = hmacSHA256(kService, AWS4REQUEST);
			return kSigning;
		} catch(Exception e) {
			LOGGER.error(ExceptionUtils.getStackTrace(e));
			throw new RuntimeException(e);
		}
	}
	private String bytesToHex(byte[] bytes) {
		char[] hexArray = "0123456789ABCDEF".toCharArray();
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars).toLowerCase();
	}
	private String getUTCTime() {
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));//server timezone
		return dateFormat.format(new Date());
	}
	private String getUTCDate() {
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));//server timezone
		return dateFormat.format(new Date());
	}
	private String encodeParameter(String param) {
		try {
			return URLEncoder.encode(param, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOGGER.info(ExceptionUtils.getStackTrace(e));
			return URLEncoder.encode(param);
		}
	}
}