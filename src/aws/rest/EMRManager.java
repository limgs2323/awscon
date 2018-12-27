package aws.rest;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aws.model.TempSecurityCredentials;
import java.lang.RuntimeException;

/**
 * <pre>
 * 본 클래스는 AWS EMR API를 관리하는 클래스입니다.
 * </pre>
 *
 */
public class EMRManager extends AWSManager{
	/**
	 * Logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(EMRManager.class);
	/**
	 * AWS EMR Service Name
	 */
	private String sService = "elasticmapreduce";
	/**
	 * AWS EMR API Version
	 */
	private String sVersion = "2009-03-31";
	/**
	 * EMR api call
	 * @param action EMR api action name
	 * @param credentials 임시자격증명
	 * @param parameter 파라미터
	 * @return String Response body 
	 * @throws Exception
	 */
	private String executeAction(String action, TempSecurityCredentials credentials, Map<String, String> parameter) {
		try {
			AWSSignatureV4 sign = new AWSSignatureV4();
			sign.setRegion(credentials.getRegion());
			sign.setService(sService);
			sign.setHost(String.format("%s.%s.amazonaws.com",sService,credentials.getRegion()));
			sign.setAccessKeyId(credentials.getAccessKeyId());
			sign.setSecretAccessKey(credentials.getSecretAccessKey());
			sign.addQueryParameter("Action",action);
			sign.addQueryParameter("Version",sVersion);
			if( parameter != null && !parameter.isEmpty() ) {
				for (Map.Entry<String, String> entrySet : parameter.entrySet()) {
					sign.addQueryParameter(entrySet.getKey(), entrySet.getValue());
				}
			}
			//임시자격증명 사용시 세팅
			if( StringUtils.isNotEmpty(credentials.getSessionToken()) ) {
				sign.addReqHeader(cHEADERKEYSESSIONTOKEN, credentials.getSessionToken());
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
	/**
	 * EMR 클러스터 목록 조회
	 * @param credentials 임시자격증명
	 * @return String response body
	 * @throws Exception
	 */
	public String listClusters(TempSecurityCredentials credentials) {
		//https://docs.aws.amazon.com/ko_kr/emr/latest/APIReference/API_ListClusters.html
		return executeAction("ListClusters", credentials, null);
	}
	/**
	 * EMR 클러스터내 인스턴스 목록 조회
	 * @param credentials 임시자격증명
	 * @param parameter 파라미터 
	 * @return String response body
	 * @throws Exception
	 */
	public String listInstances(TempSecurityCredentials credentials, Map<String, String> parameter) {
		//https://docs.aws.amazon.com/ko_kr/emr/latest/APIReference/API_ListInstances.html
		return executeAction("ListInstances", credentials, parameter);
	}
}
