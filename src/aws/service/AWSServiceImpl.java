package aws.service;

import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import javax.annotation.Resource;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import aws.model.AWSResource;
import aws.model.AWSUsage;
import aws.model.TempSecurityCredentials;
import aws.rest.CloudWatchManager;
import aws.rest.EC2Manager;
import aws.rest.EMRManager;
import aws.rest.S3Manager;
import aws.rest.SecurityTokenMgmt;
import java.lang.RuntimeException;

/**
 * AWS API Call
 *
 */
@Service("awsService")
public class AWSServiceImpl implements AWSService {
	/**
	 * Logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(AWSServiceImpl.class);
	/**
	 * virtualMachines
	 */
	private static final String VIRTUALMACHINES = "virtualMachines";
	/**
	 * accessKeyId
	 */
	@Value("${aws.accesskeyid}")
	private String accessKeyId;
	/**
	 * secretAccesskey
	 */
	@Value("${aws.secretaccesskey}")
	private String secretAccessKey;
	/**
	 * 공통 DAO관련 리소스 클래스
	 */
	@Resource
	private CommonDao commonDao;
	/**
	 * 임시자격증명 가져오기
	 * @param resGroupId 리소스그룹ID
	 * @return TempSecurityCredentials 임시자격증명
	 */
	private TempSecurityCredentials getCredentials(String resGroupId) {
		try {
			TempSecurityCredentials credentials = SecurityTokenMgmt.getCredentials(resGroupId);
			if( credentials == null ) {
				CreateManualPlt queryPraramVo = new CreateManualPlt();
				queryPraramVo.setResGroupId(resGroupId); 
				CreateManualPlt createManualPlt = commonDao.select("sportal.bds.service.aws.retrieveArn", queryPraramVo);
				String sRegion = createManualPlt.getRegion();
				String sArn = createManualPlt.getArn();
				credentials = SecurityTokenMgmt.getCredentials(accessKeyId, secretAccessKey, sRegion, resGroupId, sArn);//"arn:aws:iam::999998888888:role/AllowApiDAPWASRole"
			}
			return credentials;
		} catch (Exception e) {
			LOGGER.error(ExceptionUtils.getStackTrace(e));
			throw new RuntimeException(e);
		}
	}
	/**
	 * api 호출결과 Response body에 오류 응답이 왔는지 확인 
	 * @param sResult response body
	 * @throws RuntimeException
	 */
	private void checkError(String sResult) {
		if( sResult != null && !"".equals(sResult) ) {
			if( sResult.toLowerCase().indexOf("<error>") >= 0 ) {//Error
				String sCode = "", sMesg = "";
				if( sResult.indexOf("<Code>") >= 0 ) {
					sCode = sResult.substring(sResult.indexOf("<Code>")+"<Code>".length(), sResult.indexOf("</Code>"));
				}
				if( sResult.indexOf("<Message>") >= 0 ) {
					sMesg = sResult.substring(sResult.indexOf("<Message>")+"<Message>".length(), sResult.indexOf("</Message>"));
				}
				throw new RuntimeException(sCode, sMesg);
			}
		} else {
			throw new RuntimeException("Http Response body empty");
		}
	}
	/**
	 * 인스턴스 시작
	 * @param resGroupId 리소스그룹Id
	 * @param instanceId 리소스명(
	 */
	@Override
	public void startInstances(String resGroupId, String instanceId) {
		TempSecurityCredentials credentials = getCredentials(resGroupId);
		HashMap<String, String> mapParam = new HashMap<String, String>();
		mapParam.put("InstanceId.1", instanceId);

		EC2Manager ec2Manager = new EC2Manager();
		String sResult = ec2Manager.startInstances(credentials, mapParam);
		checkError(sResult);

		JSONObject json = XML.toJSONObject(sResult);
		JSONObject jStartInstancesResponse = json.getJSONObject("StartInstancesResponse");
		JSONObject jInstancesSet = jStartInstancesResponse.getJSONObject("instancesSet");
		JSONArray jaItem = jInstancesSet.optJSONArray("item");
		if( jaItem == null ) {
			jaItem = new JSONArray();
			jaItem.put(jInstancesSet.getJSONObject("item"));
		}
		for (int i = 0; i < jaItem.length(); i++) {
			JSONObject jItem = jaItem.getJSONObject(i);
			String sInstanceId = jItem.getString("instanceId");
			JSONObject jCurrentState = jItem.getJSONObject("currentState");
			//int iCurrStataCode = jCurrentState.getInt("code");
			String sCurrStateName = jCurrentState.getString("name");
			JSONObject jPreviousState = jItem.getJSONObject("previousState");
			//int iPrevStataCode = jPreviousState.getInt("code");
			String sPrevStateName = jPreviousState.getString("name");
			LOGGER.debug(new StringBuilder().append("StartInstances ").append(sInstanceId).append(" current state=").append(sCurrStateName).append(" previous state=").append(sPrevStateName).toString());
		}
	}
	/**
	 * 인스턴스 중지
	 * @param resGroupId 리소스그룹Id
	 * @param instanceId 리소스명(
	 */
	@Override
	public void stopInstances(String resGroupId, String instanceId) {
		TempSecurityCredentials credentials = getCredentials(resGroupId);
		HashMap<String, String> mapParam = new HashMap<String, String>();
		mapParam.put("InstanceId.1", instanceId);
		
		EC2Manager ec2Manager = new EC2Manager();
		String sResult = ec2Manager.stopInstances(credentials, mapParam);
		checkError(sResult);
		
		JSONObject json = XML.toJSONObject(sResult);
		JSONObject jStopInstancesResponse = json.getJSONObject("StopInstancesResponse");
		JSONObject jInstancesSet = jStopInstancesResponse.getJSONObject("instancesSet");
		JSONArray jaItem = jInstancesSet.optJSONArray("item");
		if( jaItem == null ) {
			jaItem = new JSONArray();
			jaItem.put(jInstancesSet.getJSONObject("item"));
		}
		for (int i = 0; i < jaItem.length(); i++) {
			JSONObject jItem = jaItem.getJSONObject(i);
			String sInstanceId = jItem.getString("instanceId");
			JSONObject jCurrentState = jItem.getJSONObject("currentState");
			//int iCurrStataCode = jCurrentState.getInt("code");
			String sCurrStateName = jCurrentState.getString("name");
			JSONObject jPreviousState = jItem.getJSONObject("previousState");
			//int iPrevStataCode = jPreviousState.getInt("code");
			String sPrevStateName = jPreviousState.getString("name");
			LOGGER.debug(new StringBuilder().append("stopInstances ").append(sInstanceId).append(" current state=").append(sCurrStateName).append(" previous state=").append(sPrevStateName).toString());
		}
	}
	/**
	 * 인스턴스 종료
	 * @param resGroupId 리소스그룹Id
	 * @param instanceId 리소스명(
	 */
	@Override
	public void terminateInstances(String resGroupId, String instanceId) {
		TempSecurityCredentials credentials = getCredentials(resGroupId);
		HashMap<String, String> mapParam = new HashMap<String, String>();
		mapParam.put("InstanceId.1", instanceId);
		
		EC2Manager ec2Manager = new EC2Manager();
		String sResult = ec2Manager.terminateInstances(credentials, mapParam);
		checkError(sResult);
		
		JSONObject json = XML.toJSONObject(sResult);
		JSONObject jStopInstancesResponse = json.getJSONObject("TerminateInstancesResponse");
		JSONObject jInstancesSet = jStopInstancesResponse.getJSONObject("instancesSet");
		JSONArray jaItem = jInstancesSet.optJSONArray("item");
		if( jaItem == null ) {
			jaItem = new JSONArray();
			jaItem.put(jInstancesSet.getJSONObject("item"));
		}
		for (int i = 0; i < jaItem.length(); i++) {
			JSONObject jItem = jaItem.getJSONObject(i);
			String sInstanceId = jItem.getString("instanceId");
			JSONObject jCurrentState = jItem.getJSONObject("currentState");
			//int iCurrStataCode = jCurrentState.getInt("code");
			String sCurrStateName = jCurrentState.getString("name");
			JSONObject jPreviousState = jItem.getJSONObject("previousState");
			//int iPrevStataCode = jPreviousState.getInt("code");
			String sPrevStateName = jPreviousState.getString("name");
			LOGGER.debug(new StringBuilder().append("TerminateInstances ").append(sInstanceId).append(" current state=").append(sCurrStateName).append(" previous state=").append(sPrevStateName).toString());
		}
	}
	/**
	 * 인스턴스 종료
	 * @param resGroupId 리소스그룹Id
	 * @param instanceId 리소스명(
	 */
	@Override
	public void terminateInstances(String resGroupId, List<String> instanceIdList) {
		TempSecurityCredentials credentials = getCredentials(resGroupId);
		HashMap<String, String> mapParam = new HashMap<String, String>();
		int n = 1;
		for( String instanceId : instanceIdList ) {
			mapParam.put("InstanceId."+n, instanceId);
			n++;
		}
		
		EC2Manager ec2Manager = new EC2Manager();
		String sResult = ec2Manager.terminateInstances(credentials, mapParam);
		checkError(sResult);
		
		JSONObject json = XML.toJSONObject(sResult);
		JSONObject jStopInstancesResponse = json.getJSONObject("TerminateInstancesResponse");
		JSONObject jInstancesSet = jStopInstancesResponse.getJSONObject("instancesSet");
		JSONArray jaItem = jInstancesSet.optJSONArray("item");
		if( jaItem == null ) {
			jaItem = new JSONArray();
			jaItem.put(jInstancesSet.getJSONObject("item"));
		}
		for (int i = 0; i < jaItem.length(); i++) {
			JSONObject jItem = jaItem.getJSONObject(i);
			String sInstanceId = jItem.getString("instanceId");
			JSONObject jCurrentState = jItem.getJSONObject("currentState");
			//int iCurrStataCode = jCurrentState.getInt("code");
			String sCurrStateName = jCurrentState.getString("name");
			JSONObject jPreviousState = jItem.getJSONObject("previousState");
			//int iPrevStataCode = jPreviousState.getInt("code");
			String sPrevStateName = jPreviousState.getString("name");
			LOGGER.debug(new StringBuilder().append("TerminateInstances ").append(sInstanceId).append(" current state=").append(sCurrStateName).append(" previous state=").append(sPrevStateName).toString());
		}
	}
	/**
	 * 해당 리소스그룹이 있는 계정에 인스턴스 정보 조회
	 * @param resGroupId 리소스그룹ID
	 * @return List<AWSResource> 인스턴스 정보 목록
	 */
	@Override
	public List<AWSResource> describeInstances(String resGroupId) {
		ArrayList<AWSResource> list = new ArrayList<AWSResource>();
		
		TempSecurityCredentials credentials = getCredentials(resGroupId);
		
		EC2Manager ec2Manager = new EC2Manager();
		String sResult = ec2Manager.describeInstances(credentials);
		checkError(sResult);
		
		JSONObject json = XML.toJSONObject(sResult);
		JSONObject jReservationSet = json.getJSONObject("DescribeInstancesResponse").optJSONObject("reservationSet");
		if( jReservationSet == null ) {//인스턴스가 없음
			return list;
		}
		JSONArray jaItem = jReservationSet.optJSONArray("item");
		if( jaItem == null ) {
			jaItem = new JSONArray();
			jaItem.put(jReservationSet.getJSONObject("item"));
		}
		String sInstanceId = "";
		AWSResource r;
		for (int i = 0; i < jaItem.length(); i++) {
			JSONObject jItem = jaItem.getJSONObject(i);
			//LOGGER.debug("\n"+i+"::"+jItem.toString());
			JSONObject jInstancesSet = jItem.getJSONObject("instancesSet");
			
			JSONArray jaInstancesItems = jInstancesSet.optJSONArray("item");
			if (jaInstancesItems == null) {
				jaInstancesItems = new JSONArray();
				jaInstancesItems.put(jInstancesSet.getJSONObject("item"));
			}
			for (int j = 0; j < jaInstancesItems.length(); j++) {
				JSONObject jInstancesItem = jaInstancesItems.getJSONObject(j);
				LOGGER.debug(jInstancesItem.toString());
				r = new AWSResource();
				sInstanceId = jInstancesItem.getString("instanceId");
				r.setResourceType(VIRTUALMACHINES);//instances - virtualMachines
				r.setResourceId(sInstanceId);
				r.setPrivateIpAddress(jInstancesItem.optString("privateIpAddress",""));
				r.setInstanceType(jInstancesItem.getString("instanceType"));
				r.setArchitecture(jInstancesItem.getString("architecture"));
				r.setPlatform(!jInstancesItem.isNull("platform") ? jInstancesItem.getString("platform") : "");
				JSONObject jTagSet = jInstancesItem.optJSONObject("tagSet");
				if( jTagSet != null ) {
					JSONArray jaTagSetItem = jTagSet.optJSONArray("item");
					if (jaTagSetItem == null) {
						jaTagSetItem = new JSONArray().put(jTagSet.getJSONObject("item"));
					}
					for (int k = 0; k < jaTagSetItem.length(); k++) {
						JSONObject jTagSetItem = jaTagSetItem.getJSONObject(k);
						if( "Name".equals(jTagSetItem.getString("key")) ) {
							r.setName(jTagSetItem.get("value").toString());
						}
						r.addTag(jTagSetItem.getString("key"), jTagSetItem.get("value").toString());
					}
				}
				list.add(r);
				if( jInstancesItem.optJSONObject("networkInterfaceSet") != null ) { 
					JSONObject jAssociation = jInstancesItem.getJSONObject("networkInterfaceSet").getJSONObject("item").optJSONObject("association");
					if( jAssociation != null ) {
						//publicIp Resource 만들기 전에 instance에 publicip를 붙인다. 
						r.setPublicIpAddress(jAssociation.getString("publicIp"));
						r = new AWSResource();
						r.setResourceType("publicIPAddresses");
						r.setResourceId(jInstancesItem.getJSONObject("networkInterfaceSet").getJSONObject("item").getString("networkInterfaceId"));
						r.setPrivateIpAddress(jAssociation.getString("publicIp"));
						r.setAttachInstanceId(sInstanceId);
						list.add(r);
					}
				}
				JSONObject jBlockDeviceMapping = jInstancesItem.optJSONObject("blockDeviceMapping");
				if( jBlockDeviceMapping != null ) {
					JSONArray jaBlockDeviceMappingItems = jBlockDeviceMapping.optJSONArray("item");
					if (jaBlockDeviceMappingItems == null) {
						jaBlockDeviceMappingItems = new JSONArray();
						jaBlockDeviceMappingItems.put(jBlockDeviceMapping.getJSONObject("item"));
					}
					for (int k = 0; k < jaBlockDeviceMappingItems.length(); k++) {
						JSONObject jBlockDeviceMappingItem = jaBlockDeviceMappingItems.getJSONObject(k);
						JSONObject jEbs = jBlockDeviceMappingItem.getJSONObject("ebs");
						r = new AWSResource();
						r.setResourceType("volumn");
						r.setResourceId(jEbs.getString("volumeId"));
						r.setAttachInstanceId(sInstanceId);
						list.add(r);
					}
				}
			}
		}
		for (int i = 0; i < list.size(); i++) {
			LOGGER.debug(i+"::"+list.get(i).toString());
		}
		return list;
	}
	/**
	 * EMR 클러스터 목록 조회
	 * @param resGroupId 리소스그룹ID
	 * @return List<AWSResource> EMR 클러스터 목록
	 */
	@Override
	public List<AWSResource> listClusters(String resGroupId) {
		ArrayList<AWSResource> list = new ArrayList<AWSResource>();
		
		TempSecurityCredentials credentials = getCredentials(resGroupId);
		
		EMRManager emrManager = new EMRManager();
		String sResult = emrManager.listClusters(credentials);
		checkError(sResult);
		
		JSONObject json = XML.toJSONObject(sResult);
		
		JSONObject jClusters = json.getJSONObject("ListClustersResponse").getJSONObject("ListClustersResult").optJSONObject("Clusters");
		if( jClusters != null ) {
			JSONArray jaMember = jClusters.optJSONArray("member");
			if( jaMember == null ) {
				jaMember = new JSONArray();
				jaMember.put(jClusters.optJSONObject("member"));
			}
			AWSResource r = null;
			for (int i = 0; i < jaMember.length(); i++) {
				JSONObject jMember = jaMember.getJSONObject(i);
				r = new AWSResource();
				r.setResourceType("EMR::cluster");
				r.setResourceId(jMember.getString("Id"));
				r.setClusterState(jMember.getJSONObject("Status").getString("State"));
				r.addTag("Name",jMember.getString("Name"));
				list.add(r);
			}
			for (int i = 0; i < list.size(); i++) {
				LOGGER.debug(i+"::"+list.get(i).toString());
			}
		}
		return list;
	}
	/**
	 * EMR 클러스터에 인스턴스 목록 조회
	 * @param resGroupId 리소스그룹ID
	 * @param resourceId 리소스Id = EMR 클러스터 Id
	 * @return List<AWSResource> EMR 클러스터에 인스턴스 목록
	 */
	@Override
	public List<AWSResource> listInstances(String resGroupId, String resourceId) {
		ArrayList<AWSResource> list = new ArrayList<AWSResource>();
		
		TempSecurityCredentials credentials = getCredentials(resGroupId);
		HashMap<String, String> mapParam = new HashMap<String, String>();
		mapParam.put("ClusterId", resourceId);
		
		EMRManager emrManager = new EMRManager();
		String sResult = emrManager.listInstances(credentials, mapParam);
		checkError(sResult);
		
		JSONObject json = XML.toJSONObject(sResult);
		
		JSONObject jInstances = json.getJSONObject("ListInstancesResponse").getJSONObject("ListInstancesResult").optJSONObject("Instances");
		if( jInstances != null ) {
			JSONArray jaMember = jInstances.optJSONArray("member");
			if( jaMember == null ) {
				jaMember = new JSONArray();
				jaMember.put(jInstances.optJSONObject("member"));
			}
			AWSResource r = null;
			for (int i = 0; i < jaMember.length(); i++) {
				JSONObject jMember = jaMember.getJSONObject(i);
				r = new AWSResource();
				r.setResourceType("EMR Cluster instances");//instances - virtualMachines
				r.setResourceId(jMember.getString("Ec2InstanceId"));
				r.setClusterId(resourceId);
				list.add(r);
			}
		}	
		for (int i = 0; i < list.size(); i++) {
			LOGGER.debug(i+"::"+list.get(i).toString());
		}
		return list;
	}	
	/**
	 * 리소스에 tag 생성
	 * @param resGroupId 리소스그룹Id
	 * @param solutionReqId 솔루션요청Id
	 * @param resourceId 리소스Id
	@Override
	public void createTags(String resGroupId, String solutionReqId, String resourceId) {
		TempSecurityCredentials credentials = getCredentials(resGroupId);
		HashMap<String, String> mapParam = new HashMap<String, String>();
		mapParam.put("ResourceId.1", resourceId);
		mapParam.put("ResourceId.2", resourceId);
		mapParam.put("Tag.1.Key", "rg");
		mapParam.put("Tag.1.Value", resGroupId);
		mapParam.put("Tag.2.Key", "solution");
		mapParam.put("Tag.2.Value", solutionReqId);
		
		EC2Manager ec2Manager = new EC2Manager();
		String sResult = ec2Manager.createTags(credentials, mapParam);
		LOGGER.debug(sResult);
		
		JSONObject json = XML.toJSONObject(sResult);
		JSONObject jCreateTagsResponse = json.getJSONObject("CreateTagsResponse");
		boolean sReturn = jCreateTagsResponse.getBoolean("return");//true
		LOGGER.debug(Boolean.toString(sReturn));
	}	 */	
	/**
	 * 모니터랑 데이터 조회 getMetricData api 호출은 50/second tps && datapoint 제한이 있음
	 * @param resGroupId 리소스그룹Id
	 * @param instanceIdList 인스턴스Id들
	 * @return Monitoring 모니터링 데이터
	 */
	@Override
	public Monitoring getMetricData(String resGroupId, List<String> instanceIdList) {
		//100개 지표 조회 가능 19개 vm이면 100개됨
		String[] aryMetric = {"CPUUtilization", "NetworkIn", "NetworkOut", "DiskReadBytes","DiskWriteBytes"};
		String[] aryStat   = {"Average","Sum","Sum","Sum","Sum"};
		ArrayList<Metric> cpu = null, netIn = null, netOut = null, diskRead = null, diskWrite = null;
		DateFormat localDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		localDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+9"));//local time timezone
		int n = 1;
		
		TempSecurityCredentials credentials = getCredentials(resGroupId);
		HashMap<String, String> mapParam = null;
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));//server timezone
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE,1);//로컬타임 0시로 맞추기 위해서
		cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE), 0, 0, 0);//
		
		if( instanceIdList.size() > 18 ) { //vm이 18개 보다 크면 따로 데이터 조회
			String sEndTime = dateFormat.format(cal.getTime());
			cal.add(Calendar.DATE, -7);//일주일
			String sStartTime = dateFormat.format(cal.getTime());
			for( int k = 0; k < aryMetric.length; k++ ) {
				if( k > 0 ) { try { Thread.sleep(200); } catch (Exception e) { ExceptionUtils.getStackTrace(e); } } 
				mapParam = new HashMap<String, String>();
				mapParam.put("EndTime", sEndTime);
				mapParam.put("StartTime", sStartTime);
				n = 1;
				if( k == 0 ) {
					mapParam.put("MetricDataQueries.member."+n+".Id", aryMetric[k].toLowerCase());
					mapParam.put("MetricDataQueries.member."+n+".Label", "");
					mapParam.put("MetricDataQueries.member."+n+".Expression", "AVG(METRICS('m_"+aryMetric[k].toLowerCase()+"'))");
				} else {
					mapParam.put("MetricDataQueries.member."+n+".Id", aryMetric[k].toLowerCase());
					mapParam.put("MetricDataQueries.member."+n+".Label", "");
					mapParam.put("MetricDataQueries.member."+n+".Expression", "SUM(METRICS('m_"+aryMetric[k].toLowerCase()+"'))");
				}
				n++;
				for( int i = 0; i < instanceIdList.size(); i++ ) {
					mapParam.put("MetricDataQueries.member."+n+".Id", "m_"+aryMetric[k].toLowerCase()+(i+1));
					mapParam.put("MetricDataQueries.member."+n+".Label", aryMetric[k]+"_"+instanceIdList.get(i));
					mapParam.put("MetricDataQueries.member."+n+".ReturnData", "false");
					mapParam.put("MetricDataQueries.member."+n+".MetricStat.Period", String.valueOf(3600*24));
					mapParam.put("MetricDataQueries.member."+n+".MetricStat.Stat", aryStat[k]);
					if( k != 0 ) {
						mapParam.put("MetricDataQueries.member."+n+".MetricStat.Unit", "Bytes");//Kilobytes 단위가 적용 안됨
					}
					mapParam.put("MetricDataQueries.member."+n+".MetricStat.Metric.Namespace", "AWS/EC2");
					mapParam.put("MetricDataQueries.member."+n+".MetricStat.Metric.MetricName", aryMetric[k]);
					mapParam.put("MetricDataQueries.member."+n+".MetricStat.Metric.Dimensions.member.1.Name", "InstanceId");
					mapParam.put("MetricDataQueries.member."+n+".MetricStat.Metric.Dimensions.member.1.Value", instanceIdList.get(i));
					n++;
				}
				CloudWatchManager cloudWatchManager = new CloudWatchManager();
				String sResult = cloudWatchManager.getMetricData(credentials, mapParam);
				try {
					checkError(sResult);
				} catch (RuntimeException e) {//Throttling 오류이면 건별조회로 바꾸서 데이터 추출
					if( "Throttling".equals(e.getCode()) ) {
						return getMonitoringData(resGroupId, instanceIdList);
					} else {
						throw e;
					}
				}
				try { 
					JSONObject json = XML.toJSONObject(sResult);
					
					JSONObject jMetricDataResults = json.getJSONObject("GetMetricDataResponse").getJSONObject("GetMetricDataResult").getJSONObject("MetricDataResults");
					JSONArray jaMember = jMetricDataResults.optJSONArray("member");
					if( jaMember == null ) {
						jaMember = new JSONArray().put(jMetricDataResults.getJSONObject("member"));
					}
					ArrayList<Metric> tmpList = null;
					for (int i = 0; i < jaMember.length(); i++) {
						JSONObject jMember = jaMember.getJSONObject(i);
						JSONObject jTimestamps = jMember.optJSONObject("Timestamps");
						JSONObject jValues = jMember.optJSONObject("Values");
						String id = jMember.getString("Id");
						tmpList = new ArrayList<Metric>();
						if( aryMetric[0].equalsIgnoreCase(id) ) {
							cpu = tmpList;
						} else if( aryMetric[1].equalsIgnoreCase(id) ) {
							netIn = tmpList;
						} else if( aryMetric[2].equalsIgnoreCase(id) ) {
							netOut = tmpList;
						} else if( aryMetric[3].equalsIgnoreCase(id) ) {
							diskRead = tmpList;
						} else if( aryMetric[4].equalsIgnoreCase(id) ) {
							diskWrite = tmpList;
						} else {
							continue;
						}
						if( jTimestamps != null ) {
							JSONArray jTimeItem = jTimestamps.optJSONArray("member");
							if( jTimeItem == null ) {
								jTimeItem = new JSONArray();
								jTimeItem.put(jTimestamps.get("member"));
							}
							JSONArray jValItem = jValues.optJSONArray("member");
							if( jValItem == null ) {
								jValItem = new JSONArray();
								jValItem.put(jValues.get("member"));
							}
							for (int j = 0; j < jTimeItem.length(); j++) {
								Metric m = new Metric();
								m.setTimeStamp(localDateFormat.format(dateFormat.parse(jTimeItem.getString(j))).substring(0,10));
								if( aryMetric[0].equalsIgnoreCase(id) ) {//Kilobytes 변환
									m.setAverage(jValItem.getBigDecimal(j).divide(new BigDecimal(1000)).toString());
									m.setTotal("0");
								} else {
									m.setAverage("0");
									m.setTotal(jValItem.getBigDecimal(j).divide(new BigDecimal(1000)).toString());
								}
								tmpList.add(m);
							}
						}
					}
				} catch(Exception e) {
					LOGGER.error(ExceptionUtils.getStackTrace(e));
					throw new RuntimeException(e);
				}
			}
		} else {
			mapParam = new HashMap<String, String>();
			mapParam.put("EndTime", dateFormat.format(cal.getTime()));
			cal.add(Calendar.DATE, -7);//일주일
			mapParam.put("StartTime", dateFormat.format(cal.getTime()));
			
			for( int k = 0; k < aryMetric.length; k++ ) {
				if( k == 0 ) {
					mapParam.put("MetricDataQueries.member."+n+".Id", aryMetric[k].toLowerCase());
					mapParam.put("MetricDataQueries.member."+n+".Label", "");
					mapParam.put("MetricDataQueries.member."+n+".Expression", "AVG(METRICS('m_"+aryMetric[k].toLowerCase()+"'))");
				} else {
					mapParam.put("MetricDataQueries.member."+n+".Id", aryMetric[k].toLowerCase());
					mapParam.put("MetricDataQueries.member."+n+".Label", "");
					mapParam.put("MetricDataQueries.member."+n+".Expression", "SUM(METRICS('m_"+aryMetric[k].toLowerCase()+"'))");
				}
				n++;
				for( int i = 0; i < instanceIdList.size(); i++ ) {
					mapParam.put("MetricDataQueries.member."+n+".Id", "m_"+aryMetric[k].toLowerCase()+(i+1));
					mapParam.put("MetricDataQueries.member."+n+".Label", aryMetric[k]+"_"+instanceIdList.get(i));
					mapParam.put("MetricDataQueries.member."+n+".ReturnData", "false");
					mapParam.put("MetricDataQueries.member."+n+".MetricStat.Period", String.valueOf(3600*24));
					mapParam.put("MetricDataQueries.member."+n+".MetricStat.Stat", aryStat[k]);
					if( k != 0 ) {
						mapParam.put("MetricDataQueries.member."+n+".MetricStat.Unit", "Bytes");////Kilobytes 단위가 적용 안됨
					}
					mapParam.put("MetricDataQueries.member."+n+".MetricStat.Metric.Namespace", "AWS/EC2");
					mapParam.put("MetricDataQueries.member."+n+".MetricStat.Metric.MetricName", aryMetric[k]);
					mapParam.put("MetricDataQueries.member."+n+".MetricStat.Metric.Dimensions.member.1.Name", "InstanceId");
					mapParam.put("MetricDataQueries.member."+n+".MetricStat.Metric.Dimensions.member.1.Value", instanceIdList.get(i));
					n++;
				}
			}
			CloudWatchManager cloudWatchManager = new CloudWatchManager();
			String sResult = cloudWatchManager.getMetricData(credentials, mapParam);
			try {
				checkError(sResult);
			} catch (RuntimeException e) {//Throttling 오류이면 건별조회로 바꾸서 데이터 추출
				if( "Throttling".equals(e.getCode()) ) {
					return getMonitoringData(resGroupId, instanceIdList);
				} else {
					throw e;
				}
			}
			
			try { 
				JSONObject json = XML.toJSONObject(sResult);
				
				JSONObject jMetricDataResults = json.getJSONObject("GetMetricDataResponse").getJSONObject("GetMetricDataResult").getJSONObject("MetricDataResults");
				JSONArray jaMember = jMetricDataResults.optJSONArray("member");
				if( jaMember == null ) {
					jaMember = new JSONArray().put(jMetricDataResults.getJSONObject("member"));
				}
				ArrayList<Metric> tmpList = null;
				for (int i = 0; i < jaMember.length(); i++) {
					JSONObject jMember = jaMember.getJSONObject(i);
					JSONObject jTimestamps = jMember.optJSONObject("Timestamps");
					JSONObject jValues = jMember.optJSONObject("Values");
					String id = jMember.getString("Id");
					tmpList = new ArrayList<Metric>();
					if( aryMetric[0].equalsIgnoreCase(id) ) {
						cpu = tmpList;
					} else if( aryMetric[1].equalsIgnoreCase(id) ) {
						netIn = tmpList;
					} else if( aryMetric[2].equalsIgnoreCase(id) ) {
						netOut = tmpList;
					} else if( aryMetric[3].equalsIgnoreCase(id) ) {
						diskRead = tmpList;
					} else if( aryMetric[4].equalsIgnoreCase(id) ) {
						diskWrite = tmpList;
					} else {
						continue;
					}
					if( jTimestamps != null ) {
						JSONArray jTimeItem = jTimestamps.optJSONArray("member");
						if( jTimeItem == null ) {
							jTimeItem = new JSONArray();
							jTimeItem.put(jTimestamps.get("member"));
						}
						JSONArray jValItem = jValues.optJSONArray("member");
						if( jValItem == null ) {
							jValItem = new JSONArray();
							jValItem.put(jValues.get("member"));
						}
						for (int j = 0; j < jTimeItem.length(); j++) {
							Metric m = new Metric();
							m.setTimeStamp(localDateFormat.format(dateFormat.parse(jTimeItem.getString(j))).substring(0,10));
							if( aryMetric[0].equalsIgnoreCase(id) ) {//Kilobytes 변환
								m.setAverage(jValItem.getBigDecimal(j).divide(new BigDecimal(1000)).toString());
								m.setTotal("0");
							} else {
								m.setAverage("0");
								m.setTotal(jValItem.getBigDecimal(j).divide(new BigDecimal(1000)).toString());
							}
							tmpList.add(m);
						}
					}
				}
			} catch(Exception e) {
				LOGGER.error(ExceptionUtils.getStackTrace(e));
				throw new RuntimeException(e);
			}
		}
		
		//timestamp에 값이 없을수 있음 강제로 값 생성
		ArrayList<String> aryTime = new ArrayList<String>();
		for( int k = 0; k < Math.abs(7); k++ ) {
			aryTime.add(localDateFormat.format(cal.getTime()).substring(0,10));
			cal.add(Calendar.DATE, 1);
		}
		LinkedList<ArrayList<Metric>> aryList = new LinkedList<ArrayList<Metric>>();
		aryList.add(cpu);
		aryList.add(netIn);
		aryList.add(netOut);
		aryList.add(diskRead);
		aryList.add(diskWrite);
		for( int i = 0; i < aryList.size(); i++ ) {
			ArrayList<Metric> list = aryList.get(i);
			if( list != null && list.size() > 0 ) {
				boolean isExist = false;
				for( String sTime : aryTime ) {
					isExist = false;
					for( Metric m : list ) {
						if( m.getTimeStamp().equals(sTime) ) {
							isExist = true;
							break;
						}
					}
					if( !isExist ) {
						Metric metric = new Metric();
						metric.setTimeStamp(sTime);
						metric.setTotal("0");
						metric.setAverage("0");
						list.add(metric);
					}
				}
				Collections.sort(list, new Comparator<Metric>() {
					@Override
					public int compare(Metric s1, Metric s2) {
						return s1.getTimeStamp().compareTo(s2.getTimeStamp());
					}
				});
			} else {
				//if( list == null ) { 
				list = new ArrayList<Metric>();
				if( i == 0 ) {
					cpu = list; 
				} else if( i == 1 ) {
					netIn = list;
				} else if( i == 2 ) {
					netOut = list;
				} else if( i == 3 ) {
					diskRead = list;
				} else if( i == 4 ) {
					diskWrite = list;
				}
				//}
				for( String sTime : aryTime ) {
					Metric m = new Metric();
					m.setTimeStamp(sTime);
					m.setTotal("0");
					m.setAverage("0");
					list.add(m);
				}
			}
		}
		Monitoring monitoring = new Monitoring();
		monitoring.setCpuUsageList(cpu);
		monitoring.setNetworkInList(netIn);
		monitoring.setNetworkOutList(netOut);
		if( cpu != null && cpu.size() > 0 ) {
			monitoring.setCpuUsageAvg(Double.parseDouble(cpu.get(cpu.size()-1).getAverage()));
		}
		if( netIn != null && netIn.size() > 0 ) {
			monitoring.setNetworkInKbyte(Double.parseDouble(netIn.get(netIn.size()-1).getTotal()));
		}
		if( netOut != null && netOut.size() > 0 ) {
			monitoring.setNetworkOutKbyte(Double.parseDouble(netOut.get(netOut.size()-1).getTotal()));
		}
		if( diskRead != null && diskRead.size() > 0 ) {
			monitoring.setDiskReadSum(Double.parseDouble(diskRead.get(diskRead.size()-1).getTotal()));
		}
		if( diskWrite != null && diskWrite.size() > 0 ) {
			monitoring.setDiskWriteSum(Double.parseDouble(diskWrite.get(diskWrite.size()-1).getTotal()));
		}
		return monitoring;
	}
	/**
	 * getMetricData에서 Throttling이 나면 건별로 조회해서 모니터링 데이터를 조회한다.
	 * @param resGroupId 리소스그룹
	 * @param instanceIdList 인스턴스ID 목록
	 * @return Monitoring
	 */
	public Monitoring getMonitoringData(String resGroupId, List<String> instanceIdList) {
		String[] aryMetric = {"CPUUtilization", "NetworkIn", "NetworkOut", "DiskReadBytes","DiskWriteBytes"};
		String[] aryStat   = {"Average","Sum","Sum","Sum","Sum"};
		HashMap<String, List<Metric>> cpuMap = new HashMap<String, List<Metric>>(); 
		HashMap<String, List<Metric>> netInMap = new HashMap<String, List<Metric>>();
		HashMap<String, List<Metric>> netOutMap = new HashMap<String, List<Metric>>();
		HashMap<String, List<Metric>> diskReadMap = new HashMap<String, List<Metric>>();
		HashMap<String, List<Metric>> diskWriteMap = new HashMap<String, List<Metric>>();
		ArrayList<Metric> cpu = null, netIn = null, netOut = null, diskRead = null, diskWrite = null;
		DateFormat localDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		localDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+9"));//local time timezone
		
		TempSecurityCredentials credentials = getCredentials(resGroupId);
		HashMap<String, String> mapParam = new HashMap<String, String>();
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));//server timezone
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE,1);//로컬타임 0시로 맞추기 위해서
		cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE), 0, 0, 0);
		
		String sEdTime = dateFormat.format(cal.getTime());
		cal.add(Calendar.DATE, -7);//일주일
		String sStTime = dateFormat.format(cal.getTime());
		
		ArrayList<String> aryTime = new ArrayList<String>();
		for( int k = 0; k < 7; k++ ) {
			aryTime.add(localDateFormat.format(cal.getTime()).substring(0,10));
			cal.add(Calendar.DATE, 1);
		}
		
		ArrayList<Metric> list = null;
		String instanceId = null;
		CloudWatchManager cloudWatchManager = new CloudWatchManager();
		
		mapParam.put("EndTime", sEdTime);
		mapParam.put("StartTime", sStTime);
		mapParam.put("Period", "86400");//1 hour
		mapParam.put("Namespace", "AWS/EC2");
		mapParam.put("Dimensions.member.1.Name","InstanceId");
		for (int x = 0; x < aryMetric.length; x++) {
			mapParam.put("MetricName", aryMetric[x]); //CPUUtilization NetworkIn NetworkOut DiskReadBytes DiskWriteBytes
			mapParam.put("Statistics.member.1",aryStat[x]);//Minimum, Maximum, Average, SampleCount, Sum
			for (int y = 0; y < instanceIdList.size(); y++) {
				instanceId = instanceIdList.get(y);
				mapParam.put("Dimensions.member.1.Value",instanceId);
				
				list = new ArrayList<Metric>();
				
				String sResult = cloudWatchManager.getMetricStatistics(credentials, mapParam);
				
				JSONObject json = XML.toJSONObject(sResult);
				
				JSONObject jGetMetricStatisticsResult = json.getJSONObject("GetMetricStatisticsResponse").getJSONObject("GetMetricStatisticsResult");
				JSONObject jDatapoints = jGetMetricStatisticsResult.optJSONObject("Datapoints");
				if( jDatapoints != null ) {
					JSONArray jaMember = jDatapoints.optJSONArray("member");
					if( jaMember == null ) {
						jaMember = new JSONArray();
						jaMember.put(jDatapoints.getJSONObject("member"));
					}
					
					BigDecimal bdTmp = null;
					for( int i = 0; i < jaMember.length(); i++ ) {
						JSONObject jo = jaMember.getJSONObject(i);
						Metric metric = new Metric();
						try {
							metric.setTimeStamp(localDateFormat.format(dateFormat.parse(jo.getString("Timestamp"))).substring(0,10));
						} catch (Exception e) { ExceptionUtils.getStackTrace(e); } 
						//metric.setTimeStamp(jo.getString("Timestamp"));
						bdTmp = jo.optBigDecimal("Average", new BigDecimal(0));
						metric.setAverage(bdTmp.toString());
						bdTmp = jo.optBigDecimal("Sum", new BigDecimal(0));
						metric.setTotal(bdTmp.toString());
						list.add(metric);
					}
				}	
					
				if( list != null && list.size() > 0 ) {
					boolean isExist = false;
					for( String sTime : aryTime ) {
						isExist = false;
						for( Metric m : list ) {
							if( m.getTimeStamp().equals(sTime) ) {
								isExist = true;
								break;
							}
						}
						if( !isExist ) {
							Metric metric = new Metric();
							metric.setTimeStamp(sTime);
							metric.setTotal("0");
							metric.setAverage("0");
							list.add(metric);
						}
					}
					Collections.sort(list, new Comparator<Metric>() {
						@Override
						public int compare(Metric s1, Metric s2) {
							return s1.getTimeStamp().compareTo(s2.getTimeStamp());
						}
					});
				} else {
					list = new ArrayList<Metric>();
					for( String sTime : aryTime ) {
						Metric m = new Metric();
						m.setTimeStamp(sTime);
						m.setTotal("0");
						m.setAverage("0");
						list.add(m);
					}
				}
				if( x == 0 ) {
					cpuMap.put(instanceId, list);
				} else if( x == 1 ) {
					netInMap.put(instanceId, list);
				} else if( x == 2 ) {
					netOutMap.put(instanceId, list);
				} else if( x == 3 ) {
					diskReadMap.put(instanceId, list);
				} else if( x == 4 ) {
					diskWriteMap.put(instanceId, list);
				}
			}
		}
		
		Monitoring monitoring = new Monitoring();
		
		double[] val = null;
		double[] cnt = null;
		cpu = new ArrayList<Metric>();
		Iterator<String> it = cpuMap.keySet().iterator();
		boolean isFirst = true;
		while( it.hasNext() ) {
			String key = it.next();
			List<Metric> metricList = cpuMap.get(key);
			if( val == null ) { 
				val = new double[metricList.size()];
				cnt = new double[metricList.size()];
			}
			for( int idx = 0; idx < metricList.size(); idx++ ) {
				Metric m = metricList.get(idx);
				if( isFirst ) {
					Metric metric = new Metric();
					metric.setTimeStamp(m.getTimeStamp());
					metric.setTotal("0");
					metric.setAverage("0");
					cpu.add(metric);
				}
				double avg = Double.parseDouble(m.getAverage());
				if( avg != 0 ) {
					val[idx] += avg;
					cnt[idx]++;
				}
			}
			isFirst = false;
		}
		for( int idx = 0; idx < cpu.size(); idx++ ) {
			Metric m = cpu.get(idx);
			if( cnt[idx] != 0 ) {
				m.setAverage(String.format("%.10f",val[idx]/cnt[idx]));
			} else {
				m.setAverage("0");
			}
		}
		
		BigDecimal[] bVal = null;
		HashMap<String, List<Metric>> tMap = null;
		ArrayList<Metric> tList = null;
		for( int v = 1; v < aryMetric.length; v++ ) {
			if( v == 1 ) {
				tMap = netInMap;
			} else if( v == 2 ) {
				tMap = netOutMap;
			} else if( v == 3 ) {
				tMap = diskReadMap;
			} else if( v == 4 ) {
				tMap = diskWriteMap;
			}
			tList = new ArrayList<Metric>();
			it = tMap.keySet().iterator();
			bVal = null;
			isFirst = true;
			while( it.hasNext() ) {
				String key = it.next();
				List<Metric> metricList = tMap.get(key);
				if( bVal == null ) { 
					bVal = new BigDecimal[metricList.size()];
				}
				for( int idx = 0; idx < metricList.size(); idx++ ) {
					Metric m = metricList.get(idx);
					if( isFirst ) {
						Metric metric = new Metric();
						metric.setTimeStamp(m.getTimeStamp());
						metric.setTotal("0");
						metric.setAverage("0");
						tList.add(metric);
					}
					if( !"0".equals(m.getTotal()) ) {
						if( bVal[idx] == null ) {
							bVal[idx] = new BigDecimal(m.getTotal());
						} else {
							bVal[idx] = bVal[idx].add(new BigDecimal(m.getTotal()));
						}
					}
				}
				isFirst = false;
			}
			for( int idx = 0; idx < tList.size(); idx++ ) {
				Metric m = tList.get(idx);
				if( cnt[idx] != 0 ) {
					m.setTotal(bVal[idx].divide(new BigDecimal(1000)).toBigInteger().toString());
				} else {
					m.setTotal("0");
				}
			}
			if( v == 1 ) {
				netIn = tList;
			} else if( v == 2 ) {
				netOut = tList;
			} else if( v == 3 ) {
				diskRead = tList;
			} else if( v == 4 ) {
				diskWrite = tList;
			}
		}
		
		monitoring.setCpuUsageList(cpu);
		monitoring.setNetworkInList(netIn);
		monitoring.setNetworkOutList(netOut);
		if( cpu != null && cpu.size() > 0 ) {
			monitoring.setCpuUsageAvg(Double.parseDouble(cpu.get(cpu.size()-1).getAverage()));
		}
		if( netIn != null && netIn.size() > 0 ) {
			monitoring.setNetworkInKbyte(Double.parseDouble(netIn.get(netIn.size()-1).getTotal()));
		}
		if( netOut != null && netOut.size() > 0 ) {
			monitoring.setNetworkOutKbyte(Double.parseDouble(netOut.get(netOut.size()-1).getTotal()));
		}
		if( diskRead != null && diskRead.size() > 0 ) {
			monitoring.setDiskReadSum(Double.parseDouble(diskRead.get(diskRead.size()-1).getTotal()));
		}
		if( diskWrite != null && diskWrite.size() > 0 ) {
			monitoring.setDiskWriteSum(Double.parseDouble(diskWrite.get(diskWrite.size()-1).getTotal()));
		}
		return monitoring;
	}
	/*
{"GetMetricDataResponse":{"ResponseMetadata":{"RequestId":"581241ad-396d-11e8-9a9e-5b6253a16bcb"},"GetMetricDataResult":{"MetricDataResults":{"member":[{"StatusCode":"Complete","Values":{"member":[818501,829560,865446,1724904,7835476,1.4787792E7,1390962]},"Id":"networkin","Label":"netin","Timestamps":{"member":["2018-04-05T07:00:00Z","2018-04-04T07:00:00Z","2018-04-03T07:00:00Z","2018-04-02T07:00:00Z","2018-04-01T07:00:00Z","2018-03-31T07:00:00Z","2018-03-30T07:00:00Z"]}},{"StatusCode":"Complete","Values":{"member":[0.009079572803795727,0.008794054838897782,0.008939306949394899,0.017664136001777467,0.032499323770935375,0.06490546473786123,0.010365982826438911]},"Id":"cpuutilization","Label":"cpu","Timestamps":{"member":["2018-04-05T07:00:00Z","2018-04-04T07:00:00Z","2018-04-03T07:00:00Z","2018-04-02T07:00:00Z","2018-04-01T07:00:00Z","2018-03-31T07:00:00Z","2018-03-30T07:00:00Z"]}},{"StatusCode":"Complete","Values":"","Id":"diskwritebytes","Label":"diskwrite","Timestamps":""},{"StatusCode":"Complete","Values":"","Id":"diskreadbytes","Label":"diskread","Timestamps":""},{"StatusCode":"Complete","Values":{"member":[993344,981805,1075655,2216513,1.2107354E7,2.6771517E7,1847119]},"Id":"networkout","Label":"netout","Timestamps":{"member":["2018-04-05T07:00:00Z","2018-04-04T07:00:00Z","2018-04-03T07:00:00Z","2018-04-02T07:00:00Z","2018-04-01T07:00:00Z","2018-03-31T07:00:00Z","2018-03-30T07:00:00Z"]}}]}},"xmlns":"http://monitoring.amazonaws.com/doc/2010-08-01/"}}
	 */	
	/**
	 * one VM - one metricNm 24시간전부터 1시간단위 통계 조회 
	 * @param resGroupId 리소스그룹Id
	 * @param instanceId 인스턴스Id
	 * @param metricNm 지표 CPUUtilization NetworkIn NetworkOut DiskReadBytes DiskWriteBytes
	 * @param statistics 값유형 Minimum, Maximum, Average, SampleCount, Sum
	 * @return List<Metric> 통계값
	 */
	public List<Metric> getMetricStatistics(String resGroupId, String instanceId, String metricNm, String statistics) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));//server timezone
		
		//DateFormat localDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		//localDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+9"));//local time timezone
		
		ArrayList<Metric> list = null;
		
		TempSecurityCredentials credentials = getCredentials(resGroupId);
		
		HashMap<String, String> mapParam = new HashMap<String, String>();
		Calendar cal = Calendar.getInstance();
		cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE), cal.get(Calendar.HOUR_OF_DAY), 0, 0);
		mapParam.put("EndTime", dateFormat.format(cal.getTime()));
		cal.add(Calendar.HOUR, -23);
		mapParam.put("StartTime", dateFormat.format(cal.getTime()));
		mapParam.put("Period", "3600");//1 hour
		mapParam.put("MetricName", metricNm); //CPUUtilization NetworkIn NetworkOut DiskReadBytes DiskWriteBytes
		mapParam.put("Namespace", "AWS/EC2");
		mapParam.put("Statistics.member.1",statistics);//Minimum, Maximum, Average, SampleCount, Sum
		mapParam.put("Dimensions.member.1.Name","InstanceId");
		mapParam.put("Dimensions.member.1.Value",instanceId);
		
		CloudWatchManager cloudWatchManager = new CloudWatchManager();
		String sResult = cloudWatchManager.getMetricStatistics(credentials, mapParam);
		
		JSONObject json = XML.toJSONObject(sResult);
		
		JSONObject jGetMetricStatisticsResult = json.getJSONObject("GetMetricStatisticsResponse").getJSONObject("GetMetricStatisticsResult");
		JSONObject jDatapoints = jGetMetricStatisticsResult.optJSONObject("Datapoints");
		if( jDatapoints != null ) {
			list = new ArrayList<Metric>();
			JSONArray jaMember = jDatapoints.optJSONArray("member");
			if( jaMember == null ) {
				jaMember = new JSONArray();
				jaMember.put(jDatapoints.getJSONObject("member"));
			}
			
			BigDecimal bdTmp = null;
			for( int i = 0; i < jaMember.length(); i++ ) {
				JSONObject j = jaMember.getJSONObject(i);
				Metric metric = new Metric();
				//metric.setTimeStamp(localDateFormat.format(dateFormat.parse(j.getString("Timestamp"))));
				metric.setTimeStamp(j.getString("Timestamp"));
				bdTmp = j.optBigDecimal("Average", new BigDecimal(0));
				metric.setAverage(bdTmp.toString());
				bdTmp = j.optBigDecimal("Sum", new BigDecimal(0));
				metric.setTotal(bdTmp.toString());
				list.add(metric);
			}
			//timestamp에 값이 없을수 있음 강제로 값 생성
			String sTime = null;
			for( int k = 0; k < 24; k++ ) {
				//sTime = localDateFormat.format(cal.getTime());
				sTime = dateFormat.format(cal.getTime());
				boolean isExist = false;
				for( int i = 0; i < list.size(); i++ ) {
					if( list.get(i).getTimeStamp().equals(sTime) ) {
						isExist = true;
						break;
					}
				}
				if( !isExist ) {
					Metric metric = new Metric();
					metric.setTimeStamp(sTime);
					metric.setTotal("0");
					metric.setAverage("0");
					list.add(metric);
				}
				cal.add(Calendar.HOUR, 1);
			}
			Collections.sort(list, new Comparator<Metric>() {
				@Override
				public int compare(Metric s1, Metric s2) {
					return s1.getTimeStamp().compareTo(s2.getTimeStamp());
				}
			});
		} else {
			String sTime = null;
			list = new ArrayList<Metric>();
			for( int k = 0; k < 24; k++ ) {
				//sTime = localDateFormat.format(cal.getTime());
				sTime = dateFormat.format(cal.getTime());
				Metric metric = new Metric();
				metric.setTimeStamp(sTime);
				metric.setTotal("0");
				metric.setAverage("0");
				list.add(metric);
				cal.add(Calendar.HOUR, 1);
			}
		}
		for( Metric m : list ) {
			LOGGER.debug(metricNm+" time::"+m.getTimeStamp()+",total::"+m.getTotal()+",average::"+m.getAverage());
		}
		return list;
	}
	/**
	 * instance 상태 조회
	 * @param resGroupId 리소스그룹ID
	 * @param instanceIdList 인스턴스ID목록
	 * @return List<AWSResource> 리소스 상태
	 */
	@Override
	public List<UseResource> describeInstanceStatus(String resGroupId, List<UseResource> instanceIdList) {
		ArrayList<UseResource> list = new ArrayList<UseResource>();
		
		TempSecurityCredentials credentials = getCredentials(resGroupId);
		HashMap<String, String> mapParam = new HashMap<String, String>();
		mapParam.put("IncludeAllInstances", "true");//모든상태 포함
		for (int i = 0; i < instanceIdList.size(); i++) {
			mapParam.put(String.format("InstanceId.%s",String.valueOf(i+1)), instanceIdList.get(i).getResNm());
		}
		EC2Manager ec2Manager = new EC2Manager();
		String sResult = ec2Manager.describeInstanceStatus(credentials, mapParam);
		try {
			checkError(sResult);
		} catch(RuntimeException e){
			//not found instance는 제거하고 다시 조회한다.
			if( "InvalidInstanceID.NotFound".equals(e.getCode()) ){
				Iterator<String> iterator = mapParam.keySet().iterator();
				ArrayList<String> remKeyList = new ArrayList<String>();
				while( iterator.hasNext() ) {
					String sKey = iterator.next();
					String sVal = mapParam.get(sKey);
					if( e.getMessage().indexOf(sVal) >= 0 ) {
						UseResource resource = new UseResource();
						resource.setResNm(sVal);
						resource.setResStatus("not found");
						list.add(resource);
						remKeyList.add(sKey);
					}
				}
				for( String sRemKey : remKeyList ) {
					mapParam.remove(sRemKey);
				}
				if( mapParam.size() > 1 ) {
					sResult = ec2Manager.describeInstanceStatus(credentials, mapParam);
					checkError(sResult);
				} else {
					throw e;
				}
			} else {
				throw e;
			}
		}
		
		JSONObject json = XML.toJSONObject(sResult);
		JSONObject j = json.getJSONObject("DescribeInstanceStatusResponse");
		JSONObject jInstanceStatusSet = j.getJSONObject("instanceStatusSet");
		JSONArray jaItems = jInstanceStatusSet.optJSONArray("item");
		if (jaItems == null) {
			jaItems = new JSONArray();
			jaItems.put(jInstanceStatusSet.getJSONObject("item"));
		}
		String sStatus = null;
		for (int i = 0; i < jaItems.length(); i++) {
			UseResource resource = new UseResource(); 
			resource.setResNm(jaItems.getJSONObject(i).getString("instanceId"));
			JSONObject jInstanceState = jaItems.getJSONObject(i).getJSONObject("instanceState");
			sStatus = jInstanceState.getString("name");
			if( sStatus != null && !"".equals(sStatus) ) {
				if( "running".equals(sStatus) ) {
					resource.setResStatus("VM running");
				} else if( "stopped".equals(sStatus) ) {
					resource.setResStatus("VM deallocated");
				} else {
					resource.setResStatus(sStatus);
				}
			} else {
				resource.setResStatus("");
			}	
			list.add(resource);
		}
		return list;
	}
	/**
	 * instance 상태 세부 조회
	 * @param resGroupId 리소스그룹ID
	 * @param instanceIdList 인스턴스ID목록
	 * @return List<AwsResource> 인스턴스 상태 목록
	 */
	public List<AWSResource> describeInstanceStatusDetail(String resGroupId, List<String> instanceIdList) {
		ArrayList<AWSResource> list = new ArrayList<AWSResource>();
		
		TempSecurityCredentials credentials = getCredentials(resGroupId);
		HashMap<String, String> mapParam = new HashMap<String, String>();
		mapParam.put("IncludeAllInstances", "true");//모든상태 포함
		for (int i = 0; i < instanceIdList.size(); i++) {
			mapParam.put(String.format("InstanceId.%s",String.valueOf(i+1)), instanceIdList.get(i));
		}
		EC2Manager ec2Manager = new EC2Manager();
		String sResult = ec2Manager.describeInstanceStatus(credentials, mapParam);
		try {
			checkError(sResult);
		} catch(RuntimeException e){
			//not found instance는 제거하고 다시 조회한다.
			if( "InvalidInstanceID.NotFound".equals(e.getCode()) ){
				Iterator<String> iterator = mapParam.keySet().iterator();
				ArrayList<String> remKeyList = new ArrayList<String>();
				while( iterator.hasNext() ) {
					String sKey = iterator.next();
					String sVal = mapParam.get(sKey);
					if( e.getMessage().indexOf(sVal) >= 0 ) {
						AWSResource resource = new AWSResource();
						resource.setResourceId(sVal);
						resource.setInstanceState("not found");
						list.add(resource);
						remKeyList.add(sKey);
					}
				}
				for( String sRemKey : remKeyList ) {
					mapParam.remove(sRemKey);
				}
				if( mapParam.size() > 1 ) {
					sResult = ec2Manager.describeInstanceStatus(credentials, mapParam);
					checkError(sResult);
				} else {
					throw e;
				}
			} else {
				throw e;
			}
		}
		
		JSONObject json = XML.toJSONObject(sResult);
		JSONObject j = json.getJSONObject("DescribeInstanceStatusResponse");
		JSONObject jInstanceStatusSet = j.getJSONObject("instanceStatusSet");
		JSONArray jaItems = jInstanceStatusSet.optJSONArray("item");
		if (jaItems == null) {
			jaItems = new JSONArray();
			jaItems.put(jInstanceStatusSet.getJSONObject("item"));
		}
		String sStatus = null;
		for (int i = 0; i < jaItems.length(); i++) {
			JSONObject jItem = jaItems.getJSONObject(i);
			AWSResource resource = new AWSResource(); 
			resource.setResourceId(jItem.getString("instanceId"));
			JSONObject jInstanceState = jItem.getJSONObject("instanceState");
			sStatus = jInstanceState.getString("name");
			if( sStatus != null && !"".equals(sStatus) ) {
				if( "running".equals(sStatus) ) {
					resource.setInstanceState("VM running");
				} else if( "stopped".equals(sStatus) ) {
					resource.setInstanceState("VM deallocated");
				} else {
					resource.setInstanceState(sStatus);
				}
			} else {
				resource.setInstanceState("");
			}
			resource.setCheckSystemStatus(jItem.getJSONObject("systemStatus").getString("status"));
			resource.setCheckInstanceStatus(jItem.getJSONObject("instanceStatus").getString("status"));
			list.add(resource);
		}
		return list;
	}
	/**
	 * S3 bucket 목록 조회
	 * @param resGroupId 리소스그룹ID
	 * @return
	 */
	@Override
	public List<AWSResource> listAllMyBuckets(String resGroupId) {
		ArrayList<AWSResource> list = new ArrayList<AWSResource>();
		
		TempSecurityCredentials credentials = getCredentials(resGroupId);

		S3Manager s3Manager = new S3Manager();
		String sResult = s3Manager.listAllMyBuckets(credentials);
		checkError(sResult);

		JSONObject json = XML.toJSONObject(sResult);
		
		JSONObject jBuckets = json.getJSONObject("ListAllMyBucketsResult").optJSONObject("Buckets");
		if( jBuckets != null ) {
			JSONArray jaBucket = jBuckets.optJSONArray("Bucket");
			if( jaBucket == null ) {
				jaBucket = new JSONArray();
				jaBucket.put(jBuckets.optJSONObject("Bucket"));
			}
			AWSResource r = null;
			for (int i = 0; i < jaBucket.length(); i++) {
				JSONObject jBucket = jaBucket.getJSONObject(i);
				r = new AWSResource();
				r.setResourceType("S3::bucket");
				r.setResourceId(jBucket.getString("Name"));
				try {
					String taggingResult = s3Manager.tagging(credentials, jBucket.getString("Name"));
					checkError(taggingResult);
					JSONObject jTagSet = XML.toJSONObject(taggingResult).getJSONObject("Tagging").optJSONObject("TagSet");
					if( jTagSet != null ) {
						JSONArray jaTag = jTagSet.optJSONArray("Tag");
						if( jaTag == null ) {
							jaTag = new JSONArray();
							jaTag.put(jTagSet.optJSONObject("Tag"));
						}
						for (int j = 0; j < jaTag.length(); j++) {
							JSONObject jTag = jaTag.getJSONObject(j);
							r.addTag(jTag.getString("Key"), jTag.get("Value").toString());
						}
					}
				//확인해서 오류더라도 무시한다.
				} catch (RuntimeException e) { //Tagging 이 없으면 <Code>NoSuchTagSet</Code><Message>The TagSet does not exist</Message>
					LOGGER.error(ExceptionUtils.getStackTrace(e));
				}
				list.add(r);
			}
		}
		for (int i = 0; i < list.size(); i++) {
			LOGGER.debug(i+"::"+list.get(i).toString());
		}
		return list;
	}
	/**
	 * 사용량 레포트 가져와서 DB에 적재
	 * @param yyyymm 년월 ex)2018-04
	 * @return int 데이터 생성 결과
	 */
	@Override
	public int getUsageReport(String yyyymm) {
		//S3 connect Payer Account : 임시자격증명을 사용하지 않음
		//파일을 생성안함
		//S3에서 report를 ByteArrayOutStream을 사용하여 
		//메모리상에서 압축풀고 csv파일 내용을 읽어 DB에 저장함
		String sStDate = "";
		String lastDay = "";
		Calendar cal = Calendar.getInstance();
		int iDay = cal.get(Calendar.DATE);

		if (yyyymm.length() > 8) {
			// 사용자 배치 재기동 화면에서 처리할 경우 
			lastDay = yyyymm.substring(6, 8);
			if ("02".equals(lastDay) || "17".equals(lastDay)) {
				sStDate = yyyymm.substring(0, 7) +"-01 00:00:00";
			} else {
				cal.add(Calendar.DATE, -3);
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				sStDate = df.format(cal.getTime())+" 00:00:00";
				
				if( !sStDate.substring(0, 7).equals(yyyymm.substring(0, 7)) ) {
					sStDate = yyyymm.substring(0, 7) +"-01 00:00:00";
				} else {
					sStDate = yyyymm +" 00:00:00";
				}
			}
		} else {
			// 기존 배치 처리 로직
			if( iDay == 2 || iDay == 17) { //17일과 2일은 해당월 데이터 전체를 삭제하고 해당월 데이터 전체를 밀어넣음
				sStDate = yyyymm+"-01 00:00:00";
			} else {//다른 날짜는 로컬타임으로 -3일 날짜를 기준으로 삭제하고 밀어넣음(원래는 -2일인데 하루는 겹쳐지게 여유를 둬서 -3일로 처리함   
				cal.add(Calendar.DATE, -3);
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				sStDate = df.format(cal.getTime())+" 00:00:00";
				if( !sStDate.substring(0,7).equals(yyyymm) ) {
					sStDate = yyyymm+"-01 00:00:00";
				}
			}			
		}
		TempSecurityCredentials credentials = new TempSecurityCredentials();
		credentials.setAccessKeyId(accessKeyId);
		credentials.setSecretAccessKey(secretAccessKey);
		credentials.setRegion("ap-northeast-2");
		
		int cnt = 0;
		String sResult = "";
		
		S3Manager s3Manager = new S3Manager();
		if (yyyymm.length() > 8) {
			sResult = s3Manager.getUsageReport(credentials, yyyymm.substring(0, 7));
		} else {
			sResult = s3Manager.getUsageReport(credentials, yyyymm);	
		}
		
		try {
			BufferedReader reader = new BufferedReader(new StringReader(sResult));
			String sLine = null;
			int iLineNum = 0, iFieldNum = 0;
			LinkedList<String> csvFieldList = new LinkedList<String>();
			LinkedList<String> voMethodList = new LinkedList<String>();
			ArrayList<AWSUsage> voList = new ArrayList<AWSUsage>();
			AWSUsage vo = new AWSUsage();
			String[] aryRecord = null;
			while( (sLine = reader.readLine()) != null ) {
				//LOGGER.debug("\nLINE::"+iLineNum+","+sLine);
				vo = new AWSUsage();
				aryRecord = StringUtil.split(sLine, "\",\"", true);
				if( iLineNum > 0 && !"LineItem".equals(aryRecord[3]) ) {//LineItem이 아니면 skip
					continue;
				}
				for( iFieldNum = 0; iFieldNum < aryRecord.length; iFieldNum++ ) {
					String sVal = aryRecord[iFieldNum].replaceAll("\"", "");
					if( iLineNum == 0 ) {
						//LOGGER.debug("\nLINE::"+iLineNum+",FIELD::"+iFieldNum+",value::"+sVal);
						Method[] aryMethod = vo.getClass().getDeclaredMethods();
						csvFieldList.add(sVal);
						if( sVal.toLowerCase().indexOf("createdBy".toLowerCase()) >= 0 ) {
							voMethodList.add("set"+StringUtil.toUpperCaseFirstLetter("createdBy"));
						} else if( sVal.toLowerCase().indexOf("user:name".toLowerCase()) >= 0 ) {
							voMethodList.add("set"+StringUtil.toUpperCaseFirstLetter("userTag01"));
						} else if( sVal.toLowerCase().indexOf("user:rg".toLowerCase()) >= 0 ) {
							voMethodList.add("set"+StringUtil.toUpperCaseFirstLetter("userTag02"));
						} else if( sVal.toLowerCase().indexOf("user:solution".toLowerCase()) >= 0 ) {
							voMethodList.add("set"+StringUtil.toUpperCaseFirstLetter("userTag03"));
						} else {
							String sMethodName = "";
							for (int i = 0; i < aryMethod.length; i++) {
								if( aryMethod[i].getName().toLowerCase().indexOf("set"+sVal.toLowerCase()) >= 0 ) {
									sMethodName = aryMethod[i].getName();
									break;
								}
							}
							voMethodList.add(sMethodName);
						}
					} else {
						if( csvFieldList.size() != aryRecord.length ) {
							throw new RuntimeException("csv line separate error");
						}
						//LOGGER.debug("\nLINE::"+iLineNum+",FIELD::"+iFieldNum+",value::"+sVal+",fieldName::"+csvFieldList.get(iFieldNum)+",methodName::"+voMethodList.get(iFieldNum));
						if( !"".equals(voMethodList.get(iFieldNum)) ) {
							if( csvFieldList.get(iFieldNum).equalsIgnoreCase("usageQuantity") || 
									csvFieldList.get(iFieldNum).equalsIgnoreCase("blendedRate") ||
									csvFieldList.get(iFieldNum).equalsIgnoreCase("blendedCost") ||
									csvFieldList.get(iFieldNum).equalsIgnoreCase("unBlendedRate") ||
									csvFieldList.get(iFieldNum).equalsIgnoreCase("unBlendedCost") ) {
								vo.getClass().getMethod(voMethodList.get(iFieldNum), double.class).invoke(vo, Double.parseDouble(sVal));
							} else {
								vo.getClass().getMethod(voMethodList.get(iFieldNum), sLine.getClass()).invoke(vo, sVal);
							}
						}
					}
				}
				if( iLineNum != 0 ) {
					if( vo.getUsageStartDate().compareTo(sStDate) >= 0 ) {//사용량 날짜가 작업시작일시보다 크거나 같으면 처리 대상으로 넣음 
						vo.setSeq(iLineNum);
						vo.setFirstRegUser("batch");
						vo.setLastUpdUser("batch");
						voList.add(vo);
					}
				} else {
					StringBuilder sb1 = new StringBuilder();
					StringBuilder sb2 = new StringBuilder();
					for (int i = 0; i < csvFieldList.size(); i++) {
						sb1.append(","+csvFieldList.get(i));
						sb2.append(","+voMethodList.get(i));
					}
					//LOGGER.debug("\n"+sb1.toString()+"\n"+sb2.toString());
				}
				iLineNum++;
			}
			if( voList.size() > 0 ) {
				AWSUsage deleteParamVO = new AWSUsage();
				deleteParamVO.setUsageStartDate(sStDate);
				commonDao.delete("sportal.bds.service.aws.deleteUsageAWS", deleteParamVO);
				commonDao.batchInsert("sportal.bds.service.aws.insertUsageAWS", voList, true);
				cnt = voList.size();
			}
			return cnt;
		} catch(Exception e) {
			LOGGER.error(ExceptionUtils.getStackTrace(e));
			throw new RuntimeException(e);
		}
	}
	/**
	 * AWS 사용량 집계
	 * @param yyyymm 년월 ex)2018-04
	 */
	@Override
	public void aggregatesDailyUsage(String yyyymm){
		Using parameter = new Using();
		parameter.setUseDate(yyyymm);
		//tbl_usage_aggregates_day_aws table에 해당월 데이터 삭제후 일별 집계처리
		commonDao.delete("sportal.bds.service.aws.deleteUsageAggregatesAWS", parameter);
		commonDao.insert("sportal.bds.service.aws.insertUsageAggregatesAWS", parameter);
		//tbl_usage_aggregates_day_azure table에 해당월 데이터 삭제후 복사
		commonDao.delete("sportal.bds.service.aws.deleteUsageAggregates", parameter);
		commonDao.insert("sportal.bds.service.aws.insertUsageAggregates", parameter);
	}
	/**
	 * AWS 플랫폼 삭제
	 * @param serviceDeleteReqId 삭제 시퀀스 아이디
	 * @param deleteTp 삭제타입
	 * @param serviceRequest 서비스 요청정보
	 */
	@Override
	public void removePlatformResources(String serviceDeleteReqId, String deleteTp, ServiceRequest serviceRequest) {
		String resGroupId = serviceRequest.getResGroupId();
		ArrayList<UseResource> delInstanceList = new ArrayList<UseResource>();
		List<UseResource> cloudResList = null;
		try {
			cloudResList = Provider.getProvider("AWS").allResourcesList(resGroupId,"AWS");
		} catch (Exception e) {
			LOGGER.error(ExceptionUtils.getStackTrace(e));
			throw new RuntimeException(e);
		}
		if( cloudResList == null || cloudResList.size() == 0 ) {
			throw new RuntimeException("Resource is Empty");
		}
		//virtualMachines을 처음으로 순서변경
		for (int n = 0; n < cloudResList.size(); n++) {
			UseResource resource = cloudResList.get(n);
			if( VIRTUALMACHINES.equalsIgnoreCase(resource.getResType()) ) {
				cloudResList.remove(n);
				cloudResList.add(0, resource);
			}
		}
		
		List<UseResource> resList = commonDao.selectList("sportal.bds.service.aws.retrieveSolutionResNmList", serviceDeleteReqId);
		//삭제대상솔루션리스트를 반복하여...
		for( UseResource useRes : resList ) {
			if( VIRTUALMACHINES.equalsIgnoreCase(useRes.getResType()) ) {//virtualMachines 일때만
				for( UseResource cloudRes : cloudResList ) {
					if( useRes.getResNm().equals(cloudRes.getResNm()) && VIRTUALMACHINES.equalsIgnoreCase(cloudRes.getResType())) {
						delInstanceList.add(useRes);
					}
				}
			}
		}
		//혹시 밑에서 해당 리스트로 삭제 처리되는걸 방지 하기 위해 null 처리함
		cloudResList = null;
		resList = null;
		for( UseResource delRes : delInstanceList ) {
			LOGGER.debug("terminate instance ::"+delRes.getResNm());
		}
		if( delInstanceList.size() > 0 ) {
			TempSecurityCredentials credentials = getCredentials(resGroupId);
			HashMap<String, String> mapParam = new HashMap<String, String>();
			for (int i = 1; i <= delInstanceList.size(); i++) {
				mapParam.put(String.format("InstanceId.%s",Integer.toString(i)), delInstanceList.get(i-1).getResNm());
			}
			
			EC2Manager ec2Manager = new EC2Manager();
			String sResult = ec2Manager.terminateInstances(credentials, mapParam);
			checkError(sResult);
			
			JSONObject json = XML.toJSONObject(sResult);
			JSONObject jStopInstancesResponse = json.getJSONObject("TerminateInstancesResponse");
			JSONObject jInstancesSet = jStopInstancesResponse.getJSONObject("instancesSet");
			JSONArray jaItem = jInstancesSet.optJSONArray("item");
			if( jaItem == null ) {
				jaItem = new JSONArray();
				jaItem.put(jInstancesSet.getJSONObject("item"));
			}
			for (int i = 0; i < jaItem.length(); i++) {
				JSONObject jItem = jaItem.getJSONObject(i);
				String sInstanceId = jItem.getString("instanceId");
				JSONObject jCurrentState = jItem.getJSONObject("currentState");
				//int iCurrStataCode = jCurrentState.getInt("code");
				String sCurrStateName = jCurrentState.getString("name");
				JSONObject jPreviousState = jItem.getJSONObject("previousState");
				//int iPrevStataCode = jPreviousState.getInt("code");
				String sPrevStateName = jPreviousState.getString("name");
				LOGGER.debug(new StringBuilder().append("TerminateInstances ").append(sInstanceId).append(" current state=").append(sCurrStateName).append(" previous state=").append(sPrevStateName).toString());
			}
		}
	}
}
