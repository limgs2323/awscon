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
/***
 * <pre>
 * 본 클래스는 AWS EC2 API를 관리하는 클래스입니다.
 * </pre>
 * 
 */
public class EC2Manager extends AWSManager{
	/**
	 * Logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(EC2Manager.class);
	/**
	 * AWS EC2 Service Name
	 */
	private String sService = "ec2";
	/**
	 * AWS EC2 API Version
	 */
	private String sVersion = "2016-11-15";
	/**
	 * EC2 api call
	 * @param action EC2 api action name
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
	/* 미사용
	 * 네트워크 인터페이스 정보 조회
	 * @param credentials 임시자격증명
	 * @return String response body
	 * @throws Exception
	 *
	public String describeNetworkInterfaces(TempSecurityCredentials credentials) {
		//https://docs.aws.amazon.com/ko_kr/AWSEC2/latest/APIReference/API_DescribeNetworkInterfaces.html
		return executeAction("DescribeNetworkInterfaces", credentials, null);
	}*/
	/**
	 * 모든 인스턴스 정보 조회
	 * @param credentials 임시자격증명
	 * @return String Response body
	 * @throws Exception
	 */
	public String describeInstances(TempSecurityCredentials credentials) { 
		//https://docs.aws.amazon.com/ko_kr/AWSEC2/latest/APIReference/API_DescribeInstances.html
		return executeAction("DescribeInstances", credentials, null);
	}
	/**
	 * 인스턴스 상태 조회
	 * @param credentials 임시자격증명
	 * @param parameter 파라미터 
	 * @return String Response body
	 * @throws Exception
	 */
	public String describeInstanceStatus(TempSecurityCredentials credentials, Map<String, String> parameter) {
		//https://docs.aws.amazon.com/ko_kr/AWSEC2/latest/APIReference/API_DescribeInstanceStatus.html
		return executeAction("DescribeInstanceStatus", credentials, parameter);
	}	
	/**
	 * 인스턴스 start
	 * @param credentials 임시자격증명
	 * @param parameter 파라미터
	 * @return String Response body
	 * @throws Exception
	 */
	public String startInstances(TempSecurityCredentials credentials, Map<String, String> parameter) {
		//https://docs.aws.amazon.com/ko_kr/AWSEC2/latest/APIReference/API_StartInstances.html
		return executeAction("StartInstances", credentials, parameter);
	}
	/**
	 * 인스턴스 stop
	 * @param credentials 임시자격증명
	 * @param parameter 파라미터
	 * @return String Response body
	 * @throws Exception
	 */
	public String stopInstances(TempSecurityCredentials credentials, Map<String, String> parameter) {
		//https://docs.aws.amazon.com/ko_kr/AWSEC2/latest/APIReference/API_StopInstances.html
		return executeAction("StopInstances", credentials, parameter);
	}
	/**
	 * EC2 instance 삭제
	 * @param credentials 임시자격증명 
	 * @param parameter 파라미터
	 * @return String Response body
	 * @throws Exception
	 */
	public String terminateInstances(TempSecurityCredentials credentials, Map<String, String> parameter) {
		//https://docs.aws.amazon.com/ko_kr/AWSEC2/latest/APIReference/API_TerminateInstances.html
		return executeAction("TerminateInstances", credentials, parameter);
	}
	/*
	 * EC2 Resource에 tag 생성
	 * @param credentials 임시자격증명
	 * @param parameter 파라미터
	 * @return String Response body
	 * @throws Exception
	public String createTags(TempSecurityCredentials credentials, Map<String, String> parameter) {
		//https://docs.aws.amazon.com/ko_kr/AWSEC2/latest/APIReference/API_CreateTags.html
		return executeAction("CreateTags", credentials, parameter);
	}
	*/	
}