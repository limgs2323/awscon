package aws.rest;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.Header;
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
 * 본 클래스는 AWS S3 API를 관리하는 클래스입니다.
 * </pre>
 *
 */
public class S3Manager extends AWSManager {
	/**
	 * Logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(S3Manager.class);
	/**
	 * AWS S3 Service Name
	 */
	private String sService = "s3";
	/**
	 * 사용량 레포트 /billing-lgcns-dap/257270354271-aws-billing-detailed-line-items-with-resources-and-tags-2018-04.csv.zip
	 */
	private String sUsageReport = "/billing-lgcns-dap/257270354271-aws-billing-detailed-line-items-with-resources-and-tags-%s.csv.zip";
	/**
	 * AWS S3 API Version
	 */
	//private String sVersion = "2006-03-01";
	/**
	 * S3 버킷 목록 조회
	 * @param credentials 임시자격증명
	 * @return response body
	 * @throws Exception
	 */
	public String listAllMyBuckets(TempSecurityCredentials credentials) {
		try {
			AWSSignatureV4 sign = new AWSSignatureV4();
			sign.setRegion(credentials.getRegion());
			sign.setService(sService);
			sign.setHost(String.format("%s.%s.amazonaws.com",sService, credentials.getRegion()));
			sign.setAccessKeyId(credentials.getAccessKeyId());
			sign.setSecretAccessKey(credentials.getSecretAccessKey());
	
			//임시자격증명 사용시 세팅
			if( StringUtils.isNotEmpty(credentials.getSessionToken()) ) {
				sign.addReqHeader(cHEADERKEYSESSIONTOKEN, credentials.getSessionToken());
			}
			sign.addReqHeader("x-amz-content-sha256", sign.getPayloadSha256Hex());
			
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
	 * S3 버킷 태그 조회
	 * @param credentials 임시자격증명
	 * @return response body
	 * @throws Exception
	 */	
	public String tagging(TempSecurityCredentials credentials, String bucketName){
		try {
			AWSSignatureV4 sign = new AWSSignatureV4();
			sign.setRegion(credentials.getRegion());
			sign.setService(sService);
			sign.setHost(String.format("%s.%s.amazonaws.com",sService, credentials.getRegion()));
			sign.setAccessKeyId(credentials.getAccessKeyId());
			sign.setSecretAccessKey(credentials.getSecretAccessKey());
			sign.setCanonicalUri("/"+bucketName);
			sign.addQueryParameter("tagging","");

			//임시자격증명 사용시 세팅
			if( StringUtils.isNotEmpty(credentials.getSessionToken()) ) {
				sign.addReqHeader(cHEADERKEYSESSIONTOKEN, credentials.getSessionToken());
			}
			sign.addReqHeader("x-amz-content-sha256", sign.getPayloadSha256Hex());
			
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
	 * S3 bucket에서 사용량 레포트 파일 가져오기
	 * @param credentials 임시자격증명
	 * @param yyyymm 년월
	 * @return 사용량 zip파일을 압축 풀어서 csv 파일을 읽어 문자열로 넘김
	 */
	public String getUsageReport(TempSecurityCredentials credentials, String yyyymm) {
		String sKey = String.format(sUsageReport,yyyymm);
		String sFileNm = sKey.substring(sKey.lastIndexOf("/")+1);
		
		AWSSignatureV4 sign = new AWSSignatureV4();
		sign.setRegion(credentials.getRegion());
		sign.setService(sService);
		sign.setHost(String.format("%s.%s.amazonaws.com",sService, credentials.getRegion()));
		sign.setAccessKeyId(credentials.getAccessKeyId());
		sign.setSecretAccessKey(credentials.getSecretAccessKey());
		sign.setCanonicalUri(sKey);//"/billing-lgcns-dap/257270354271-aws-billing-detailed-line-items-with-resources-and-tags-2018-04.csv.zip"

		//임시자격증명 사용시 세팅
		if( StringUtils.isNotEmpty(credentials.getSessionToken()) ) {
			sign.addReqHeader(cHEADERKEYSESSIONTOKEN, credentials.getSessionToken());
		}
		sign.addReqHeader("x-amz-content-sha256", sign.getPayloadSha256Hex());
		
		Map<String, String> mapHeader = sign.getSignatureHeaders();
		
		CloseableHttpClient httpClient = null; 
		CloseableHttpResponse response = null;
		String result = null;
		try {
			httpClient = HttpClients.createDefault();
			URI uri = new URI(makeUri(sign));
	
			HttpGet httpGet = new HttpGet();
			httpGet.setURI(uri);
			for (Map.Entry<String, String> entrySet : mapHeader.entrySet()) {
				httpGet.addHeader(entrySet.getKey(), entrySet.getValue());
			}
	
			response = httpClient.execute(httpGet);
			Header[] aryHeader = response.getAllHeaders();
			for (int i = 0; i < aryHeader.length; i++) {
				Header h = aryHeader[i];
				LOGGER.debug(h.getName()+"::"+h.getValue());
			}
			//int statusCode = response.getStatusLine().getStatusCode();
			InputStream inputStream = response.getEntity().getContent();
			BufferedInputStream in = new BufferedInputStream(inputStream);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[1024*100];//100kbyte
			int iread = 0;
			while( (iread = in.read(buf)) > 0 ) {
				out.write(buf, 0, iread);
			}
			out.flush();
			out.close();
			response.close();
			httpClient.close();
			
			ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
			ByteArrayOutputStream zipOut = new ByteArrayOutputStream();
			ZipEntry zEntry = null;
			while((zEntry = zipIn.getNextEntry()) != null){
				if( sFileNm.indexOf(zEntry.getName()) >= 0 ) {
					while( (iread = zipIn.read(buf)) > 0 ) {
						zipOut.write(buf, 0, iread);
					}
					zipOut.flush();
					zipOut.close();
				}
			}
			result = new String(zipOut.toByteArray());
		} catch(Exception e) {
			throw new RuntimeException(e);
		} finally {
			if( response != null ) { try { response.close(); } catch (IOException e) { LOGGER.error(ExceptionUtils.getStackTrace(e)); } }
			if( httpClient != null ) {  try { httpClient.close(); } catch (IOException e) { LOGGER.error(ExceptionUtils.getStackTrace(e)); }}
		}
		return result;
	}
	/*
	public void getObject(TempSecurityCredentials credentials, String key) throws Exception {
		AWSSignatureV4 sign = new AWSSignatureV4();
		sign.setRegion(credentials.getRegion());
		sign.setService(sService);
		sign.setHost(String.format("%s.%s.amazonaws.com",sService, credentials.getRegion()));
		sign.setAccessKeyId(credentials.getAccessKeyId());
		sign.setSecretAccessKey(credentials.getSecretAccessKey());
		sign.setCanonicalUri(key);//"/billing-lgcns-dap/257270354271-aws-billing-detailed-line-items-with-resources-and-tags-2018-04.csv.zip"

		//임시자격증명 사용시 세팅
		if( StringUtils.isNotEmpty(credentials.getSessionToken()) ) {
			sign.addReqHeader(HEADERKEYSESSIONTOKEN, credentials.getSessionToken());
		}
		sign.addReqHeader("x-amz-content-sha256", sign.getPayloadSha256Hex());
		
		Map<String, String> mapHeader = sign.getSignatureHeaders();

		CloseableHttpClient httpClient = HttpClients.createDefault();
		URI uri = new URI(makeUri(sign));

		HttpGet httpGet = new HttpGet();
		httpGet.setURI(uri);
		for (Map.Entry<String, String> entrySet : mapHeader.entrySet()) {
			httpGet.addHeader(entrySet.getKey(), entrySet.getValue());
		}

		CloseableHttpResponse response = httpClient.execute(httpGet);
		Header[] aryHeader = response.getAllHeaders();
		for (int i = 0; i < aryHeader.length; i++) {
			Header h = aryHeader[i];
			LOGGER.debug(h.getName()+"::"+h.getValue());
		}
		//int statusCode = response.getStatusLine().getStatusCode();
		InputStream inputStream = response.getEntity().getContent();
		
		byte[] buffer = new byte[10240];
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File("D:\\temp\\"+key)));
		BufferedInputStream in = new BufferedInputStream(inputStream);
		int readlen = 0;
		while ((readlen = in.read(buffer)) > 0) {
			out.write(buffer, 0, readlen);
		}
		out.flush();
		out.close();
		in.close();

		response.close();
		httpClient.close();
		LOGGER.debug("getObject end::"+key);
	}
	
	public String listBuckets(TempSecurityCredentials credentials) throws Exception {
		AWSSignatureV4 sign = new AWSSignatureV4();
		sign.setRegion(credentials.getRegion());
		sign.setService(sService);
		sign.setHost(String.format("%s.%s.amazonaws.com",sService, credentials.getRegion()));
		sign.setAccessKeyId(credentials.getAccessKeyId());
		sign.setSecretAccessKey(credentials.getSecretAccessKey());
		sign.setCanonicalUri("/billing-lgcns-dap");
		sign.addQueryParameter("list-type","2");

		//임시자격증명 사용시 세팅
		if( StringUtils.isNotEmpty(credentials.getSessionToken()) ) {
			sign.addReqHeader(HEADERKEYSESSIONTOKEN, credentials.getSessionToken());
		}
		sign.addReqHeader("x-amz-content-sha256", sign.getPayloadSha256Hex());
		
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
	}
	*/
	
}
