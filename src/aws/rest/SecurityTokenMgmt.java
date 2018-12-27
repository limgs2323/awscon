package aws.rest;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aws.model.TempSecurityCredentials;
import java.lang.RuntimeException;
/**
 * <pre>
 * 본 클래스는 AWS 임시자격증명을 관리하기 위한 클래스입니다.
 * </pre>
 *
 */
public class SecurityTokenMgmt {
	/**
	 * Logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(SecurityTokenMgmt.class);
	/**
	 * 임시자격증명 저장
	 */
	private static volatile HashMap<String, TempSecurityCredentials> mapToken;
	/**
	 * 자젹증명발급중인 리소스그룹 저장
	 */
	private static volatile List<String> penddingToken;
	/**
	 * 임시자격증명 리턴
	 * @param resGroupId 리소스그룹ID
	 * @return TempSecurityCredentials 임시자격증명 
	 */
	public static TempSecurityCredentials getCredentials(String resGroupId) {
		TempSecurityCredentials reObj = null;
		Calendar calExpire = Calendar.getInstance();
		Calendar calCurret = Calendar.getInstance();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		if( mapToken != null && mapToken.containsKey(resGroupId) ) {
			reObj = mapToken.get(resGroupId);
			try {
				calExpire.setTime(dateFormat.parse(reObj.getExpiration()));//2018-04-27T07:26:53Z
			} catch (ParseException e) {
				ExceptionUtils.getStackTrace(e); 
				return null;
			}
			calCurret.setTime(new Date());
			long remainTimeInMillis = calExpire.getTimeInMillis() - calCurret.getTimeInMillis();
			if( remainTimeInMillis < 5*60*1000 ) {//만료시간-현재시간 5분보다 작으면
				if( !penddingToken.contains(resGroupId) || remainTimeInMillis < 10*1000 ) {//새로운 토큰이 발급중이지도 않고 남은시간이 10초 보다 작으면
					reObj = null;
				}
			}
		}
		return reObj;
	}
	/**
	 * 저장된 임시자격증명이 없을때 임시자격증명을 발급받아서 리턴
	 * @param sAccessKeyId Access Key Id
	 * @param sSecretAccessKey Secret Access Key
	 * @param sRegion Region
	 * @param resGroupId 리소스그룹Id
	 * @param arn Arn
	 * @return TempSecurityCredentials 임시자격증명
	 * @throws Exception
	 */
	public static TempSecurityCredentials getCredentials(String sAccessKeyId, String sSecretAccessKey, String sRegion, String resGroupId, String arn) {
		try {
			TempSecurityCredentials reObj = null;
			if( mapToken == null ) {
				mapToken = new HashMap<String, TempSecurityCredentials>();
			}
			if( penddingToken == null ) {
				penddingToken = new ArrayList<String>();
			}
			if( penddingToken.contains(resGroupId) ) {
				int maxCnt = 20, cnt = 0;
				while( cnt < maxCnt ){//최대 20회 10초
					LOGGER.debug("AWS TempSecurityCredentials Pendding. Check "+cnt);
					Thread.sleep(500);//0.5초 sleep
					if( mapToken.containsKey(resGroupId) ) {
						reObj = mapToken.get(resGroupId);
						break;
					}
				}
				LOGGER.debug("AWS TempSecurityCredentials Pendding. Check End");
			} else {
				penddingToken.add(resGroupId);
				
				HashMap<String, String> mapParam = new HashMap<String, String>();
				mapParam.put("DurationSeconds", "3600");//1 hour
				mapParam.put("RoleSessionName", resGroupId);
				mapParam.put("RoleArn", arn);
				
				STSManager stsManager = new STSManager();
				String sResult = stsManager.assumeRole(sAccessKeyId, sSecretAccessKey, sRegion, mapParam);
				
				TempSecurityCredentials tsc = new TempSecurityCredentials();
				tsc.setRegion(sRegion);
				JSONObject json = XML.toJSONObject(sResult);
				JSONObject jCreateTagsResponse = json.getJSONObject("AssumeRoleResponse");
				JSONObject jAssumeRoleResult = jCreateTagsResponse.getJSONObject("AssumeRoleResult");
				JSONObject jAssumedRoleUser = jAssumeRoleResult.getJSONObject("AssumedRoleUser");
				tsc.setAssumedRoleId(jAssumedRoleUser.getString("AssumedRoleId"));
				tsc.setArn(jAssumedRoleUser.getString("Arn"));
				JSONObject jCredentials = jAssumeRoleResult.getJSONObject("Credentials");
				tsc.setSessionToken(jCredentials.getString("SessionToken"));
				tsc.setSecretAccessKey(jCredentials.getString("SecretAccessKey"));
				tsc.setAccessKeyId(jCredentials.getString("AccessKeyId"));
				tsc.setExpiration(jCredentials.getString("Expiration"));
				
				mapToken.put(resGroupId, tsc);
				reObj = tsc;
				penddingToken.remove(resGroupId);
			}
			return reObj;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
}
