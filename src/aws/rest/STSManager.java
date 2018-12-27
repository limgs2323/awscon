package aws.rest;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.RuntimeException;
/**
 * <pre>
 * 본 클래스는 AWS STS API를 관리하는 클래스입니다.
 * </pre>
 *
 */
public class STSManager extends AWSManager{
	/**
	 * Logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(STSManager.class);
	/**
	 * AWS STS Service Name
	 */
	private String sService = "sts";
	/**
	 * AWS STS API Version
	 */
	private String sVersion = "2011-06-15";
	/**
	 * 임시자격증명 획득
	 * @param pAccessKeyId access key id
	 * @param pSecretAccessKey secret acces key
	 * @param pRegion region
	 * @param parameter 파라미터
	 * @return String Response body 
	 * @throws Exception
	 */
	public String assumeRole(String pAccessKeyId, String pSecretAccessKey, String pRegion, Map<String, String> parameter) {
		try {
			/*
			 * https://docs.aws.amazon.com/ko_kr/STS/latest/APIReference/API_AssumeRole.htmlcurrentDate
			 * https://docs.aws.amazon.com/ko_kr/IAM/latest/UserGuide/id_credentials_temp.html
			 * temp token 기본 15분 최대 1시간 유효기간을 가질수 있음
			 * RoleSessionName=임의키
			 * RoleArn=arn:aws:iam::999998888888:role/AllowApiDAPWASRole
			 * DurationSeconds=3600
			 */
			AWSSignatureV4 sign = new AWSSignatureV4();
			sign.setRegion(pRegion);
			sign.setService(sService);
			sign.setHost(String.format("%s.%s.amazonaws.com",sService, pRegion));
			sign.setAccessKeyId(pAccessKeyId);
			sign.setSecretAccessKey(pSecretAccessKey);
			sign.addQueryParameter("Action","AssumeRole");
			sign.addQueryParameter("Version",sVersion);
			for (Map.Entry<String, String> entrySet : parameter.entrySet()) {
				sign.addQueryParameter(entrySet.getKey(), entrySet.getValue());
			}
			
			Map<String, String> mapHeader = sign.getSignatureHeaders();
			
			CloseableHttpClient httpClient = HttpClients.createDefault();
			URI uri = new URI(makeUri(sign));
			
			HttpGet httpGet = new HttpGet();
			httpGet.setURI(uri);
			for (Map.Entry<String, String> entrySet : mapHeader.entrySet()) {
				httpGet.addHeader(entrySet.getKey(), entrySet.getValue());
			}
			
			CloseableHttpResponse response = httpClient.execute(httpGet);
			//int statusCode = response.getStatusLine().getStatusCode();
			InputStream inputStream = response.getEntity().getContent();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			StringBuilder sb = new StringBuilder();
			String line = "";
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
			response.close();
			httpClient.close();
			LOGGER.debug(sb.toString());
			return sb.toString();
		} catch(Exception e) {
			LOGGER.error(ExceptionUtils.getStackTrace(e));
			throw new RuntimeException(e);
		}
	}
}