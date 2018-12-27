package aws.rest;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aws.model.TempSecurityCredentials;
import java.lang.RuntimeException;
/**
 * <pre>
 * 본 클래스는 AWS CloueWatch API를 관리하는 클래스입니다.
 * </pre>
 *
 */
public class CloudWatchManager extends AWSManager {
	/**
	 * Logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(CloudWatchManager.class);
	/**
	 * AWS CloudWatch Service Name
	 */
	private String sService = "monitoring";
	/**
	 * AWS CloudWatch API Version
	 */
	private String sVersion = "2010-08-01";	
	/**
	 * 단건 통계 조회
	 * @param credentials 임시자격증명
	 * @param parameter 파라미터
	 * @return String Response body 
	 * @throws Exception
	 */
	public String getMetricStatistics(TempSecurityCredentials credentials, Map<String, String> parameter) {
		try {
			//https://docs.aws.amazon.com/ko_kr/AmazonCloudWatch/latest/APIReference/API_GetMetricStatistics.html
			AWSSignatureV4 sign = new AWSSignatureV4();
			sign.setMethod("POST");
			//sign.setContextType("application/x-amz-json-1.0");
			
			sign.setRegion(credentials.getRegion());
			sign.setService(sService);
			sign.setHost(String.format("%s.%s.amazonaws.com",sService,credentials.getRegion()));
			sign.setAccessKeyId(credentials.getAccessKeyId());
			sign.setSecretAccessKey(credentials.getSecretAccessKey());
			sign.addQueryParameter("Action","GetMetricStatistics");
			sign.addQueryParameter("Version",sVersion);
			for (Map.Entry<String, String> entrySet : parameter.entrySet()) {
				sign.addQueryParameter(entrySet.getKey(), entrySet.getValue());
			}
			sign.setPayload(sign.getConcatQueryParameter());
			//임시자격증명 사용시 세팅
			if( StringUtils.isNotEmpty(credentials.getSessionToken()) ) {
				sign.addReqHeader(cHEADERKEYSESSIONTOKEN, credentials.getSessionToken());
			}
			
			Map<String, String> mapHeader = sign.getSignatureHeaders();
			
			CloseableHttpClient httpClient = HttpClients.createDefault();
			URI uri = new URI(makeUri(sign));
			
			HttpPost httpPost = new HttpPost();
			httpPost.setURI(uri);
			StringEntity entity = new StringEntity(sign.getPayload());
			httpPost.setEntity(entity);
			for (Map.Entry<String, String> entrySet : mapHeader.entrySet()) {
				httpPost.addHeader(entrySet.getKey(), entrySet.getValue());
			}
			
			CloseableHttpResponse response = httpClient.execute(httpPost);
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
	 * 다건 통계 조회
	 * @param credentials 임시자격증명
	 * @param parameter 파라미터
	 * @return String Response body 
	 * @throws Exception
	 */
	public String getMetricData(TempSecurityCredentials credentials, Map<String, String> parameter) {
		try {
			//https://docs.aws.amazon.com/ko_kr/AmazonCloudWatch/latest/APIReference/API_GetMetricStatistics.html
			AWSSignatureV4 sign = new AWSSignatureV4();
			sign.setMethod("POST");
			//sign.setContextType("application/x-amz-json-1.0");
			
			sign.setRegion(credentials.getRegion());
			sign.setService(sService);
			sign.setHost(String.format("%s.%s.amazonaws.com",sService,credentials.getRegion()));
			sign.setAccessKeyId(credentials.getAccessKeyId());
			sign.setSecretAccessKey(credentials.getSecretAccessKey());
			sign.addQueryParameter("Action","GetMetricData");
			sign.addQueryParameter("Version",sVersion);
			for (Map.Entry<String, String> entrySet : parameter.entrySet()) {
				sign.addQueryParameter(entrySet.getKey(), entrySet.getValue());
			}
			sign.setPayload(sign.getConcatQueryParameter());
			//임시자격증명 사용시 세팅
			if( StringUtils.isNotEmpty(credentials.getSessionToken()) ) {
				sign.addReqHeader(cHEADERKEYSESSIONTOKEN, credentials.getSessionToken());
			}
			
			Map<String, String> mapHeader = sign.getSignatureHeaders();
			
			CloseableHttpClient httpClient = HttpClients.createDefault();
			URI uri = new URI(makeUri(sign));
			
			HttpPost httpPost = new HttpPost();
			httpPost.setURI(uri);
			StringEntity entity = new StringEntity(sign.getPayload());
			httpPost.setEntity(entity);
			for (Map.Entry<String, String> entrySet : mapHeader.entrySet()) {
				httpPost.addHeader(entrySet.getKey(), entrySet.getValue());
			}
			
			CloseableHttpResponse response = httpClient.execute(httpPost);
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
